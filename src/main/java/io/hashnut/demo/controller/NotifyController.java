package io.hashnut.demo.controller;

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

    public NotifyController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostMapping(value = "/notify", produces = "text/plain")
    public String handleNotify(@RequestBody Map<String, Object> payload) {
        String payOrderId = (String) payload.get("payOrderId");
        String merchantOrderId = (String) payload.get("merchantOrderId");
        Integer state = (Integer) payload.get("state");
        String payTxId = (String) payload.get("payTxId");

        log.info("[Notify] payOrderId={} merchantOrderId={} state={} payTxId={}",
                payOrderId, merchantOrderId, state, payTxId);

        // Find order by payOrderId
        String orderNo;
        String currentStatus;
        try {
            Map<String, Object> row = jdbc.queryForMap(
                    "SELECT order_no, status FROM orders WHERE pay_order_id = ?", payOrderId);
            orderNo = (String) row.get("order_no");
            currentStatus = (String) row.get("status");
        } catch (EmptyResultDataAccessException e) {
            log.warn("[Notify] order not found for payOrderId={}", payOrderId);
            return "success";
        }

        String status;
        if (state != null && (state == 3 || state == 4)) {
            status = "paid";
        } else if (state != null && state == -1) {
            status = "failed";
        } else if (state != null && state == -2) {
            status = "expired";
        } else if (state != null && state == -3) {
            status = "canceled";
        } else {
            status = currentStatus;
        }

        jdbc.update("UPDATE orders SET status = ?, pay_tx_id = COALESCE(?, pay_tx_id), updated_at = NOW() WHERE order_no = ?",
                status, payTxId, orderNo);

        return "success";
    }
}
