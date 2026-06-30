package io.hashnut.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.hashnut.client.HashNutClient;
import io.hashnut.client.HashNutClientImpl;
import io.hashnut.client.HashNutClientResponse;
import io.hashnut.model.request.CreateOrderRequest;
import io.hashnut.service.HashNutService;
import io.hashnut.service.HashNutServiceImpl;

import java.util.UUID;

/**
 * 独立测试：通过 API Key 创建订单，打印完整的 request body 和 response body。
 *
 * 使用方法：直接运行 main 方法，无需 Spring 和数据库。
 * 请先填入你的 accessKeyId、secretKey、splitterAddress。
 */
public class CreateOrderApiTest {

    // ============ 请填入你的配置 ============
    static final String BASE_URL         = "https://defi.hashnut.io/api/v4.0.0";
    static final String ACCESS_KEY_ID    = "<your-access-key-id>";
    static final String SECRET_KEY       = "<your-secret-key>";
    static final String SPLITTER_ADDRESS = "<your-splitter-address>";
    static final String CHAIN_CODE       = "trc20";
    static final String COIN_CODE        = "usdt";
    static final String AMOUNT           = "0.01";
    // ======================================

    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        String merchantOrderId = UUID.randomUUID().toString().substring(0, 20);

        // 构建请求
        CreateOrderRequest request = new CreateOrderRequest.Builder()
                .withAccessKeyId(ACCESS_KEY_ID)
                .withMerchantOrderId(merchantOrderId)
                .withChainCode(CHAIN_CODE)
                .withCoinCode(COIN_CODE)
                .withAmount(AMOUNT)
                .withSplitterAddress(SPLITTER_ADDRESS)
                .withSubject("Test Order - API Doc")
                .withExpireDuration(600L)
                .build();

        // 打印 Request Body
        String requestBody = mapper.writeValueAsString(request);
        System.out.println("========== REQUEST ==========");
        System.out.println("POST " + BASE_URL + request.getUri());
        System.out.println("Content-Type: application/json");
        System.out.println("Headers: hashnut-request-uuid, hashnut-request-timestamp, hashnut-request-sign (auto-generated)");
        System.out.println();
        System.out.println(requestBody);

        // 使用包装的 client 来捕获原始响应
        LoggingClient loggingClient = new LoggingClient(SECRET_KEY, BASE_URL);
        HashNutService service = new HashNutServiceImpl(loggingClient);

        System.out.println();
        System.out.println("========== RESPONSE ==========");
        try {
            var response = service.createOrder(request);
            // loggingClient 已经打印了原始 response
            System.out.println();
            System.out.println("========== PARSED ==========");
            System.out.println("payOrderId:     " + response.getData().getPayOrderId());
            System.out.println("merchantOrderId:" + response.getData().getMerchantOrderId());
            System.out.println("receiptAddress: " + response.getData().getReceiptAddress());
            System.out.println("accessSign:     " + response.getData().getAccessSign());
            System.out.println("state:          " + response.getData().getState());
            System.out.println("amount:         " + response.getData().getAmount());
            System.out.println("chainCode:      " + response.getData().getChainCode());
            System.out.println("coinCode:       " + response.getData().getCoinCode());
        } catch (Exception e) {
            // loggingClient 已经打印了原始 response
            System.out.println();
            System.out.println("========== ERROR ==========");
            System.out.println(e.getMessage());
        }
    }

    /**
     * 包装 HashNutClientImpl，在发送请求前后打印原始 HTTP 信息
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
                // 格式化打印 JSON
                ObjectMapper mapper = new ObjectMapper();
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                Object json = mapper.readValue(resp.getBody(), Object.class);
                System.out.println(mapper.writeValueAsString(json));
            } catch (Exception e) {
                // 非 JSON 直接打印
                System.out.println(resp.getBody());
            }
            return resp;
        }
    }
}
