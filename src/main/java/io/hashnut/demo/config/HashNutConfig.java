package io.hashnut.demo.config;

import io.hashnut.client.HashNutClient;
import io.hashnut.client.HashNutClientImpl;
import io.hashnut.service.HashNutService;
import io.hashnut.service.HashNutServiceImpl;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ConcurrentHashMap;

@Configuration
@ConfigurationProperties(prefix = "hashnut")
public class HashNutConfig {

    private boolean testMode = true;
    private String baseUrl;

    // 按 secretKey 缓存 SDK Service 实例，避免重复创建
    private final ConcurrentHashMap<String, HashNutService> serviceCache = new ConcurrentHashMap<>();

    public HashNutService getService(String secretKey) {
        return serviceCache.computeIfAbsent(secretKey, key -> {
            HashNutClient client;
            if (baseUrl != null && !baseUrl.isEmpty()) {
                client = new HashNutClientImpl(key, baseUrl);
            } else {
                client = new HashNutClientImpl(key, testMode);
            }
            return new HashNutServiceImpl(client);
        });
    }

    /**
     * 构建 HashNut 支付页面 URL（与 Go SDK buildPayURL 逻辑一致）
     */
    public String buildPayUrl(String payOrderId, String merchantOrderId, String accessSign, String chainCode) {
        String apiBase;
        if (baseUrl != null && !baseUrl.isEmpty()) {
            apiBase = baseUrl;
        } else if (testMode) {
            apiBase = HashNutClientImpl.TESTNET_URL;
        } else {
            apiBase = HashNutClientImpl.PRODUCTION_URL;
        }
        // "https://defi.hashnut.io/api/v4.0.0" → "https://defi.hashnut.io/pay"
        String payBase = apiBase.replaceFirst("/api/v[0-9.]+$", "") + "/pay";
        return payBase
                + "?payOrderId=" + payOrderId
                + "&merchantOrderId=" + merchantOrderId
                + "&accessSign=" + accessSign
                + "&chainCode=" + chainCode
                + "&payApiVersion=4";
    }

    public void setTestMode(boolean testMode) { this.testMode = testMode; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
}
