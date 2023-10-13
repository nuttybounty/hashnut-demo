package io.hashnut.shop.controller;

import com.alibaba.fastjson.JSONObject;
import io.hashnut.client.HashNutClient;
import io.hashnut.client.HashNutClientImpl;
import io.hashnut.exception.HashNutException;
import io.hashnut.model.HashNutOrder;
import io.hashnut.model.request.CreateOrderRequest;
import io.hashnut.model.request.QueryOrderRequest;
import io.hashnut.model.response.Response;
import io.hashnut.service.HashNutService;
import io.hashnut.service.HashNutServiceImpl;
import io.hashnut.shop.dao.model.GoodsOrder;
import io.hashnut.shop.service.GoodsOrderService;
import io.hashnut.shop.util.Constant;
import io.hashnut.util.HashNutUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/shop")
public class GoodsOrderController {

    private final HashNutClient hashNutClient;
    private final HashNutService hashNutService;
    private final GoodsOrderService goodsOrderService;

    // configure of hashnut,should in database
    @Value("${hashnut.chain}")
    private String chain;
    @Value("${hashnut.chainCode}")
    private String chainCode;
    @Value("${hashnut.coinCode}")
    private String coinCode;
    @Value("${hashnut.serviceType}")
    private int serviceType;
    @Value("${hashnut.serviceVersion}")
    private String serviceVersion;
    @Value("${hashnut.serviceId}")
    private int serviceId;
    @Value("${hashnut.mchAddress}")
    private String mchAddress;
    @Value("${hashnut.accessKeyId}")
    private String accessKeyId;
    @Value("${hashnut.requestKey}")
    private String requestKey;
    @Value("${hashnut.responseKey}")
    private String responseKey;
    @Value("${hashnut.receiptAddress}")
    private String receiptAddress;

    public GoodsOrderController(GoodsOrderService goodsOrderService) {
        this.hashNutClient = new HashNutClientImpl(requestKey,responseKey,true);
        this.hashNutService = new HashNutServiceImpl(hashNutClient);
        this.goodsOrderService = goodsOrderService;
    }

    // NOTE: read body as a String not json
    private String readBodyFromRequest(HttpServletRequest request){
        try{
            InputStream inputStream = request.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder requestBody = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }
            return requestBody.toString();
        }catch (Exception e){
            return null;
        }
    }

    // verify request sign
    private boolean verifyNotify(HttpServletRequest request){
        String body=readBodyFromRequest(request);
        String uuid=request.getHeader("webhook-uuid");
        String timeStamp=request.getHeader("webhook-timestamp");
        String sign=request.getHeader("webhook-sign");
        String dataToSign=uuid+","+timeStamp+","+body;
        String sign1= HashNutUtil.hmacSha256Base64(responseKey,dataToSign);

        assert sign1 != null;
        return sign1.equals(sign);
    }

    @PostMapping(value = "/buy")
    @ResponseBody
    public GoodsOrder buy(@RequestParam("goodsId") String goodsId,
                          @RequestParam("goodsName") String goodsName,
                          @RequestParam("amount") String amountString) throws HashNutException {
        // generate merchant order no
        final String goodsOrderId = UUID.randomUUID().toString();
        BigDecimal amountD=new BigDecimal(amountString);
        BigInteger amount=amountD.multiply(BigDecimal.TEN.pow(6)).toBigInteger();

        // create hashnut order
        Response<HashNutOrder> orderResponse=hashNutService.request(new CreateOrderRequest.Builder()
                .withChain(chain)
                .withMchAddress(mchAddress)
                .withAccessKeyId(accessKeyId)
                .withMchOrderNo(goodsOrderId)
                .withChainCode(chainCode)
                .withCoinCode(coinCode)
                .withAccessChannel(0)
                .withAmount(amount.toString())
                .withReceiptContractAddress(receiptAddress)
                .build());
        HashNutOrder hashNutOrder=orderResponse.data;

        // record order info to database
        GoodsOrder goodsOrder = new GoodsOrder();
        goodsOrder.setPayOrderId(hashNutOrder.getPlatformId());
        goodsOrder.setGoodsOrderId(goodsOrderId);
        goodsOrder.setGoodsId(goodsId);
        goodsOrder.setGoodsName(goodsName);
        goodsOrder.setChain(chain);
        goodsOrder.setChainCode(chainCode);
        goodsOrder.setCoinCode(coinCode);
        goodsOrder.setAmount(amount.longValue());
        goodsOrder.setAccessSign(hashNutOrder.getAccessSign());
        goodsOrder.setChannelId("0");
        goodsOrder.setUserId("user_000001");
        goodsOrder.setStatus(Constant.GOODS_ORDER_STATUS_INIT);
        int result = goodsOrderService.addGoodsOrder(goodsOrder);
        log.info("insert into order return {}", result);
        return goodsOrder;
    }

    @PostMapping(value = "/query")
    @ResponseBody
    public GoodsOrder query(@RequestParam("goodsOrderId") String goodsOrderId) {
        return goodsOrderService.queryGoodsOrder(goodsOrderId);
    }

    @PostMapping(value="/payNotify")
    public void payNotify(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("hashnut notify process start");

        // verify sign, NOTE: must read body as a String not json
        String body=readBodyFromRequest(request);
        if(!verifyNotify(request)){
            log.info("hashnut notify process failed");
            outResult(response,"failed");
            return;
        }

        // query pay order detail info and process pay order
        JSONObject jsonObject=JSONObject.parseObject(body);
        String platformId=jsonObject.getString("platformId");
        String mchOrderNo=jsonObject.getString("mchOrderNo");
        String accessSign=jsonObject.getString("accessSign");
        Response<HashNutOrder> orderResponse=hashNutService.request(new QueryOrderRequest.Builder()
                .withPlatformId(platformId)
                .withMchOrderNo(mchOrderNo)
                .withAccessSign(accessSign)
                .build());
        HashNutOrder hashNutOrder=orderResponse.data;
        if(hashNutOrder.getState() < 0){
            log.info("hashnut pay order failed state {}",hashNutOrder.getState());
            goodsOrderService.updateOrderState(mchOrderNo, Constant.GOODS_ORDER_STATUS_FAIL);
        }

        log.info("hashnut notify process success");
        outResult(response,"success");
    }

    void outResult(HttpServletResponse response, String content) {
        response.setContentType("text/html");
        PrintWriter pw;
        try {
            pw = response.getWriter();
            pw.print(content);
            log.info("response complete.");
        } catch (IOException e) {
            log.error("response write exception.",e);
        }
    }
}