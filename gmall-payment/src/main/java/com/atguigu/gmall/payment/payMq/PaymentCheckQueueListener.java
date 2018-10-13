package com.atguigu.gmall.payment.payMq;

import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;


@Component
public class PaymentCheckQueueListener {

    @Autowired
    PaymentService paymentService;

    /**
     * 检查支付状态的消费端consumer  监听器
     * @param mapMessage
     * @throws JMSException
     */
    @JmsListener(destination = "PAYMENT_CHECK_QUEUE",containerFactory = "jmsQueueListener")
    public void consumeCheckResult(MapMessage mapMessage) throws JMSException {//信息消费端
        //得到外部订单号
        String outTradeNo = mapMessage.getString("outTradeNo");
        //检查次数
        int count = mapMessage.getInt("count");
        //调用支付宝检查接口，得到支付状态
        Map<String, String> tradeStatus = paymentService.checkAlipayPayment(outTradeNo);
        //支付成功
        if("TRADE_SUCCESS".equals(tradeStatus.get("status"))){
            //支付状态的幂等性判断
            boolean b = paymentService.checkStatus(outTradeNo);
            if (!b){
                PaymentInfo paymentInfo = new PaymentInfo();
                paymentInfo.setPaymentStatus("已支付");
                paymentInfo.setCallbackTime(new Date());
                paymentInfo.setAlipayTradeNo(tradeStatus.get("tradeNo"));
                paymentInfo.setCallbackContent(tradeStatus.get("callback"));
                paymentInfo.setOutTradeNo(outTradeNo);
                //更新支付信息
                System.err.println("进行第"+(6-count)+"次检查订单的支付状态，支付成功，更新支付信息发送成功的消息队列");
                paymentService.updatePaymentSuccess(paymentInfo);
            }else{
                System.out.println("检查到该笔交易已经支付完毕，直接返回结果，消息队列任务结束 ");
            }

        }else {
            //根据支付情况决定是否调用支付成功队列，还是继续延时检查
            if (count>0){
                System.err.println("进行第"+(6-count)+"次检查订单的支付状态，当前订单"+outTradeNo+"的支付状态未支付，继续发送延时队列");
                paymentService.sendDelayPaymentResult(outTradeNo,count-1);//信息生产端
            }else {
                System.err.println("检查次数上限，用户在规定时间内，没有支付");
            }
        }
    }





}
