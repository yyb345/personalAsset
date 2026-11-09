package com.example.finance.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class StripeGateway {
    private final ObjectMapper mapper;
    private final String key;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).build();
    public StripeGateway(ObjectMapper mapper, @Value("${stripe.secret-key:}") String key) {
        this.mapper = mapper;
        this.key = key;
    }
    public boolean configured() { return !key.isBlank(); }
    public JsonNode create(Map<String, String> fields, String idempotencyKey) {
        String body = fields.entrySet().stream().map(e -> encode(e.getKey()) + "=" + encode(e.getValue())).collect(Collectors.joining("&"));
        return send(HttpRequest.newBuilder(URI.create("https://api.stripe.com/v1/checkout/sessions"))
            .header("Idempotency-Key", idempotencyKey)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body)));
    }
    public JsonNode price(String id) {
        if (!id.matches("price_[A-Za-z0-9_]+")) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        return send(HttpRequest.newBuilder(URI.create("https://api.stripe.com/v1/prices/" + id)).GET());
    }
    public JsonNode retrieve(String id) {
        if (!id.matches("cs_[A-Za-z0-9_]+")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return send(HttpRequest.newBuilder(URI.create("https://api.stripe.com/v1/checkout/sessions/" + id)).GET());
    }
    public JsonNode subscription(String id) {
        if (!id.matches("sub_[A-Za-z0-9_]+")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return send(HttpRequest.newBuilder(URI.create("https://api.stripe.com/v1/subscriptions/" + id + "?expand[]=latest_invoice")).GET());
    }
    public JsonNode portal(String customerId, String returnUrl) {
        if (!customerId.matches("cus_[A-Za-z0-9_]+")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        String body = "customer=" + encode(customerId) + "&return_url=" + encode(returnUrl);
        return send(HttpRequest.newBuilder(URI.create("https://api.stripe.com/v1/billing_portal/sessions"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body)));
    }
    private JsonNode send(HttpRequest.Builder builder) {
        if (!configured()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "支付尚未配置");
        try {
            var response = client.send(builder.timeout(java.time.Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + key).build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe 服务暂时不可用");
            return mapper.readTree(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "支付请求中断");
        } catch (java.io.IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "无法连接 Stripe");
        }
    }
    private static String encode(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
}
