create database shop_db default character set utf8mb4 collate utf8mb4_unicode_ci;
use shop_db;

DROP TABLE IF EXISTS t_goods_order;
CREATE TABLE t_goods_order (
    `MerchantOrderId` varchar(64) NOT NULL COMMENT 'goods order id',
    `PayOrderId` varchar(26) DEFAULT NULL COMMENT 'HashNut支付订单号',
    `AccessSign` varchar(64) DEFAULT NULL COMMENT 'HashNut支付订单号',
    `GoodsId` varchar(30) NOT NULL COMMENT '商品ID',
    `GoodsName` varchar(64) NOT NULL DEFAULT '' COMMENT '商品名称',
    `Chain` varchar(64) DEFAULT NULL COMMENT '链，ETH,TRON,BSC,POLYGON等',
    `ChainCode` varchar(64) DEFAULT NULL COMMENT '币种标准，ERC20,TRC20,BEP20等',
    `CoinCode` varchar(64) DEFAULT NULL COMMENT '币种代号，USDT,USDC,DAI等',
    `Amount` decimal(64) NOT NULL COMMENT '商品价格',
    `ReceiptAddress` varchar(42) NOT NULL COMMENT '收款地址',
    `UserId` varchar(30) NOT NULL COMMENT '用户ID',
    `Status` int NOT NULL DEFAULT '0' COMMENT '订单状态,订单生成(0),支付成功(1),处理完成(2),处理失败(-1)',
    `ChannelId` varchar(24) DEFAULT NULL COMMENT '渠道ID',
    `ChannelUserId` varchar(64) DEFAULT NULL COMMENT '支付渠道用户ID',
    `CreateTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UpdateTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`MerchantOrderId`),
    UNIQUE KEY `IDX_PayOrderId` (`PayOrderId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品订单表';
