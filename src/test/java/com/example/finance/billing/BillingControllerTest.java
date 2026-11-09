package com.example.finance.billing;

import com.example.finance.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class BillingControllerTest {
    final UserRepository users = mock(UserRepository.class);
    final PaymentRepository payments = mock(PaymentRepository.class);
    final StripeGateway stripe = mock(StripeGateway.class);
    final ObjectMapper mapper = new ObjectMapper();
    final BillingController controller = new BillingController(users, payments, stripe, mapper, "price_test", "http://localhost:3000", "whsec_test", "会员");
    final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("alice", null, List.of());
    @Test void rejectsAnonymousCheckout() {
        assertThrows(ResponseStatusException.class, () -> controller.checkout(null));
        verifyNoInteractions(stripe, payments);
    }
    @Test void refusesIncorrectPriceBeforeCreatingPayment() throws Exception {
        User user = new User("alice", "password", "alice@example.com"); user.setId(1L);
        when(users.findByUsername("alice")).thenReturn(Optional.of(user));
        when(payments.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(stripe.configured()).thenReturn(true);
        when(stripe.price("price_test")).thenReturn(mapper.readTree("{\"active\":true,\"unit_amount\":100,\"currency\":\"usd\",\"type\":\"one_time\"}"));
        assertThrows(ResponseStatusException.class, () -> controller.checkout(auth));
        verify(payments, never()).save(any());
        verify(stripe, never()).create(any(), any());
    }
    @Test void createsCheckoutForNinetyNineCentsUsd() throws Exception {
        User user = new User("alice", "password", "alice@example.com"); user.setId(1L);
        when(users.findByUsername("alice")).thenReturn(Optional.of(user));
        when(payments.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(stripe.configured()).thenReturn(true);
        when(stripe.price("price_test")).thenReturn(mapper.readTree("{\"active\":true,\"unit_amount\":99,\"currency\":\"usd\",\"type\":\"recurring\",\"recurring\":{\"interval\":\"year\",\"interval_count\":1}}"));
        when(stripe.create(any(), any())).thenReturn(mapper.readTree("{\"id\":\"cs_test_mock\",\"url\":\"https://checkout.stripe.com/mock\"}"));
        assertEquals("https://checkout.stripe.com/mock", controller.checkout(auth).get("url"));
        verify(stripe).create(argThat(fields -> "price_test".equals(fields.get("line_items[0][price]")) && "subscription".equals(fields.get("mode"))), any());
    }
    @Test void rejectsForgedWebhookBeforeReadingOrders() {
        assertThrows(ResponseStatusException.class, () -> controller.webhook("{}", "t=1,v1=bad"));
        verifyNoInteractions(payments, stripe);
    }
    private void signedEvent(String type, String object) throws Exception {
        String payload = "{\"type\":\"" + type + "\",\"data\":{\"object\":" + object + "}}";
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("whsec_test".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8)));
        controller.webhook(payload, "t=" + timestamp + ",v1=" + signature);
    }
    private Payment existingSubscription() {
        Payment p = new Payment(); p.id = "order"; p.userId = 1L; p.subscriptionId = "sub_test";
        p.customerId = "cus_test"; p.status = "paid"; p.subscriptionStatus = "active";
        p.memberUntil = Instant.now().plusSeconds(3600);
        when(payments.findById("order")).thenReturn(Optional.of(p));
        User user = new User("alice", "password", "alice@example.com"); user.setId(1L);
        when(users.findByUsername("alice")).thenReturn(Optional.of(user));
        when(payments.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(p));
        return p;
    }
    private String subscriptionJson(String status, boolean cancel, String invoice) {
        return "{\"id\":\"sub_test\",\"customer\":\"cus_test\",\"metadata\":{\"order_id\":\"order\"},\"status\":\"" + status +
            "\",\"cancel_at_period_end\":" + cancel + ",\"latest_invoice\":" + invoice + "}";
    }
    @Test void paidRenewalExtendsAccessAndDeduplicatesInvoice() throws Exception {
        Payment p = existingSubscription(); long end = Instant.now().plusSeconds(31536000).getEpochSecond();
        String invoice = "{\"id\":\"in_renewal\",\"status\":\"paid\",\"amount_paid\":99,\"currency\":\"usd\",\"billing_reason\":\"subscription_cycle\",\"created\":1," +
            "\"lines\":{\"data\":[{\"amount\":99,\"pricing\":{\"price_details\":{\"price\":\"price_test\"}},\"period\":{\"end\":" + end + "}}]}}";
        when(stripe.subscription("sub_test")).thenReturn(mapper.readTree(subscriptionJson("active", false, invoice)));
        when(payments.existsById("in_renewal")).thenReturn(false, true);
        signedEvent("invoice.paid", "{\"parent\":{\"subscription_details\":{\"subscription\":\"sub_test\"}}}");
        signedEvent("invoice.paid", "{\"subscription\":\"sub_test\"}");
        assertEquals(Instant.ofEpochSecond(end), p.memberUntil);
        assertEquals(true, controller.membership(auth).get("active"));
        verify(payments, times(1)).save(argThat(record -> "in_renewal".equals(record.id)));
    }
    @Test void failedRenewalDoesNotExtendAccess() throws Exception {
        Payment p = existingSubscription(); p.memberUntil = Instant.now().minusSeconds(1); Instant original = p.memberUntil;
        when(stripe.subscription("sub_test")).thenReturn(mapper.readTree(subscriptionJson("past_due", false, "{\"status\":\"open\",\"amount_paid\":0}")));
        signedEvent("invoice.payment_failed", "{\"subscription\":\"sub_test\"}");
        assertEquals(original, p.memberUntil);
        assertEquals(false, controller.membership(auth).get("active"));
        assertEquals(true, controller.membership(auth).get("canManage"));
    }
    @Test void scheduledCancellationKeepsPaidAccessButDeletedSubscriptionRevokesIt() throws Exception {
        existingSubscription();
        when(stripe.subscription("sub_test")).thenReturn(mapper.readTree(subscriptionJson("active", true, "null")),
            mapper.readTree(subscriptionJson("canceled", false, "null")));
        signedEvent("customer.subscription.updated", "{\"id\":\"sub_test\"}");
        assertEquals(true, controller.membership(auth).get("active"));
        assertEquals(true, controller.membership(auth).get("cancelAtPeriodEnd"));
        signedEvent("customer.subscription.deleted", "{\"id\":\"sub_test\"}");
        assertEquals(false, controller.membership(auth).get("active"));
    }
    @Test void portalUsesAuthenticatedUsersCustomer() throws Exception {
        existingSubscription();
        when(stripe.portal("cus_test", "http://localhost:3000/dashboard/billing"))
            .thenReturn(mapper.readTree("{\"url\":\"https://billing.stripe.com/test\"}"));
        assertEquals("https://billing.stripe.com/test", controller.portal(auth).get("url"));
    }

    @Test void rejectsMonthlyAndOneTimePrices() throws Exception {
        existingSubscription();
        when(payments.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(stripe.configured()).thenReturn(true);
        for (String type : List.of("one_time", "recurring")) {
            when(stripe.price("price_test")).thenReturn(mapper.readTree("{\"active\":true,\"unit_amount\":99,\"currency\":\"usd\",\"type\":\"" + type + "\",\"recurring\":{\"interval\":\"month\",\"interval_count\":1}}"));
            assertThrows(ResponseStatusException.class, () -> controller.checkout(auth));
        }
        verify(stripe, never()).create(any(), any());
    }
    @Test void unpaidInitialSubscriptionNeverGrantsAccess() throws Exception {
        Payment p = existingSubscription(); p.status = "pending"; p.memberUntil = null;
        when(stripe.subscription("sub_test")).thenReturn(mapper.readTree(subscriptionJson("incomplete", false, "{\"status\":\"open\",\"amount_paid\":0}")));
        signedEvent("customer.subscription.created", "{\"id\":\"sub_test\"}");
        assertEquals(false, controller.membership(auth).get("active"));
        assertNull(p.memberUntil);
    }

}
