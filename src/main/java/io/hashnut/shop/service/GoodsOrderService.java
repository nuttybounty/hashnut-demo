package io.hashnut.shop.service;

import io.hashnut.shop.dao.mapper.GoodsOrderMapper;
import io.hashnut.shop.dao.model.GoodsOrder;
import io.hashnut.shop.dao.model.GoodsOrderExample;
import io.hashnut.shop.util.Constant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GoodsOrderService {

    @Autowired
    private GoodsOrderMapper goodsOrderMapper;

    public int addGoodsOrder(GoodsOrder goodsOrder) {
        return goodsOrderMapper.insertSelective(goodsOrder);
    }

    public int updateOrderState(String goodsOrderId,byte state){
        GoodsOrder goodsOrder = new GoodsOrder();
        goodsOrder.setStatus(state);
        GoodsOrderExample example = new GoodsOrderExample();
        GoodsOrderExample.Criteria criteria = example.createCriteria();
        criteria.andGoodsOrderIdEqualTo(goodsOrderId);

        return goodsOrderMapper.updateByExampleSelective(goodsOrder, example);
    }
}
