package com.atguigu.gmall.passport.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.JwtUtil;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Controller
public class PassprotController {

    @Reference
    UserService userService;

    @Reference
    CartService cartService;


    /**
     * 跳入登录页面
     * @param returnUrl
     * @param map
     * @return
     */
    @RequestMapping("index")
    public String index(String returnUrl,ModelMap map){
        map.put("originUrl",returnUrl);
        return  "index";
    }


    /**
     * 用户登录
     * @param map
     * @param request
     * @return  返回一个token值
     */
    @RequestMapping("login")
    @ResponseBody
    public String login(UserInfo userInfo, ModelMap map, HttpServletRequest request, HttpServletResponse response){

        //验证用户名密码
       UserInfo user= userService.login(userInfo);

       if (user == null){
           return "err";
       }else {

           //如果验证成功，根据用户名和密码生成token，然后将该用户的用户信息从db中提取到redis中，设置该用户的过期时间
           Map <String,String> userMap = new HashMap<>();
           userMap.put("userId",user.getId());
           userMap.put("nickName",user.getNickName());
           String ip=getMyIpFromRequest(request);
           String token = JwtUtil.encode("atguigugmall", userMap, ip);
           String userId = user.getId();

           //合并购物车
           //从cookie中取出购物车数据
           String listCartCookie = CookieUtil.getCookieValue(request, "listCartCookie", true);
           //把cookie中的数据保存到db中
           List<CartInfo> cartInfos = JSON.parseArray(listCartCookie, CartInfo.class);
           //通过消息服务，发送异步的消息通知，并行处理购物车合并业务
           cartService.combine(userId,cartInfos);
           //删除cookie中购物车的数据
           CookieUtil.deleteCookie(request,response,"listCartCookie");


           return  token;
       }

    }

    /**
     * 验证token
     * @param request
     * @return
     */
    @RequestMapping("verify")
    @ResponseBody
    public String verify(HttpServletRequest request,String currentIp,String token,ModelMap map){

        try{
            Map<String, Object> atguigugmall = JwtUtil.decode(token,"atguigugmall", currentIp);
            if (atguigugmall != null ){
                //验证cookie的过期时间


                return "success";
            }else {
                return "fail";
            }
        }catch (Exception e){
            return"fail";
        }


    }

    /**
     * 获取客户端ip
     * @param request
     * @return
     */
    private String getMyIpFromRequest(HttpServletRequest request) {
        String ip = "";
        ip = request.getRemoteAddr();
        if (StringUtils.isBlank(ip)){
            ip = request.getHeader("X-Forwarded-For");
            if (StringUtils.isBlank(ip)){
                ip="127.0.0.1";
            }
        }
        return ip;
    }



}
