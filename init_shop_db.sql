create database shop_db default character set utf8mb4 collate utf8mb4_unicode_ci;
use shop_db;

DROP TABLE IF EXISTS t_goods_order;
CREATE TABLE `t_goods_order` (
                                 `GoodsOrderId` varchar(64) NOT NULL COMMENT '商品订单ID',
                                 `GoodsId` varchar(30) NOT NULL COMMENT '商品ID',
                                 `GoodsName` varchar(64) NOT NULL DEFAULT '' COMMENT '商品名称',
                                 `Amount` bigint(64) NOT NULL COMMENT '商品价格',
                                 `UserId` varchar(30) NOT NULL COMMENT '用户ID',
                                 `Status` tinyint(6) NOT NULL DEFAULT '0' COMMENT '订单状态,订单生成(0),支付成功(1),处理完成(2),处理失败(-1)',
                                 `PayOrderId` varchar(30) DEFAULT NULL COMMENT '支付订单号',
                                 `ChannelId` varchar(24) DEFAULT NULL COMMENT '渠道ID',
                                 `ChannelUserId` varchar(64) DEFAULT NULL COMMENT '支付渠道用户ID',
                                 `Chain` varchar(64) DEFAULT NULL COMMENT '链，ETH,TRON,BSC,POLYGON等',
                                 `ChainCode` varchar(64) DEFAULT NULL COMMENT '币种标准，ERC20,TRC20,BEP20等',
                                 `CoinCode` varchar(64) DEFAULT NULL COMMENT '币种代号，USDT,USDC,DAI等',
                                 `AccessSign` varchar(130) DEFAULT NULL COMMENT 'AccessSign',
                                 `CreateTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `UpdateTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 PRIMARY KEY (`GoodsOrderId`),
                                 UNIQUE KEY `IDX_PayOrderId` (`PayOrderId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品订单表';

create user shop@localhost;
set password for shop@localhost=password('123456');
flush privileges;
grant all privileges on shop_db.* to shop@localhost;
flush privileges;