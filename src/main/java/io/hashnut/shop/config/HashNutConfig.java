package io.hashnut.shop.config;

import io.hashnut.client.HashNutClient;
import io.hashnut.client.HashNutClientImpl;
import io.hashnut.service.HashNutService;
import io.hashnut.service.HashNutServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HashNutConfig {
    // configure of hashnut,should in database
    public static final boolean testMode=true;
    public static final String chainCode="polygon-erc20";   // erc20,trc20,bep20,polygon-erc20
    public static final String coinCode="usdt";             // usdt,usdc,busd
    public static final String accessKeyId="01HRKDJ93KCNDPEPX9G87E0T33";
    public static final String secretKey ="nPCY9Q477Cl3tVulk1kYCPPyY1y114yN";
    public static final String receiptAddress="0xc7ac968908ed4c99538c8bed5371b94cbbe6d0c7".toLowerCase();

    // backend notify url is configured as https://testnet-web3.hashnut.io/shop/hashNutWebhook
    // frontend callback url is configured as https://testnet-web3.hashnut.io/shop/payFinish

    @Bean
    HashNutService hashNutService(){
        HashNutClient hashNutClient=new HashNutClientImpl(secretKey, testMode);
        return new HashNutServiceImpl(hashNutClient);
    }
}
