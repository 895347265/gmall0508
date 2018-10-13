package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OrderInfo;

public interface OrderService {
    void saveOrder(OrderInfo orderInfo);

    String getTradeCode(String userId);

    boolean checkTradeCode(String userId, String tradeCode);

    OrderInfo getOrderInfoByOutTradeNo(String out_trade_no);


    OrderInfo updateOrderInfo(OrderInfo orderInfo);

    void sendOrderResultQueue(OrderInfo orderInfo);
}

