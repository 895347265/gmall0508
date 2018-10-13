package com.atguigu.gmall.payment.service.impl;


import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.service.PaymentService;
import com.atguigu.gmall.util.ActiveMQUtil;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {
    @Autowired
    PaymentInfoMapper paymentInfoMapper;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Autowired
    AlipayClient alipayClient;

    @Override
    public void savePayment(PaymentInfo paymentInfo) {
        paymentInfoMapper.insertSelective(paymentInfo);

    }

    @Override
    public void updatePayment(PaymentInfo paymentInfo) {
        Example example = new Example(PaymentInfo.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("outTradeNo",paymentInfo.getOutTradeNo());
        paymentInfoMapper.updateByExampleSelective(paymentInfo,example);
    }



    @Override
    public void sendPaymentSuccessQueue(String outTradeNo,String trackingNo) {
        //发送支付成功的消息队列
        try {
            Connection connection= activeMQUtil.getConnection();
            connection.start();
            //第一个值表示是否使用事务，如果选择true，第二个值相当于选择0
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);

            // 消息对象
            Queue testqueue = session.createQueue("PAYMENT_SUCCUSS_QUEUE");
            MessageProducer producer = session.createProducer(testqueue);

            // 消息内容
            ActiveMQMapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setString("outTradeNo",outTradeNo);
            mapMessage.setString("trackingNo",trackingNo);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            // 发出消息
            producer.send(mapMessage);
            session.commit();
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    /**
     * 检查支付宝支付状态
     * @param outTradeNo
     * @return
     */
    @Override
    public Map<String,String> checkAlipayPayment(String outTradeNo){

        Map<String,String> returnMap =new HashMap<>();

        System.err.println("开始检查支付宝的状态，第n次，返回支付结果");
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        Map<String,String> map = new HashMap<>();
        map.put("out_trade_no",outTradeNo);
        String s = JSON.toJSONString(map);
        request.setBizContent(s);

        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            System.out.println("调用成功");
            String tradeNo = response.getTradeNo();//支付宝交易码
            String tradeStatus = response.getTradeStatus();
            if (StringUtils.isNotBlank(tradeStatus)){
                returnMap.put("status",tradeStatus);
                returnMap.put("tradeNo",response.getTradeNo());
                returnMap.put("callback",response.getMsg());
                return returnMap;
            }else {
                returnMap.put("status","fail");
                return returnMap;
            }
        } else {
            System.out.println("用户未创建交易");
            returnMap.put("status","fail");
            return returnMap;
        }
    }

    @Override
    public void updatePaymentSuccess(PaymentInfo paymentInfo) {


        //更新支付信息
        updatePayment(paymentInfo);


        String outTradeNo = paymentInfo.getOutTradeNo();
        String tradeNo = paymentInfo.getAlipayTradeNo();

        //通知订单系统，更新订单信息
       sendPaymentSuccessQueue(outTradeNo,tradeNo);

    }

    /**
     * 检查支付状态是否已经支付
     * @param outTradeNo
     * @return
     */
    @Override
    public boolean checkStatus(String outTradeNo) {
        boolean b = false;
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(outTradeNo);
        PaymentInfo paymentInfo1 = paymentInfoMapper.selectOne(paymentInfo);
        String paymentStatus = paymentInfo.getPaymentStatus();
        if ("已支付".equals(paymentStatus)){
            b =true;
        }
        return b;
    }


    @Override
    public void sendDelayPaymentResult(String outTradeNo, int count) {
        //发送检查支付状态的消息队列
        System.err.println("开始发送延时检查支付状态的队列");
        try {
            Connection connection= activeMQUtil.getConnection();
            connection.start();
            //第一个值表示是否使用事务，如果选择true，第二个值相当于选择0
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);

            // 消息对象
            Queue testqueue = session.createQueue("PAYMENT_CHECK_QUEUE");
            MessageProducer producer = session.createProducer(testqueue);

            // 消息内容
            ActiveMQMapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setString("outTradeNo",outTradeNo);
            mapMessage.setInt("count",count);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            //开启延时检查的
            mapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,1000*30);


            // 发出消息
            producer.send(mapMessage);
            session.commit();
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }

    }
}
