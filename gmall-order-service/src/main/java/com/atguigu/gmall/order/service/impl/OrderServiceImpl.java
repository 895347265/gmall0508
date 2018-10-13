package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.util.ActiveMQUtil;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {


    @Autowired
    OrderInfoMapper orderInfoMapper;

    @Autowired
    OrderDetailMapper orderDetailMapper;

    @Autowired
    RedisUtil redisUtil;


    @Autowired
    ActiveMQUtil activeMQUtil;

    /**
     * 保存订单
     * @param orderInfo
     */
    @Override
    public void saveOrder(OrderInfo orderInfo) {
        orderInfoMapper.insertSelective(orderInfo);
        String orderId = orderInfo.getId();

        //根据orderId插入订单详情表
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderId);
            orderDetailMapper.insertSelective(orderDetail);
        }
    }

    /**
     * 生成交易码
     * @param userId 用userId做key存入到redis中 十五分钟过期
     * @return 返回一个String类型的交易码
     */
    @Override
    public String getTradeCode(String userId) {
        //生成一个交易码
        String tradeCode = UUID.randomUUID().toString();
        //存到redis中
        Jedis jedis = redisUtil.getJedis();
        jedis.setex("user:"+userId+":tradeCode",1000*60*15,tradeCode);
        jedis.close();
        return  tradeCode;
    }


    /**
     * 检查交易码是否跟redis中的交易码相同
     * @param userId
     * @param tradeCode
     * @return
     */
    @Override
    public boolean checkTradeCode(String userId, String tradeCode) {
        boolean b = false;

        Jedis jedis = redisUtil.getJedis();

        String tradeCodeRedis = jedis.get("user:" + userId + ":tradeCode");

        if (tradeCodeRedis != null&& tradeCode.equals(tradeCodeRedis)){
            b=true;
            jedis.del("user:" + userId + ":tradeCode");
        }
        jedis.close();
        return b;
    }


    /**
     * 根据OutTradeNo外部订单号查询订单信息
     * @param out_trade_no
     * @return
     */
    @Override
    public OrderInfo getOrderInfoByOutTradeNo(String out_trade_no) {

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOutTradeNo(out_trade_no);
        OrderInfo orderInfo1 = orderInfoMapper.selectOne(orderInfo);

        String orderInfoId = orderInfo1.getId();

        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderInfoId);
        List<OrderDetail> orderDetailList = orderDetailMapper.select(orderDetail);

        orderInfo1.setOrderDetailList(orderDetailList);


        return orderInfo1;
    }

    @Override
    public OrderInfo updateOrderInfo(OrderInfo orderInfo) {

        Example example = new Example(OrderInfo.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("outTradeNo",orderInfo.getOutTradeNo());

        orderInfoMapper.updateByExampleSelective(orderInfo,example);
        OrderInfo orderInfos = orderInfoMapper.selectOne(orderInfo);

        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderInfos.getId());
        List<OrderDetail> orderDetails = orderDetailMapper.select(orderDetail);
        orderInfos.setOrderDetailList(orderDetails);
        return orderInfos;


    }

    @Override
    public void sendOrderResultQueue(OrderInfo orderInfo) {

        try {
            Connection connection= activeMQUtil.getConnection();
            connection.start();
            //第一个值表示是否使用事务，如果选择true，第二个值相当于选择0
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);

            // 消息对象
            Queue testqueue = session.createQueue("ORDER_RESULT_QUEUE");
            MessageProducer producer = session.createProducer(testqueue);

            // 消息内容
            ActiveMQTextMessage textMessage = new ActiveMQTextMessage();
            textMessage.setText(JSON.toJSONString(orderInfo));
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            // 发出消息
            producer.send(textMessage);
            session.commit();
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }



    }


}
