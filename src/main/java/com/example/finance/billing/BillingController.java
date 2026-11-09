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
    public Map<String, String> checkout(Authentication auth) {
        User user = user(auth);
        if (!(boolean) config().get("enabled")) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "支付尚未开放");
        if ((boolean) membership(auth).get("active")) throw new ResponseStatusException(HttpStatus.CONFLICT, "你已是会员");
        JsonNode price = stripe.price(priceId);
        if (!price.path("active").asBoolean() || price.path("unit_amount").asLong() != 1500 ||
            !"usd".equals(price.path("currency").asText()) || !"one_time".equals(price.path("type").asText()))
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "会员价格配置不正确");
        Payment p = new Payment();
        p.id = UUID.randomUUID().toString(); p.userId = user.getId(); p.status = "pending"; p.createdAt = Instant.now();
        payments.save(p);
        var session = stripe.create(Map.of(
            "mode", "payment", "line_items[0][price]", priceId, "line_items[0][quantity]", "1",
            "client_reference_id", p.id, "metadata[order_id]", p.id,
            "customer_email", user.getEmail(),
            "success_url", baseUrl + "/dashboard/billing?session_id={CHECKOUT_SESSION_ID}",
            "cancel_url", baseUrl + "/dashboard/billing?canceled=1"), p.id);
        p.sessionId = session.path("id").asText(); payments.save(p);
        return Map.of("url", session.path("url").asText());
    }
    @GetMapping("/payments")
    public List<Payment> history(Authentication auth) { return payments.findByUserIdOrderByCreatedAtDesc(user(auth).getId()); }
    @GetMapping("/membership")
    public Map<String, Boolean> membership(Authentication auth) {
        return Map.of("active", payments.findByUserIdOrderByCreatedAtDesc(user(auth).getId()).stream().anyMatch(p -> "paid".equals(p.status)));
    }
    @PostMapping("/webhook")
    public Map<String, Boolean> webhook(@RequestBody String payload,
        @RequestHeader(value = "Stripe-Signature", required = false) String signature) throws java.io.IOException {
        if (webhookSecret.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        if (!StripeSignature.valid(payload, signature, webhookSecret)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "签名无效");
        JsonNode event = mapper.readTree(payload);
        String type = event.path("type").asText();
        if (Set.of("checkout.session.completed", "checkout.session.async_payment_succeeded", "checkout.session.async_payment_failed", "checkout.session.expired").contains(type)) {
            JsonNode object = event.path("data").path("object");
            String orderId = object.path("metadata").path("order_id").asText();
            var order = payments.findById(orderId);
            if (order.isPresent()) {
                Payment p = order.get();
                // Retrieve current state to tolerate duplicate and out-of-order events.
                JsonNode session = stripe.retrieve(object.path("id").asText());
                if (!orderId.equals(session.path("client_reference_id").asText()) ||
                    (p.sessionId != null && !p.sessionId.equals(session.path("id").asText())))
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
                if (!"paid".equals(p.status)) {
                    p.sessionId = session.path("id").asText();
                    if ("paid".equals(session.path("payment_status").asText()) &&
                        session.path("amount_total").asLong() == 1500 && "usd".equals(session.path("currency").asText())) {
                        p.status = "paid"; p.amount = session.path("amount_total").asLong(); p.currency = session.path("currency").asText();
                    } else if ("expired".equals(session.path("status").asText())) p.status = "expired";
                    else if ("checkout.session.async_payment_failed".equals(type)) p.status = "failed";
                    payments.save(p);
                }
            }
        }
        return Map.of("received", true);
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
