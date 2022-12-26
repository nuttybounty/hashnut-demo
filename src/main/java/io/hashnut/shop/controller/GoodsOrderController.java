package io.hashnut.shop.controller;

import io.hashnut.shop.dao.model.GoodsOrder;
import io.hashnut.shop.service.GoodsOrderService;
import io.hashnut.shop.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.hashnut.sdk.exception.OrderException;
import io.hashnut.sdk.models.*;
import io.hashnut.sdk.config.PayConstant;
import io.hashnut.sdk.util.OrderUtil;
import io.hashnut.sdk.util.PayDigestUtil;

@Slf4j
@Controller
@RequestMapping("/shop")
public class GoodsOrderController {

    @Autowired
    private GoodsOrderService goodsOrderService;

    public static final String CHAIN="ETH";
    public static final int SERVICE_TYPE=0;
    public static final String SERVICE_VERSION="PaymentSplitterV2_1";
    public static final int SERVICE_ID=0;
    public static String MCH_ADDRESS ="0xEA997d01742B777F083A4529832450155B3623a6".toLowerCase();
    public static String ACCESS_KEY_ID="ACC_1056989465932726272";
    public static String REQUEST_KEY="bxu9zz9x3wb0yUg9HHsEAzX8RNNb9VsH";
    public static String RESPONSE_KEY="8PjPKWDKmPMmWLKE2sX8pS88VD46f16D";
    public static String RECEIPT_CONTRACT_ADDRESS="0x0e0AB4350306e079399E58a6A98FCeeCB6c9A942".toLowerCase();

    static {
        PayConstant.initEnv(PayConstant.ENV_TEST);
    }

    @GetMapping(value = "/buy/{goodsId}")
    @ResponseBody
    public GoodsOrder buy(@PathVariable("goodsId") String goodsId) throws IOException, OrderException {

        String goodsOrderId = UUID.randomUUID().toString();

        PayOrder order=new PayOrder();
        order.setChain(CHAIN);
        order.setMchAddress(MCH_ADDRESS);
        order.setAccessKeyId(ACCESS_KEY_ID);
        order.setMchOrderNo(goodsOrderId);
        order.setChainCode("erc20");
        order.setCoinCode("usdt");
        order.setAccessChannel(PayConstant.ACCESS_CHANNEL_CHAIN);
        // 10 usdt
        order.setAmount(BigInteger.valueOf(10_000_000L));
        order.setReceiptAddress(RECEIPT_CONTRACT_ADDRESS);

        // 下单接口
        OrderOutputParam outputParam=OrderUtil.createPayOrder(order,REQUEST_KEY,SERVICE_TYPE,SERVICE_VERSION,SERVICE_ID);
        System.out.println("get outputParam " + outputParam.toString());

        // 记录订单信息到本地
        GoodsOrder goodsOrder = new GoodsOrder();
        goodsOrder.setGoodsOrderId(goodsOrderId);
        goodsOrder.setGoodsId(goodsId);
        goodsOrder.setGoodsName("water");
        goodsOrder.setAmount(10_000_000L);
        goodsOrder.setUserId("user_000001");
        // hashnut的平台订单号
        goodsOrder.setPayOrderId(outputParam.getPlatformId());
        goodsOrder.setStatus(Constant.GOODS_ORDER_STATUS_INIT);

        int result = goodsOrderService.addGoodsOrder(goodsOrder);
        log.info("插入商品订单,返回:{}", result);
        return goodsOrder;
    }

    @PostMapping(value="/payNotify")
    public void payNotify(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("====== 开始处理支付中心通知 ======");
        Map<String,Object> paramMap = request2payResponseMap(request, new String[]{
                "orderId","chain","mchAddress","mchOrderNo","chainCode","coinCode","amount","obtainAmount","payTxId","receiptAddress","state", "confirmCount",
                "paySuccTime","subject","param1","param2","remark","backType","accessSign","sign"
        });
        String stateStr=(String)paramMap.get("state");
        String orderId = (String) paramMap.get("orderId");
        String mchOrderNo = (String) paramMap.get("mchOrderNo");
        String payTxId = (String) paramMap.get("payTxId");
        String sign=(String)paramMap.get("sign");
        long confirmCount = Long.parseLong((String)paramMap.get("confirmCount"));
        byte state=Byte.parseByte(stateStr);

        // 将可能带有中文参数的参数进行URL Decode
        String subject=URLDecoder.decode(paramMap.get("subject")==null ? "" : paramMap.get("subject").toString(), StandardCharsets.UTF_8);
        String param1=URLDecoder.decode(paramMap.get("param1")==null ? "" : paramMap.get("param1").toString(), StandardCharsets.UTF_8);
        String param2=URLDecoder.decode(paramMap.get("param2")==null ? "" : paramMap.get("param2").toString(), StandardCharsets.UTF_8);
        String remark=URLDecoder.decode(paramMap.get("remark")==null ? "" : paramMap.get("remark").toString(), StandardCharsets.UTF_8);

        paramMap.put("subject",subject);
        paramMap.put("param1",param1);
        paramMap.put("param2",param2);
        paramMap.put("remark",remark);
        log.info("支付中心通知请求参数,paramMap={}", paramMap);
        // 验签名
        boolean verifyResult=verifyApiKeySign(paramMap,RESPONSE_KEY,sign);
        log.info("支付中心验证签名 {} ",verifyResult);

        switch (stateStr){
            case "1":
                goodsOrderService.updateOrderState(mchOrderNo, Constant.GOODS_ORDER_STATUS_PAID);
                break;
            case "2":
                goodsOrderService.updateOrderState(mchOrderNo, Constant.GOODS_ORDER_STATUS_CONFIRMING);
                break;
            case "3":
                goodsOrderService.updateOrderState(mchOrderNo, Constant.GOODS_ORDER_STATUS_SUCCESS);
                break;
            case "4":
                goodsOrderService.updateOrderState(mchOrderNo, Constant.GOODS_ORDER_STATUS_FINISHED);
                break;
        }

        String resStr="success";
        log.info("响应支付中心通知结果:{},orderId={},mchOrderNo={} payTxId{}  确认次数{}", resStr, orderId, mchOrderNo,payTxId,confirmCount);
        outResult(response, resStr);
        log.info("====== 支付中心通知处理完成 ======");
    }

    void outResult(HttpServletResponse response, String content) {
        response.setContentType("text/html");
        PrintWriter pw;
        try {
            pw = response.getWriter();
            pw.print(content);
            log.info("response xxpay complete.");
        } catch (IOException e) {
            log.error("response xxpay write exception.",e);
        }
    }

    public Map<String, Object> request2payResponseMap(HttpServletRequest request, String[] paramArray) {
        Map<String, Object> responseMap = new HashMap<>();
        for (int i = 0;i < paramArray.length; i++) {
            String key = paramArray[i];
            String v = request.getParameter(key);
            if (v != null) {
                responseMap.put(key, v);
            }
        }
        return responseMap;
    }

    public boolean verifySign(Map<String, Object> map) {
        String mchAddress = (String) map.get("mchAddress");
        String chain=(String)map.get("chain");
        if(!this.MCH_ADDRESS.equals(mchAddress)) return false;
        if(!this.CHAIN.equals(chain)) return false;
        String localSign = PayDigestUtil.getHmacSign256(RESPONSE_KEY,map, "sign");
        String sign = (String) map.get("sign");
        return localSign.equalsIgnoreCase(sign);
    }

    public boolean verifyApiKeySign(Map<String, Object> map,String resKey,String sign){
        String mchAddress = (String) map.get("mchAddress");
        String chain=(String)map.get("chain");
        if(!this.MCH_ADDRESS.equals(mchAddress)) return false;
        if(!this.CHAIN.equals(chain)) return false;
        return PayDigestUtil.verifyHmacSign256(resKey,sign,map,"sign");
    }

}