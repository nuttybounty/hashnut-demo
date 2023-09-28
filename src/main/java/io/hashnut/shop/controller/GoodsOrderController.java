package io.hashnut.shop.controller;

import com.alibaba.fastjson.JSONObject;
import io.hashnut.shop.dao.model.GoodsOrder;
import io.hashnut.shop.service.GoodsOrderService;
import io.hashnut.shop.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

import io.hashnut.sdk.exception.OrderException;
import io.hashnut.sdk.models.*;
import io.hashnut.sdk.config.PayConstant;
import io.hashnut.sdk.util.OrderUtil;


@Slf4j
@Controller
@RequestMapping("/shop")
public class GoodsOrderController {

    @Autowired
    private GoodsOrderService goodsOrderService;

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

    // init environment,dev,testnet,prd
    static {
        PayConstant.initEnv(PayConstant.ENV_DEV);
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

    // generate hmac sign and base64 encode
    private String hmacSha256Base64(String key,String data) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "HmacSHA256");
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(secretKeySpec);
            byte[] signatureBytes = hmac.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            return null;
        }
    }

    // verify request sign
    private boolean verifyRequest(HttpServletRequest request,String body){
        String uuid=request.getHeader("webhook-uuid");
        String timeStamp=request.getHeader("webhook-timestamp");
        String sign=request.getHeader("webhook-sign");
        String dataToSign=uuid+","+timeStamp+","+body;
        String sign1=hmacSha256Base64(responseKey,dataToSign);

        assert sign1 != null;
        return sign1.equals(sign);
    }

    @PostMapping(value="/payNotify")
    public void payNotify(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("hashnut notify process start");

        // verify sign, NOTE: must read body as a String not json
        String body=readBodyFromRequest(request);
        if(!verifyRequest(request,body)){
            log.info("hashnut notify process failed");
            outResult(response,"failed");
            return;
        }

        // query pay order detail info and process pay order
        JSONObject jsonObject=JSONObject.parseObject(body);
        String platformId=jsonObject.getString("platformId");
        String mchOrderNo=jsonObject.getString("mchOrderNo");
        String accessSign=jsonObject.getString("accessSign");

        PayOrder payOrder=OrderUtil.queryPayOrder(platformId,mchOrderNo,accessSign);
        if(payOrder.getState()==PayConstant.PAY_STATUS_SUCCESS
                || payOrder.getState()==PayConstant.PAY_STATUS_FAILED){
            log.info("hashnut pay order success");
            goodsOrderService.updateOrderState(mchOrderNo, Constant.GOODS_ORDER_STATUS_SUCCESS);
        }
        if(payOrder.getState() < 0){
            log.info("hashnut pay order failed state {}",payOrder.getState());
            goodsOrderService.updateOrderState(mchOrderNo, Constant.GOODS_ORDER_STATUS_FAIL);
        }

        log.info("hashnut notify process success");
        outResult(response,"success");
    }

    @PostMapping(value = "/buy")
    @ResponseBody
    public GoodsOrder buy(@RequestParam("goodsId") String goodsId,
                          @RequestParam("goodsName") String goodsName,
                          @RequestParam("amount") String amountString) throws IOException, OrderException {
        // generate merchant order no
        final String goodsOrderId = UUID.randomUUID().toString();
        BigDecimal amountD=new BigDecimal(amountString);
        BigInteger amount=amountD.multiply(BigDecimal.TEN.pow(6)).toBigInteger();

        // parameters required
        PayOrder order=new PayOrder();
        order.setChain(chain);
        order.setMchAddress(mchAddress);
        order.setAccessKeyId(accessKeyId);
        order.setMchOrderNo(goodsOrderId);
        order.setChainCode(chainCode);
        order.setCoinCode(coinCode);
        order.setAccessChannel(PayConstant.ACCESS_CHANNEL_CHAIN);
        order.setAmount(amount);
        order.setReceiptAddress(receiptAddress);

        // parameters optional
        order.setSubject("merchant-subject");
        order.setRemarkInfo("merchant-remark");
        order.setParam1("my-param1");
        order.setParam2("my-param2");

        // call hashnut api to create order
        OrderOutputParam outputParam=OrderUtil.createPayOrder(order, requestKey, serviceType, serviceVersion, serviceId);
        System.out.println("get outputParam " + outputParam.toString());

        // record order info to database
        GoodsOrder goodsOrder = new GoodsOrder();
        goodsOrder.setGoodsOrderId(goodsOrderId);
        goodsOrder.setGoodsId(goodsId);
        goodsOrder.setGoodsName(goodsName);
        goodsOrder.setChain(chain);
        goodsOrder.setChainCode(chainCode);
        goodsOrder.setCoinCode(coinCode);
        goodsOrder.setAmount(amount.longValue());
        goodsOrder.setAccessSign(outputParam.getAccessSign());
        goodsOrder.setChannelId("0");
        goodsOrder.setUserId("user_000001");
        // record hashnut platformId
        goodsOrder.setPayOrderId(outputParam.getPlatformId());
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