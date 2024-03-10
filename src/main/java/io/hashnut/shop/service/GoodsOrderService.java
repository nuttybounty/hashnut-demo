package io.hashnut.shop.service;

import io.hashnut.shop.dao.mapper.GoodsOrderMapper;
import io.hashnut.shop.dao.model.GoodsOrder;
import io.hashnut.shop.dao.model.GoodsOrderExample;
import io.hashnut.shop.util.Constant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GoodsOrderService {

    private final GoodsOrderMapper goodsOrderMapper;

    public GoodsOrderService(GoodsOrderMapper goodsOrderMapper) {
        this.goodsOrderMapper = goodsOrderMapper;
    }

    public int addGoodsOrder(GoodsOrder goodsOrder) {
        return goodsOrderMapper.insertSelective(goodsOrder);
    }

    public int updateOrderState(String merchantOrderId,Integer state){
        GoodsOrder goodsOrder = new GoodsOrder();
        goodsOrder.setStatus(state);
        GoodsOrderExample example = new GoodsOrderExample();
        GoodsOrderExample.Criteria criteria = example.createCriteria();
        criteria.andMerchantOrderIdEqualTo(merchantOrderId);

        return goodsOrderMapper.updateByExampleSelective(goodsOrder, example);
    }

    public GoodsOrder queryGoodsOrder(String goodsOrderId){
        return goodsOrderMapper.selectByPrimaryKey(goodsOrderId);
    }

}
