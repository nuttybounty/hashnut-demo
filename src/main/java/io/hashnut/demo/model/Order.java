package io.hashnut.demo.model;

import java.time.LocalDateTime;

public class Order {
    private int id;
    private String orderNo;
    private int productId;
    private String amount;
    private String blockChain;
    private String tokenSymbol;
    private String payOrderId;
    private String accessSign;
    private String receiptAddress;
    private String payUrl;
    private String payTxId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String productName;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }

    public String getBlockChain() { return blockChain; }
    public void setBlockChain(String blockChain) { this.blockChain = blockChain; }

    public String getTokenSymbol() { return tokenSymbol; }
    public void setTokenSymbol(String tokenSymbol) { this.tokenSymbol = tokenSymbol; }

    public String getPayOrderId() { return payOrderId; }
    public void setPayOrderId(String payOrderId) { this.payOrderId = payOrderId; }

    public String getAccessSign() { return accessSign; }
    public void setAccessSign(String accessSign) { this.accessSign = accessSign; }

    public String getReceiptAddress() { return receiptAddress; }
    public void setReceiptAddress(String receiptAddress) { this.receiptAddress = receiptAddress; }

    public String getPayUrl() { return payUrl; }
    public void setPayUrl(String payUrl) { this.payUrl = payUrl; }

    public String getPayTxId() { return payTxId; }
    public void setPayTxId(String payTxId) { this.payTxId = payTxId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
}
