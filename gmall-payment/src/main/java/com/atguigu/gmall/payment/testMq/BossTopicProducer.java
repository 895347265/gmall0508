package com.atguigu.gmall.payment.testMq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.*;

public class BossTopicProducer {

    public static void main(String[] args) {
        // 队列模式的消息生产者
        ConnectionFactory connect = new ActiveMQConnectionFactory("tcp://localhost:61616");
        try {
            Connection connection = connect.createConnection();
            connection.start();
            //第一个值表示是否使用事务，如果选择true，第二个值相当于选择0
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);

            // 消息对象
            Topic topic = session.createTopic("KAIDAHUI");
            MessageProducer producer = session.createProducer(topic);

            // 消息内容
            TextMessage textMessage=new ActiveMQTextMessage();
            textMessage.setText("为尚硅谷的伟大复兴而努力奋斗！");
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
