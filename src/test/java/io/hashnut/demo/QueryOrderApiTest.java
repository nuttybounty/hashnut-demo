package io.hashnut.demo;

import io.hashnut.model.request.QueryOrderRequest;
import io.hashnut.service.HashNutService;

/**
 * 独立测试：查询订单，打印完整的 request body 和 response body。
 *
 * 使用方法：先运行 CreateOrderApiTest 获取 payOrderId、merchantOrderId、accessSign，
 * 填入下方常量，然后运行 main 方法。
 */
public class QueryOrderApiTest {

    // ============ 从 CreateOrderApiTest 输出中获取 ============
    static final String PAY_ORDER_ID      = "<从创建订单响应中获取>";
    static final String MERCHANT_ORDER_ID = "<从创建订单响应中获取>";
    static final String ACCESS_SIGN       = "<从创建订单响应中获取>";
    // =========================================================

    public static void main(String[] args) throws Exception {
        QueryOrderRequest request = new QueryOrderRequest.Builder()
                .withPayOrderId(PAY_ORDER_ID)
                .withMerchantOrderId(MERCHANT_ORDER_ID)
                .withAccessSign(ACCESS_SIGN)
                .build();

        String requestBody = TestConfig.MAPPER.writeValueAsString(request);
        System.out.println("========== REQUEST ==========");
        System.out.println("POST " + TestConfig.BASE_URL + request.getUri());
        System.out.println("Content-Type: application/json");
        System.out.println("Auth: body accessSign (no header signing needed)");
        System.out.println();
        System.out.println(requestBody);

        HashNutService service = TestConfig.createLoggingService();

        System.out.println();
        System.out.println("========== RESPONSE ==========");
        try {
            var response = service.queryOrder(request);
            System.out.println();
            System.out.println("========== PARSED ==========");
            System.out.println("payOrderId:      " + response.getData().getPayOrderId());
            System.out.println("merchantOrderId: " + response.getData().getMerchantOrderId());
            System.out.println("state:           " + response.getData().getState());
            System.out.println("amount:          " + response.getData().getAmount());
            System.out.println("receiptAddress:  " + response.getData().getReceiptAddress());
            System.out.println("payTxId:         " + response.getData().getPayTxId());
            System.out.println("chainCode:       " + response.getData().getChainCode());
            System.out.println("coinCode:        " + response.getData().getCoinCode());
        } catch (Exception e) {
            System.out.println();
            System.out.println("========== ERROR ==========");
            System.out.println(e.getMessage());
        }
    }
}
