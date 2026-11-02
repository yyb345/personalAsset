package com.example.finance.billing;

import org.junit.jupiter.api.Test;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import static org.junit.jupiter.api.Assertions.*;

class StripeSignatureTest {
    private String sign(String body, long timestamp) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("whsec_test".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "t=" + timestamp + ",v1=" + HexFormat.of().formatHex(mac.doFinal((timestamp + "." + body).getBytes(StandardCharsets.UTF_8)));
    }
    @Test void verifiesRawPayloadAndRejectsTampering() throws Exception {
        String header = sign("{}", Instant.now().getEpochSecond());
        assertTrue(StripeSignature.valid("{}", header, "whsec_test"));
        assertFalse(StripeSignature.valid("{ }", header, "whsec_test"));
        assertFalse(StripeSignature.valid("{}", header, "wrong"));
    }
    @Test void rejectsExpiredMissingAndMalformedSignatures() throws Exception {
        assertFalse(StripeSignature.valid("{}", sign("{}", Instant.now().getEpochSecond() - 600), "whsec_test"));
        assertFalse(StripeSignature.valid("{}", null, "whsec_test"));
        assertFalse(StripeSignature.valid("{}", "t=no,v1=broken", "whsec_test"));
    }
}
