package com.atguigu.gmall.order.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.annotation.LoginRequire;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.bean.enums.PaymentWay;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.SkuServer;
import com.atguigu.gmall.service.UserService;

import com.sun.org.apache.bcel.internal.generic.NEW;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
public class OrderController {
    
    @Reference
    CartService cartService;

    @Reference
    UserService userService;

    @Reference
    SkuServer skuServer;

    @Reference
    OrderService orderService;




    @LoginRequire(needSuccess = true)
    @RequestMapping("toTrade")
    public String toTrade(HttpServletRequest request, ModelMap map){

        //验证用户是否登录
        String userId =(String)request.getAttribute("userId");
        //根据userId查询到用户的收货地址
        List<UserAddress> userAddressList = userService.getAddressListByUserId(userId);

        List<OrderDetail> orderDetails = new ArrayList<>();
        List<CartInfo> cartInfos = cartService.getCartInfosFromCacheByUserId(userId);
        for (CartInfo cartInfo : cartInfos) {
            if (cartInfo.getIsChecked().equals("1")){
                OrderDetail orderDetail = new OrderDetail();
                orderDetail.setSkuId(cartInfo.getSkuId());
                orderDetail.setImgUrl(cartInfo.getImgUrl());
                orderDetail.setSkuName(cartInfo.getSkuName());
                orderDetail.setSkuNum(cartInfo.getSkuNum());
                orderDetail.setOrderPrice(new BigDecimal(0));
                orderDetails.add(orderDetail);
            }

        }
        BigDecimal totalAmount = getCartPrice(cartInfos);
        map.put("totalAmount",totalAmount);
        map.put("userAddressList",userAddressList);
        map.put("orderDetailList",orderDetails);

        //生成一个唯一的交易码
        String tradeCode = orderService.getTradeCode(userId);
        map.put("tradeCode",tradeCode);
        return "trade";
    }

    /**
     * 计算购物车总价格
     * @param cartInfos
     * @return
     */
    private BigDecimal getCartPrice(List<CartInfo> cartInfos) {
        BigDecimal cartPrice = new BigDecimal("0");

        for (CartInfo cartInfo : cartInfos) {
            if (cartInfo.getIsChecked().equals("1")){
                cartPrice = cartPrice.add(cartInfo.getCartPrice());
            }
        }
        return cartPrice;
    }

    @LoginRequire(needSuccess = true)
    @RequestMapping("submitOrder")
    public String submitOrder(HttpServletRequest request,String tradeCode,String addressId,ModelMap map){

        //获取用户id
        String userId = (String)request.getAttribute("userId");

        //检查交易码是否有效 如果有效跳到支付页面   如果无效跳到错误页面
        boolean b =  orderService.checkTradeCode(userId,tradeCode);
        if (b){
            //获取用户的收货信息
            UserAddress userAddress =  userService.getAddressListById(addressId);

            //获取购物车信息
            List<CartInfo> cartInfos = cartService.getCartInfosFromCacheByUserId(userId);

            //声明订单对象
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setProcessStatus("订单提交");
            orderInfo.setOrderStatus("订单未支付");
            //
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DATE,1);
            orderInfo.setExpireTime(c.getTime());
            //外部订单号
            SimpleDateFormat s = new SimpleDateFormat("yyyyMMddHHmmss");
            String format = s.format(new Date());
            String outTradeNo = "atguigugmall"+format+System.currentTimeMillis();
            orderInfo.setOutTradeNo(outTradeNo);
            orderInfo.setConsigneeTel(userAddress.getPhoneNum());
            orderInfo.setCreateTime(new Date());
            orderInfo.setDeliveryAddress(userAddress.getUserAddress());
            orderInfo.setOrderComment("硅谷快递，即时送达");
            orderInfo.setConsignee(userAddress.getConsignee());
            orderInfo.setTotalAmount(getCartPrice(cartInfos));
            orderInfo.setUserId(userId);
            orderInfo.setPaymentWay(PaymentWay.ONLINE);

            //封装订单详情
            List<String> cartIds = new ArrayList<>();
            List<OrderDetail> orderDetails = new ArrayList<OrderDetail>();
            for (CartInfo cartInfo : cartInfos) {
                String cartId = cartInfo.getId();
                cartIds.add(cartId);
                OrderDetail orderDetail = new OrderDetail();
                orderDetail.setSkuId(cartInfo.getSkuId());
                orderDetail.setSkuName(cartInfo.getSkuName());
                orderDetail.setSkuNum(cartInfo.getSkuNum());
                orderDetail.setImgUrl(cartInfo.getImgUrl());
                //验库存
                orderDetail.setHasStock("1");
                //验价格
                SkuInfo skuInfo = skuServer.getSkuById(cartInfo.getSkuId());
                if (cartInfo.getSkuPrice().compareTo(skuInfo.getPrice())==0){
                    orderDetail.setOrderPrice(cartInfo.getCartPrice());
                }else{
                    return "OrderErr";
                }
                orderDetails.add(orderDetail);
            }
            orderInfo.setOrderDetailList(orderDetails);

            //保存订单
            orderService.saveOrder(orderInfo);

            //删除购物车数据
            cartService.deteleCart(StringUtils.join(cartIds,","),userId);

            //提交订单后重定向到支付系统
            return "redirect:http://payment.gmall.com:8087/index?outTradeNo="+outTradeNo+"&totalAmount="+getCartPrice(cartInfos);
        }else{
            return "OrderErr";
        }


    }


}
