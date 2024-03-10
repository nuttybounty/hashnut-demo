package io.hashnut.shop.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hashnut.exception.HashNutException;
import io.hashnut.model.HashNutOrder;
import io.hashnut.model.OrderState;
import io.hashnut.model.request.CreateOrderRequest;
import io.hashnut.model.request.QueryOrderRequest;
import io.hashnut.model.response.CreateOrderResponse;
import io.hashnut.model.response.QueryOrderResponse;
import io.hashnut.service.HashNutService;
import io.hashnut.shop.config.HashNutConfig;
import io.hashnut.shop.dao.model.GoodsOrder;
import io.hashnut.shop.service.GoodsOrderService;
import io.hashnut.shop.util.Constant;
import io.hashnut.util.PayDigestUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/shop")
public class GoodsOrderController {

    private final ObjectMapper objectMapper;
    private final HashNutService hashNutService;
    private final GoodsOrderService goodsOrderService;

    public GoodsOrderController(ObjectMapper objectMapper,
                                HashNutService hashNutService,
                                GoodsOrderService goodsOrderService) {
        this.objectMapper=objectMapper;
        this.hashNutService = hashNutService;
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

    // verify webhook sign
    private boolean verifyWebhook(HttpServletRequest request, String body){
        try{
            String uuid=request.getHeader("hashnut-webhook-uuid");
            String timeStamp=request.getHeader("hashnut-webhook-timestamp");
            String sign=request.getHeader("hashnut-webhook-sign");
            String dataToSign=String.format("%s%s%s",uuid,timeStamp,body);
            String sign1= PayDigestUtil.HMACSHA256Base64(HashNutConfig.secretKey,dataToSign);
            return sign1.equals(sign);
        }catch (Exception e){
            return false;
        }
    }

    @PostMapping(value = "/buy")
    @ResponseBody
    public GoodsOrder buy(@RequestParam("goodsId") String goodsId,
                          @RequestParam("goodsName") String goodsName,
                          @RequestParam("amount") BigDecimal amount){

        final String merchantOrderId = UUID.randomUUID().toString();
        try{
            CreateOrderResponse response = hashNutService.createOrder(new CreateOrderRequest.Builder()
                    .withAccessKeyId(HashNutConfig.accessKeyId)
                    .withMerchantOrderId(merchantOrderId)
                    .withChainCode(HashNutConfig.chainCode)
                    .withCoinCode(HashNutConfig.coinCode)
                    .withAmount(amount)
                    .withReceiptAddress(HashNutConfig.receiptAddress)
                    .build());
            HashNutOrder hashNutOrder=response.getData();

            // record order info to database
            GoodsOrder goodsOrder = new GoodsOrder();
            goodsOrder.setMerchantOrderId(merchantOrderId);
            goodsOrder.setPayOrderId(hashNutOrder.getPayOrderId());
            goodsOrder.setAccessSign(hashNutOrder.getAccessSign());
            goodsOrder.setGoodsId(goodsId);
            goodsOrder.setGoodsName(goodsName);
            goodsOrder.setChain(hashNutOrder.getChain());
            goodsOrder.setChainCode(hashNutOrder.getChainCode());
            goodsOrder.setCoinCode(hashNutOrder.getCoinCode());
            goodsOrder.setAmount(hashNutOrder.getAmount());
            goodsOrder.setReceiptAddress(HashNutConfig.receiptAddress);
            goodsOrder.setChannelId("0");
            goodsOrder.setUserId("user_000001");
            goodsOrder.setStatus(Constant.GOODS_ORDER_STATUS_INIT);

            int result = goodsOrderService.addGoodsOrder(goodsOrder);
            log.info("insert into order return {}", result);
            return goodsOrder;
        }catch (Exception e){
            log.error("create hashnut order for goods id [{}] merchant order id [{}] exception",
                    goodsId,merchantOrderId,e);
            return null;
        }
    }

    @PostMapping(value="/hashNutWebhook")
    public void hashNutWebhook(HttpServletRequest request,
                          HttpServletResponse response) throws Exception {
        log.info("hashnut notify process start");

        // verify sign, NOTE: must read body as a String not json
        String body=readBodyFromRequest(request);
        if(!verifyWebhook(request,body)){
            log.info("hashnut notify process failed");
            outResult(response,"failed");
            return;
        }

        // query pay order detail info and process pay order
        JsonNode jsonNode=objectMapper.readTree(body);
        String payOrderId=jsonNode.get("payOrderId").asText();
        String merchantOrderId=jsonNode.get("merchantOrderId").asText();
        String accessSign=jsonNode.get("accessSign").asText();

        QueryOrderResponse hashnutResponse=hashNutService.queryOrder(new QueryOrderRequest.Builder()
                .withPayOrderId(payOrderId)
                .withMerchantOrderId(merchantOrderId)
                .withAccessSign(accessSign)
                .build());
        HashNutOrder hashNutOrder=hashnutResponse.getData();

        if(hashNutOrder.getState() >= OrderState.SUCCESS){
            log.info("hashnut pay order success");
            goodsOrderService.updateOrderState(merchantOrderId, Constant.GOODS_ORDER_STATUS_SUCCESS);
        }
        if(hashNutOrder.getState() < OrderState.INIT){
            log.info("hashnut pay order failed state {}",hashNutOrder.getState());
            goodsOrderService.updateOrderState(merchantOrderId, Constant.GOODS_ORDER_STATUS_FAIL);
        }
        log.info("hashnut notify process success");
        outResult(response,"success");
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