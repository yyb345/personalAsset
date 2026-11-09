package com.example.finance.billing;

import com.example.finance.*;
import com.fasterxml.jackson.databind.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/billing")
public class BillingController {
    private final UserRepository users;
    private final PaymentRepository payments;
    private final StripeGateway stripe;
    private final ObjectMapper mapper;
    private final String priceId, baseUrl, webhookSecret, productName;
    public BillingController(UserRepository users, PaymentRepository payments, StripeGateway stripe, ObjectMapper mapper,
        @Value("${stripe.price-id:}") String priceId,
        @Value("${stripe.base-url:http://localhost:3000}") String baseUrl,
        @Value("${stripe.webhook-secret:}") String webhookSecret,
        @Value("${stripe.product-name:会员}") String productName) {
        this.users = users; this.payments = payments; this.stripe = stripe; this.mapper = mapper;
        this.priceId = priceId; this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.webhookSecret = webhookSecret; this.productName = productName;
    }
    @GetMapping("/config")
    public Map<String, Object> config() {
        return Map.of("enabled", stripe.configured() && !priceId.isBlank() && !webhookSecret.isBlank(), "productName", productName);
    }
    @PostMapping("/checkout")
    public synchronized Map<String, String> checkout(Authentication auth) {
        User user = user(auth);
        if (!(boolean) config().get("enabled")) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "支付尚未开放");
        if ((boolean) membership(auth).get("active")) throw new ResponseStatusException(HttpStatus.CONFLICT, "你已是会员");
        if (history(auth).stream().anyMatch(p -> p.subscriptionId != null && p.subscriptionStatus != null &&
            !Set.of("canceled", "incomplete_expired").contains(p.subscriptionStatus)))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "请管理现有订阅");
        JsonNode price = stripe.price(priceId);
        if (!price.path("active").asBoolean() || price.path("unit_amount").asLong() != 99 ||
            !"usd".equals(price.path("currency").asText()) || !"recurring".equals(price.path("type").asText()) ||
            !"year".equals(price.path("recurring").path("interval").asText()) ||
            price.path("recurring").path("interval_count").asInt() != 1)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "会员价格配置不正确");
        for (Payment existing : history(auth)) {
            if ("pending".equals(existing.status) && existing.sessionId != null) {
                JsonNode previous = stripe.retrieve(existing.sessionId);
                if ("open".equals(previous.path("status").asText()) && "subscription".equals(previous.path("mode").asText()))
                    return Map.of("url", previous.path("url").asText());
                if ("complete".equals(previous.path("status").asText()) && !resourceId(previous.path("subscription")).isBlank())
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "订阅正在确认，请刷新记录");
            }
        }
        Payment p = new Payment();
        p.id = UUID.randomUUID().toString(); p.userId = user.getId(); p.status = "pending"; p.createdAt = Instant.now();
        payments.save(p);
        var session = stripe.create(Map.ofEntries(
            Map.entry("mode", "subscription"), Map.entry("line_items[0][price]", priceId), Map.entry("line_items[0][quantity]", "1"),
            Map.entry("client_reference_id", p.id), Map.entry("metadata[order_id]", p.id),
            Map.entry("subscription_data[metadata][order_id]", p.id), Map.entry("customer_email", user.getEmail()),
            Map.entry("success_url", baseUrl + "/dashboard/billing?session_id={CHECKOUT_SESSION_ID}"),
            Map.entry("cancel_url", baseUrl + "/dashboard/billing?canceled=1")), p.id);
        p.sessionId = session.path("id").asText(); payments.save(p);
        return Map.of("url", session.path("url").asText());
    }
    @GetMapping("/payments")
    public List<Payment> history(Authentication auth) { return payments.findByUserIdOrderByCreatedAtDesc(user(auth).getId()); }
    @GetMapping("/membership")
    public Map<String, Object> membership(Authentication auth) {
        List<Payment> records = history(auth);
        boolean active = records.stream().anyMatch(this::entitled);
        Optional<Payment> subscription = records.stream().filter(p -> p.subscriptionId != null && p.customerId != null).findFirst();
        Map<String, Object> result = new HashMap<>();
        result.put("active", active);
        result.put("canManage", subscription.isPresent());
        result.put("hasOpenSubscription", records.stream().anyMatch(p -> p.subscriptionId != null && p.subscriptionStatus != null &&
            !Set.of("canceled", "incomplete_expired").contains(p.subscriptionStatus)));
        subscription.ifPresent(p -> {
            result.put("subscriptionStatus", p.subscriptionStatus == null ? "" : p.subscriptionStatus);
            result.put("cancelAtPeriodEnd", Boolean.TRUE.equals(p.cancelAtPeriodEnd));
            result.put("memberUntil", p.memberUntil == null ? "" : p.memberUntil.toString());
        });
        return result;
    }
    private boolean entitled(Payment p) {
        // Existing one-time paid memberships retain their original entitlement.
        if (p.subscriptionId == null) return "paid".equals(p.status);
        return p.memberUntil != null && p.memberUntil.isAfter(Instant.now()) &&
            Set.of("active", "past_due").contains(p.subscriptionStatus == null ? "" : p.subscriptionStatus);
    }
    @PostMapping("/portal")
    public Map<String, String> portal(Authentication auth) {
        Payment p = history(auth).stream().filter(record -> record.customerId != null && record.subscriptionId != null)
            .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return Map.of("url", stripe.portal(p.customerId, baseUrl + "/dashboard/billing").path("url").asText());
    }
    @PostMapping("/webhook")
    public synchronized Map<String, Boolean> webhook(@RequestBody String payload,
        @RequestHeader(value = "Stripe-Signature", required = false) String signature) throws java.io.IOException {
        if (webhookSecret.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        if (!StripeSignature.valid(payload, signature, webhookSecret)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "签名无效");
        JsonNode event = mapper.readTree(payload);
        String type = event.path("type").asText();
        JsonNode object = event.path("data").path("object");
        if (Set.of("checkout.session.completed", "checkout.session.async_payment_succeeded", "checkout.session.async_payment_failed", "checkout.session.expired").contains(type)) {
            JsonNode session = stripe.retrieve(object.path("id").asText());
            String orderId = session.path("metadata").path("order_id").asText();
            Optional<Payment> order = payments.findById(orderId);
            if (order.isPresent()) {
                Payment p = order.get();
                if (!orderId.equals(session.path("client_reference_id").asText()) ||
                    (p.sessionId != null && !p.sessionId.equals(session.path("id").asText())))
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
                p.sessionId = session.path("id").asText();
                String subId = resourceId(session.path("subscription"));
                if (!subId.isBlank()) { payments.save(p); syncSubscription(subId); }
                else {
                    if (!"paid".equals(p.status)) {
                        if ("expired".equals(session.path("status").asText())) p.status = "expired";
                        else if ("checkout.session.async_payment_failed".equals(type)) p.status = "failed";
                        payments.save(p);
                    }
                }
            }
        } else if (Set.of("customer.subscription.created", "customer.subscription.updated", "customer.subscription.deleted").contains(type)) {
            syncSubscription(object.path("id").asText());
        } else if (Set.of("invoice.paid", "invoice.payment_failed", "invoice.payment_action_required").contains(type)) {
            String subId = resourceId(object.path("subscription"));
            if (subId.isBlank()) subId = resourceId(object.path("parent").path("subscription_details").path("subscription"));
            if (!subId.isBlank()) syncSubscription(subId);
        }
        return Map.of("received", true);
    }
    private static String resourceId(JsonNode node) { return node.isTextual() ? node.asText() : node.path("id").asText(); }
    private void syncSubscription(String id) {
        // Always query current state so delayed notifications cannot restore canceled access.
        JsonNode sub = stripe.subscription(id);
        String orderId = sub.path("metadata").path("order_id").asText();
        Optional<Payment> found = payments.findById(orderId);
        if (found.isEmpty()) return;
        Payment p = found.get();
        if (!id.equals(sub.path("id").asText()) || (p.subscriptionId != null && !id.equals(p.subscriptionId)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        p.subscriptionId = id;
        p.customerId = resourceId(sub.path("customer"));
        p.subscriptionStatus = sub.path("status").asText();
        p.cancelAtPeriodEnd = sub.path("cancel_at_period_end").asBoolean();
        JsonNode invoice = sub.path("latest_invoice");
        // Extend access only for a confirmed paid annual invoice, never for an unpaid future period.
        if ("paid".equals(invoice.path("status").asText()) && invoice.path("amount_paid").asLong() == 99 &&
            "usd".equals(invoice.path("currency").asText())) {
            for (JsonNode line : invoice.path("lines").path("data")) {
                String linePrice = resourceId(line.path("price"));
                if (linePrice.isBlank()) linePrice = resourceId(line.path("pricing").path("price_details").path("price"));
                long end = line.path("period").path("end").asLong();
                if (priceId.equals(linePrice) && line.path("amount").asLong() == 99 && end > 0) {
                    Instant until = Instant.ofEpochSecond(end);
                    if (p.memberUntil == null || until.isAfter(p.memberUntil)) p.memberUntil = until;
                    p.status = "paid"; p.amount = 99L; p.currency = "usd";
                    // Invoice IDs make renewal payment records idempotent.
                    String invoiceId = invoice.path("id").asText();
                    if (invoiceId.startsWith("in_") && "subscription_cycle".equals(invoice.path("billing_reason").asText()) && !payments.existsById(invoiceId)) {
                        Payment renewal = new Payment();
                        renewal.id = invoiceId; renewal.userId = p.userId; renewal.status = "paid";
                        renewal.amount = 99L; renewal.currency = "usd"; renewal.subscriptionId = id;
                        renewal.createdAt = Instant.ofEpochSecond(invoice.path("created").asLong());
                        payments.save(renewal);
                    }
                }
            }
        }
        payments.save(p);
    }
    private User user(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        Optional<User> found;
        if (auth instanceof OAuth2AuthenticationToken oauth) {
            found = users.findByProviderAndProviderId(oauth.getAuthorizedClientRegistrationId(), oauth.getName());
            if (found.isEmpty()) {
                String email = oauth.getPrincipal().getAttribute("email");
                found = email == null ? Optional.empty() : users.findByEmail(email);
            }
        } else found = users.findByUsername(auth.getName());
        return found.filter(User::isEnabled).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
