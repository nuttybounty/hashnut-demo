package io.hashnut.shop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration()
public class HashNutConfig {
    // configure of hashnut,should in database
    @Value("${hashnut.chain}")
    public String chain;
    @Value("${hashnut.chainCode}")
    public String chainCode;
    @Value("${hashnut.coinCode}")
    public String coinCode;
    @Value("${hashnut.serviceType}")
    public int serviceType;
    @Value("${hashnut.serviceVersion}")
    public String serviceVersion;
    @Value("${hashnut.serviceId}")
    public int serviceId;
    @Value("${hashnut.mchAddress}")
    public String mchAddress;
    @Value("${hashnut.accessKeyId}")
    public String accessKeyId;
    @Value("${hashnut.requestKey}")
    public String requestKey;
    @Value("${hashnut.responseKey}")
    public String responseKey;
    @Value("${hashnut.receiptAddress}")
    public String receiptAddress;
}
