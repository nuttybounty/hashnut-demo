package io.hashnut.demo.controller;

import io.hashnut.demo.config.HashNutConfig;
import io.hashnut.demo.model.Order;
import io.hashnut.demo.model.Product;
import io.hashnut.exception.HashNutException;
import io.hashnut.model.HashNutOrder;
import io.hashnut.model.OrderState;
import io.hashnut.model.request.ConfirmPaidRequest;
import io.hashnut.model.request.CreateOrderRequest;
import io.hashnut.model.request.QueryOrderRequest;
import io.hashnut.model.response.CreateOrderResponse;
import io.hashnut.model.response.QueryOrderResponse;
import io.hashnut.service.HashNutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.*;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final JdbcTemplate jdbc;
    private final HashNutConfig hashNutConfig;

    public OrderController(JdbcTemplate jdbc, HashNutConfig hashNutConfig) {
        this.jdbc = jdbc;
        this.hashNutConfig = hashNutConfig;
    }

    @PostMapping("/orders")
    public Map<String, Object> createOrder(@RequestBody Map<String, Object> body) {
        Integer productId = (Integer) body.get("productId");
        String blockChain = (String) body.get("blockChain");
        String tokenSymbol = (String) body.get("tokenSymbol");

        if (productId == null) {
            return Collections.singletonMap("error", "productId is required");
        }
        if (blockChain == null || blockChain.isEmpty() || tokenSymbol == null || tokenSymbol.isEmpty()) {
            return Collections.singletonMap("error", "blockChain and tokenSymbol are required");
        }

        // 查商品
        Product product;
        try {
            product = jdbc.queryForObject(
                    "SELECT id, name, description, price, image_url FROM products WHERE id = ?",
                    (rs, rowNum) -> {
                        Product p = new Product();
                        p.setId(rs.getInt("id"));
                        p.setName(rs.getString("name"));
                        p.setDescription(rs.getString("description"));
                        p.setPrice(rs.getBigDecimal("price").stripTrailingZeros().toPlainString());
                        p.setImageUrl(rs.getString("image_url"));
                        return p;
                    }, productId);
        } catch (EmptyResultDataAccessException e) {
            return Collections.singletonMap("error", "product not found");
        }

        // 查 API Key + splitter
        Map<String, Object> apiKeyRow;
        try {
            apiKeyRow = jdbc.queryForMap(
                    "SELECT splitter, access_key_id, secret_key FROM t_hashnut_api_key WHERE block_chain = ?",
                    blockChain);
        } catch (EmptyResultDataAccessException e) {
            return Collections.singletonMap("error", "unsupported chain: " + blockChain);
        }

        String splitter = (String) apiKeyRow.get("splitter");
        String accessKeyId = (String) apiKeyRow.get("access_key_id");
        String secretKey = (String) apiKeyRow.get("secret_key");

        // 用对应链的 secretKey 获取 SDK Service
        HashNutService service = hashNutConfig.getService(secretKey);

        String orderNo = UUID.randomUUID().toString();

        CreateOrderResponse payResp;
        try {
            payResp = service.createOrder(new CreateOrderRequest.Builder()
                    .withAccessKeyId(accessKeyId)
                    .withMerchantOrderId(orderNo)
                    .withBlockChain(blockChain)
                    .withTokenSymbol(tokenSymbol)
                    .withAmount(product.getPrice())
                    .withSplitterAddress(splitter)
                    .withSubject(product.getName())
                    .withExpireDuration(600L)
                    .build());
        } catch (HashNutException e) {
            log.error("HashNut CreateOrder failed: {}", e.getMessage());
            return Collections.singletonMap("error", "create payment order failed: " + e.getMessage());
        }

        HashNutOrder payOrder = payResp.getData();

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setProductId(product.getId());
        order.setAmount(product.getPrice());
        order.setBlockChain(blockChain);
        order.setTokenSymbol(tokenSymbol);
        order.setPayOrderId(payOrder.getPayOrderId());
        order.setAccessSign(payOrder.getAccessSign());
        order.setReceiptAddress(payOrder.getReceiptAddress());
        // 后端可能不返回 payUrl，按 Go SDK 逻辑自动构建
        String payUrl = payOrder.getPayUrl();
        if (payUrl == null || payUrl.isEmpty()) {
            payUrl = hashNutConfig.buildPayUrl(payOrder.getPayOrderId(), orderNo, payOrder.getAccessSign(), blockChain);
        }
        order.setPayUrl(payUrl);
        order.setStatus("paying");

        LocalDateTime now = LocalDateTime.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO orders (order_no, product_id, amount, block_chain, token_symbol, pay_order_id, access_sign, receipt_address, pay_url, status, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id, created_at, updated_at",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, order.getOrderNo());
            ps.setInt(2, order.getProductId());
            ps.setBigDecimal(3, new java.math.BigDecimal(order.getAmount()));
            ps.setString(4, order.getBlockChain());
            ps.setString(5, order.getTokenSymbol());
            ps.setString(6, order.getPayOrderId());
            ps.setString(7, order.getAccessSign());
            ps.setString(8, order.getReceiptAddress());
            ps.setString(9, order.getPayUrl());
            ps.setString(10, order.getStatus());
            ps.setTimestamp(11, Timestamp.valueOf(now));
            ps.setTimestamp(12, Timestamp.valueOf(now));
            return ps;
        }, keyHolder);

        Map<String, Object> keys = keyHolder.getKeys();
        if (keys != null) {
            order.setId(((Number) keys.get("id")).intValue());
            order.setCreatedAt(((Timestamp) keys.get("created_at")).toLocalDateTime());
            order.setUpdatedAt(((Timestamp) keys.get("updated_at")).toLocalDateTime());
        }

        return Collections.singletonMap("data", order);
    }

    @GetMapping("/orders/{id}")
    public Map<String, Object> getOrder(@PathVariable("id") String orderNo) {
        Order order;
        try {
            order = findOrderByOrderNo(orderNo);
        } catch (EmptyResultDataAccessException e) {
            return Collections.singletonMap("error", "order not found");
        }

        // If still paying, query HashNut for latest status
        if ("paying".equals(order.getStatus()) && order.getPayOrderId() != null && !order.getPayOrderId().isEmpty()) {
            // 查订单对应链的 secretKey
            HashNutService service = getServiceForChain(order.getBlockChain());
            if (service != null) {
                try {
                    QueryOrderResponse queryResp = service.queryOrder(new QueryOrderRequest.Builder()
                            .withPayOrderId(order.getPayOrderId())
                            .withMerchantOrderId(order.getOrderNo())
                            .withAccessSign(order.getAccessSign())
                            .build());
                    HashNutOrder payOrder = queryResp.getData();
                    String newStatus = mapHashNutState(payOrder.getState());
                    if (!newStatus.equals(order.getStatus())) {
                        updateOrderStatus(order.getOrderNo(), newStatus, payOrder.getPayTxId());
                        order.setStatus(newStatus);
                        order.setPayTxId(payOrder.getPayTxId());
                    }
                } catch (HashNutException e) {
                    log.warn("Query order failed: {}", e.getMessage());
                }
            }
        }

        return Collections.singletonMap("data", order);
    }

    @PostMapping("/orders/{id}/confirm")
    public Map<String, Object> confirmPaid(@PathVariable("id") String orderNo, @RequestBody Map<String, String> body) {
        String payTxId = body.get("payTxId");
        if (payTxId == null || payTxId.isEmpty()) {
            return Collections.singletonMap("error", "payTxId is required");
        }

        Order order;
        try {
            order = findOrderByOrderNo(orderNo);
        } catch (EmptyResultDataAccessException e) {
            return Collections.singletonMap("error", "order not found");
        }

        HashNutService service = getServiceForChain(order.getBlockChain());
        if (service == null) {
            return Collections.singletonMap("error", "chain config not found: " + order.getBlockChain());
        }

        try {
            service.confirmPaid(new ConfirmPaidRequest.Builder()
                    .withPayOrderId(order.getPayOrderId())
                    .withMerchantOrderId(order.getOrderNo())
                    .withAccessSign(order.getAccessSign())
                    .withPayTxId(payTxId)
                    .build());
        } catch (HashNutException e) {
            return Collections.singletonMap("error", "confirm failed: " + e.getMessage());
        }

        updateOrderStatus(orderNo, "paying", payTxId);
        return Collections.singletonMap("msg", "ok");
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

    private Order findOrderByOrderNo(String orderNo) {
        return jdbc.queryForObject(
                "SELECT o.id, o.order_no, o.product_id, o.amount, o.block_chain, o.token_symbol, " +
                "o.pay_order_id, o.access_sign, o.receipt_address, o.pay_url, o.pay_tx_id, " +
                "o.status, o.created_at, o.updated_at, p.name AS product_name " +
                "FROM orders o JOIN products p ON o.product_id = p.id WHERE o.order_no = ?",
                (rs, rowNum) -> {
                    Order o = new Order();
                    o.setId(rs.getInt("id"));
                    o.setOrderNo(rs.getString("order_no"));
                    o.setProductId(rs.getInt("product_id"));
                    o.setAmount(rs.getBigDecimal("amount").stripTrailingZeros().toPlainString());
                    o.setBlockChain(rs.getString("block_chain"));
                    o.setTokenSymbol(rs.getString("token_symbol"));
                    o.setPayOrderId(rs.getString("pay_order_id"));
                    o.setAccessSign(rs.getString("access_sign"));
                    o.setReceiptAddress(rs.getString("receipt_address"));
                    o.setPayUrl(rs.getString("pay_url"));
                    o.setPayTxId(rs.getString("pay_tx_id"));
                    o.setStatus(rs.getString("status"));
                    o.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    o.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                    o.setProductName(rs.getString("product_name"));
                    return o;
                }, orderNo);
    }

    private void updateOrderStatus(String orderNo, String status, String payTxId) {
        jdbc.update("UPDATE orders SET status = ?, pay_tx_id = COALESCE(?, pay_tx_id), updated_at = NOW() WHERE order_no = ?",
                status, payTxId, orderNo);
    }

    private String mapHashNutState(Integer state) {
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
}
