package vn.fs.config;

import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.OAuthTokenCredential;
import com.paypal.base.rest.PayPalRESTException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class PaypalConfig {

    @Value("${paypal.mode:sandbox}")
    private String mode;

    @Value("${paypal.client.app:}")
    private String clientId;

    @Value("${paypal.client.secret:}")
    private String clientSecret;

    @Bean
    public Map<String, String> paypalSdkConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("mode", mode);
        return config;
    }

    @Bean
    public OAuthTokenCredential authTokenCredential(Map<String, String> paypalSdkConfig) {
        return new OAuthTokenCredential(clientId, clientSecret, paypalSdkConfig);
    }

    @Bean
    public APIContext apiContext(OAuthTokenCredential authTokenCredential) throws PayPalRESTException {
        // Lấy access token thủ công
        String accessToken = authTokenCredential.getAccessToken();
        APIContext context = new APIContext(accessToken);
        context.setConfigurationMap(paypalSdkConfig());
        return context;
    }
}
