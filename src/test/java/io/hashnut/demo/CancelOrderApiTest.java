package io.hashnut.demo;

import io.hashnut.model.request.CancelOrderRequest;
import io.hashnut.service.HashNutService;

/**
 * 独立测试：取消订单，打印完整的 request body 和 response body。
 *
 * 使用方法：先运行 CreateOrderApiTest 获取 payOrderId，
 * 填入下方常量，然后运行 main 方法。
 * 注意：只有 INIT(0) 状态的订单才能取消。
 */
public class CancelOrderApiTest {

    // ============ 从 CreateOrderApiTest 输出中获取 ============
    static final String PAY_ORDER_ID = "<从创建订单响应中获取>";
    // =========================================================

    public static void main(String[] args) throws Exception {
        CancelOrderRequest request = new CancelOrderRequest.Builder()
                .withPayOrderId(PAY_ORDER_ID)
                .build();

        String requestBody = TestConfig.MAPPER.writeValueAsString(request);
        System.out.println("========== REQUEST ==========");
        System.out.println("POST " + TestConfig.BASE_URL + request.getUri());
        System.out.println("Content-Type: application/json");
        System.out.println("Headers: hashnut-request-uuid, hashnut-request-timestamp, hashnut-request-sign (auto-generated)");
        System.out.println();
        System.out.println(requestBody);

        HashNutService service = TestConfig.createLoggingService();

        System.out.println();
        System.out.println("========== RESPONSE ==========");
        try {
            service.cancelOrder(request);
            System.out.println();
            System.out.println("========== RESULT ==========");
            System.out.println("Cancel success");
        } catch (Exception e) {
            System.out.println();
            System.out.println("========== ERROR ==========");
            System.out.println(e.getMessage());
        }
    }
}
