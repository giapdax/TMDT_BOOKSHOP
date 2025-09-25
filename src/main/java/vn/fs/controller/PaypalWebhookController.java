package vn.fs.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.fs.service.PaypalWebhookService;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PaypalWebhookController {

    private final PaypalWebhookService verifier;
    private final ObjectMapper mapper = new ObjectMapper();

    @PostMapping("/webhook/paypal")
    public ResponseEntity<?> handle(HttpServletRequest req) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = req.getReader()) {
            String line; while ((line = br.readLine()) != null) sb.append(line);
        }
        String raw = sb.toString();

        Map<String,String> headers = new HashMap<>();
        for (Enumeration<String> e = req.getHeaderNames(); e.hasMoreElements();) {
            String name = e.nextElement();
            headers.put(name.toLowerCase(), req.getHeader(name));
        }

        // Verify chữ ký với PayPal
        if (!verifier.verify(headers, raw)) {
            log.warn("PayPal webhook signature FAILED");
            return ResponseEntity.badRequest().body("invalid signature");
        }

        // Đọc event bằng Jackson
        JsonNode root = mapper.readTree(raw);
        String eventType = root.path("event_type").asText("");
        String resourceId = root.path("resource").path("id").asText("");

        log.info("Webhook OK type={} resourceId={}", eventType, resourceId);

        return ResponseEntity.ok().build();
    }
}
