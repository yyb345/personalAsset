package com.example.finance.billing;

import com.example.finance.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
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
        when(stripe.price("price_test")).thenReturn(mapper.readTree("{\"active\":true,\"unit_amount\":99,\"currency\":\"usd\",\"type\":\"one_time\"}"));
        when(stripe.create(any(), any())).thenReturn(mapper.readTree("{\"id\":\"cs_test_mock\",\"url\":\"https://checkout.stripe.com/mock\"}"));
        assertEquals("https://checkout.stripe.com/mock", controller.checkout(auth).get("url"));
        verify(stripe).create(argThat(fields -> "price_test".equals(fields.get("line_items[0][price]")) && "payment".equals(fields.get("mode"))), any());
    }
    @Test void rejectsForgedWebhookBeforeReadingOrders() {
        assertThrows(ResponseStatusException.class, () -> controller.webhook("{}", "t=1,v1=bad"));
        verifyNoInteractions(payments, stripe);
    }
}
