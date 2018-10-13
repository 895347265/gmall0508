package com.atguigu.gmall.payment.controller;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.annotation.LoginRequire;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.payment.conf.AlipayConfig;
import com.atguigu.gmall.service.OrderService;

import com.atguigu.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PaymentController {

    @Autowired
    AlipayClient alipayClient;


    @Reference
    OrderService orderService;

    @Autowired
    PaymentService paymentService;


    @LoginRequire(needSuccess = true)
    @RequestMapping("index")
    public String index(ModelMap map,String outTradeNo,String totalAmount){

        map.put("outTradeNo",outTradeNo);
        map.put("totalAmount",totalAmount);
        return "index";
    }
    @LoginRequire(needSuccess = true)
    @RequestMapping("alipay/submit")
    @ResponseBody
    public String alipay(String outTradeNo,HttpServletResponse httpResponse){

       OrderInfo orderInfo =  orderService.getOrderInfoByOutTradeNo(outTradeNo);


        //设置支付宝page，pay的请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        alipayRequest.setNotifyUrl(AlipayConfig.return_payment_url);//在公共参数中设置回跳和通知地址

        Map<String,Object> map = new HashMap<>();
        map.put("out_trade_no",outTradeNo);
        map.put("product_code","FAST_INSTANT_TRADE_PAY");
        map.put("total_amount",0.01);
        map.put("subject",orderInfo.getOrderDetailList().get(0).getSkuName());
        map.put("body","硅谷支付产品测试");
        String s = JSON.toJSONString(map);

        alipayRequest.setBizContent(s);//填充业务参数
        String form="";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        //保存交易信息
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderId(orderInfo.getId());
        paymentInfo.setOutTradeNo(outTradeNo);
        paymentInfo.setSubject(orderInfo.getOrderDetailList().get(0).getSkuName());
        paymentInfo.setPaymentStatus("未支付");
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        System.out.println(new Date());
        paymentService.savePayment(paymentInfo);

        //发送定时检查的延时队列
        paymentService.sendDelayPaymentResult(outTradeNo,5);


        return form;
    }


    /**
     * 支付成功之后的回调函数
     * @param request
     * @return
     */
    @LoginRequire(needSuccess = true)
    @RequestMapping("alipay/callback/return")
    public String alipayReturn(HttpServletRequest request){



        //回调接口首先需要验证阿里的签名
        try {
            boolean signVerified = AlipaySignature.rsaCheckV1(null, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type);
            if(signVerified){
                // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            }else{
                // TODO 验签失败则记录异常日志，并在response中返回failure.
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


       String tradeNo =  (String)request.getParameter("trade_no");
       String callback = request.getQueryString();
        String outTradeNo= (String)request.getParameter("out_trade_no");


        //更新支付信息
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus("已支付");
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setAlipayTradeNo(tradeNo);
        paymentInfo.setCallbackContent(callback);
        paymentInfo.setOutTradeNo(outTradeNo);
        paymentService.updatePaymentSuccess(paymentInfo);


        //通知订单系统，更新订单信息
        paymentService.sendPaymentSuccessQueue(outTradeNo,tradeNo);

        return "finish";
    }

}
