package vn.fs.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.paypal.base.rest.OAuthTokenCredential;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaypalWebhookService {

    private final OAuthTokenCredential oauth;
    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${paypal.mode:sandbox}") private String mode;
    @Value("${paypal.webhookId}")    private String webhookId;

    private String apiBase() {
        return "live".equalsIgnoreCase(mode)
                ? "https://api.paypal.com"
                : "https://api.sandbox.paypal.com";
    }

    private String getAccessToken() throws Exception {
        return oauth.getAccessToken();
    }

    @SuppressWarnings("unchecked")
    public boolean verify(Map<String, String> lowerHeaders, String rawBody) {
        try {
            String token = getAccessToken();

            HttpHeaders h = new HttpHeaders();
            h.setBearerAuth(token);
            h.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> webhookEvent = mapper.readValue(rawBody, Map.class);

            Map<String, Object> body = new HashMap<>();
            body.put("transmission_id",   lowerHeaders.get("paypal-transmission-id"));
            body.put("transmission_time", lowerHeaders.get("paypal-transmission-time"));
            body.put("cert_url",          lowerHeaders.get("paypal-cert-url"));
            body.put("auth_algo",         lowerHeaders.get("paypal-auth-algo"));
            body.put("transmission_sig",  lowerHeaders.get("paypal-transmission-sig"));
            body.put("webhook_id",        webhookId);
            body.put("webhook_event",     webhookEvent);

            ResponseEntity<Map> res = rest.postForEntity(
                    apiBase()+"/v1/notifications/verify-webhook-signature",
                    new HttpEntity<>(body, h),
                    Map.class
            );

            Object status = (res.getBody() == null) ? null : res.getBody().get("verification_status");
            return "SUCCESS".equalsIgnoreCase(String.valueOf(status));
        } catch (Exception e) {
            return false;
        }
    }
}
