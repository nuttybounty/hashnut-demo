package io.hashnut.demo.controller;

import io.hashnut.demo.config.HashNutConfig;
import io.hashnut.exception.HashNutException;
import io.hashnut.model.HashNutOrder;
import io.hashnut.model.OrderState;
import io.hashnut.model.request.QueryOrderRequest;
import io.hashnut.model.response.QueryOrderResponse;
import io.hashnut.service.HashNutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class NotifyController {

    private static final Logger log = LoggerFactory.getLogger(NotifyController.class);

    private final JdbcTemplate jdbc;
    private final HashNutConfig hashNutConfig;

    public NotifyController(JdbcTemplate jdbc, HashNutConfig hashNutConfig) {
        this.jdbc = jdbc;
        this.hashNutConfig = hashNutConfig;
    }

    /**
     * HashNut 支付结果回调通知。
     *
     * 回调参数只有: payOrderId, merchantOrderId, accessSign, state
     * 官方推荐做法: 收到通知后，通过 SDK 主动查询订单，以查询结果为准来更新本地状态。
     */
    @PostMapping(value = "/notify", produces = "text/plain")
    public String handleNotify(@RequestBody Map<String, Object> payload) {
        String payOrderId = (String) payload.get("payOrderId");
        String merchantOrderId = (String) payload.get("merchantOrderId");
        String accessSign = (String) payload.get("accessSign");
        Integer notifyState = (Integer) payload.get("state");

        log.info("[Notify] payOrderId={} merchantOrderId={} state={}", payOrderId, merchantOrderId, notifyState);

        // 1. 查找本地订单
        Map<String, Object> orderRow;
        try {
            orderRow = jdbc.queryForMap(
                    "SELECT order_no, status, block_chain FROM orders WHERE pay_order_id = ?", payOrderId);
        } catch (EmptyResultDataAccessException e) {
            log.warn("[Notify] order not found for payOrderId={}", payOrderId);
            return "success";
        }

        String orderNo = (String) orderRow.get("order_no");
        String currentStatus = (String) orderRow.get("status");
        String blockChain = (String) orderRow.get("block_chain");

        // 已终态的订单不再处理
        if ("finish".equals(currentStatus) || "failed".equals(currentStatus)
                || "expired".equals(currentStatus) || "canceled".equals(currentStatus)) {
            log.info("[Notify] order {} already in terminal status: {}", orderNo, currentStatus);
            return "success";
        }

        // 2. 通过 SDK 主动查询订单，以查询结果为准
        HashNutService service = getServiceForChain(blockChain);
        if (service == null) {
            log.warn("[Notify] no SDK service for blockChain={}, skip query", blockChain);
            return "success";
        }

        HashNutOrder payOrder;
        try {
            QueryOrderResponse queryResp = service.queryOrder(new QueryOrderRequest.Builder()
                    .withPayOrderId(payOrderId)
                    .withMerchantOrderId(merchantOrderId)
                    .withAccessSign(accessSign)
                    .build());
            payOrder = queryResp.getData();
        } catch (HashNutException e) {
            log.error("[Notify] query order failed: {}", e.getMessage());
            return "success";
        }

        // 3. 根据查询到的实际状态更新本地订单
        Integer actualState = payOrder.getState();
        String newStatus = mapState(actualState);
        String payTxId = payOrder.getPayTxId();

        log.info("[Notify] order {} query result: state={} payTxId={} -> status={}",
                orderNo, actualState, payTxId, newStatus);

        if (!newStatus.equals(currentStatus)) {
            jdbc.update("UPDATE orders SET status = ?, pay_tx_id = COALESCE(?, pay_tx_id), updated_at = NOW() WHERE order_no = ?",
                    newStatus, payTxId, orderNo);
        }

        return "success";
    }

    private String mapState(Integer state) {
        if (state == null) return "paying";
        switch (state) {
            case OrderState.INIT:
            case OrderState.PAID:
                return "paying";
            case OrderState.CONFIRMING:
                return "confirming";
            case OrderState.SUCCESS:
                return "fund received";
            case OrderState.FINISH:
                return "finish";
            case OrderState.FAILED:
                return "failed";
            case OrderState.EXPIRE:
                return "expired";
            case OrderState.CANCELED:
                return "canceled";
            default:
                return "paying";
        }
    }

    private HashNutService getServiceForChain(String blockChain) {
        try {
            Map<String, Object> row = jdbc.queryForMap(
                    "SELECT secret_key FROM t_hashnut_api_key WHERE block_chain = ?", blockChain);
            return hashNutConfig.getService((String) row.get("secret_key"));
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}
