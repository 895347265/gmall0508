package com.atguigu.gmall.order.orderMq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.rpc.cluster.merger.MapMerger;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@Component
public class OrderPaymentSuccessQueueListener {


    @Autowired
    OrderService orderService;

    @JmsListener(destination = "PAYMENT_SUCCUSS_QUEUE",containerFactory = "jmsQueueListener")
    public void consumePaymentResult(MapMessage mapMessage){

        try {
            String outTradeNo = mapMessage.getString("outTradeNo");
            String trackingNo = mapMessage.getString("trackingNo");

            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setOutTradeNo(outTradeNo);
            orderInfo.setTrackingNo(trackingNo);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
            Calendar instance = Calendar.getInstance();
            instance.add(Calendar.DATE,3);
            Date time = instance.getTime();
            orderInfo.setExpectDeliveryTime(time);
            orderInfo.setOrderStatus("订单已支付");
            orderInfo.setProcessStatus("订单已支付");
            OrderInfo orderInfo1 = orderService.updateOrderInfo(orderInfo);

            //发送消息队列通知订单系统
            orderService.sendOrderResultQueue(orderInfo1);
        } catch (JMSException e) {
            e.printStackTrace();
        }

    }





}
