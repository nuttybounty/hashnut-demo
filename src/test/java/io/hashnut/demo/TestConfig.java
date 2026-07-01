package io.hashnut.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.hashnut.client.HashNutClient;
import io.hashnut.client.HashNutClientImpl;
import io.hashnut.client.HashNutClientResponse;
import io.hashnut.service.HashNutService;
import io.hashnut.service.HashNutServiceImpl;

/**
 * 测试公共配置和工具类。
 * 请填入你的 accessKeyId、secretKey、splitterAddress。
 */
public class TestConfig {

    // ============ 请填入你的配置 ============
    public static final String BASE_URL         = "https://defi.hashnut.io/api/v4.0.0";
    public static final String ACCESS_KEY_ID    = "<your-access-key-id>";
    public static final String SECRET_KEY       = "<your-secret-key>";
    public static final String SPLITTER_ADDRESS = "<your-splitter-address>";
    public static final String BLOCK_CHAIN      = "TRON";
    public static final String TOKEN_SYMBOL     = "usdt";
    public static final String AMOUNT           = "0.01";
    // ======================================

    public static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public static HashNutService createLoggingService() {
        return new HashNutServiceImpl(new LoggingClient(SECRET_KEY, BASE_URL));
    }

    /**
     * 包装 HashNutClientImpl，打印原始 HTTP 响应
     */
    static class LoggingClient implements HashNutClient {
        private final HashNutClientImpl delegate;

        LoggingClient(String secretKey, String baseUrl) {
            this.delegate = new HashNutClientImpl(secretKey, baseUrl);
        }

        @Override
        public HashNutClientResponse request(String uri, String body, boolean needSign) {
            HashNutClientResponse resp = delegate.request(uri, body, needSign);
            System.out.println("HTTP " + resp.getCode());
            try {
                Object json = MAPPER.readValue(resp.getBody(), Object.class);
                System.out.println(MAPPER.writeValueAsString(json));
            } catch (Exception e) {
                System.out.println(resp.getBody());
            }
            return resp;
        }
    }
}
