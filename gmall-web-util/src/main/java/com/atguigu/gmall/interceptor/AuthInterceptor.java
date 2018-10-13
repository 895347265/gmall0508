package com.atguigu.gmall.interceptor;

import com.atguigu.gmall.annotation.LoginRequire;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.HttpClientUtil;
import com.atguigu.gmall.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //通过handler方法反射获得注解
        HandlerMethod handlerMethod = (HandlerMethod)handler;
        LoginRequire methodAnnotation = handlerMethod.getMethodAnnotation(LoginRequire.class);
        System.out.println(methodAnnotation);

        //判断是否有注解 没有注解的直接通过
        if (methodAnnotation == null){
            //不需要登录就可以直接访问的
            return true;
        }else{

            String token ="";
            String oldToken = CookieUtil.getCookieValue(request, "oldToken", true);//从用户cookie中获取token，说明用户登录过一次
            String newToken = request.getParameter("token");//从浏览器地址栏中获取token
            System.out.println(newToken);

            // oldToken为空  newToken 为空  表示从未登录过

            // oldToken为空  newToken 不为空  表示第一次登录过
            if (StringUtils.isBlank(oldToken)&&StringUtils.isNotBlank(newToken)){
                token = newToken;
             }
            // oldToken不为空  newToken 为空  表示之前登录过
            if (StringUtils.isNotBlank(oldToken)&&StringUtils.isBlank(newToken)){
                token = oldToken;
            }

            // oldToken不为空  newToken 不为空  表示之前的cookie的oldToken过期了
            if (StringUtils.isNotBlank(oldToken)&&StringUtils.isNotBlank(newToken)){
                token = newToken;
            }


            //验证token
            if (StringUtils.isNotBlank(token)){

                String ip=getMyIpFromRequest(request);

                //调用远程认证中心passport，验证token，
                String url = "http://passport.gmall.com:8085/verify?token="+token+"&currentIp="+getMyIpFromRequest(request);

                String success = HttpClientUtil.doGet(url);//一个基于http的rest风格的webservice请求

                if ("success".equals(success)){
                    //将token写入到浏览器的cookie中,刷新用户的token过期时间
                    CookieUtil.setCookie(request,response,"oldToken",token,1000*60*60*24,true);


                    //将用户信息放入请求中
                    Map<String, Object> atguigugmall = JwtUtil.decode(token,"atguigugmall", ip);
                    request.setAttribute("userId",atguigugmall.get("userId"));
                    request.setAttribute("nickName",atguigugmall.get("nickName"));
                    System.out.println(token);
                    return true;
                }

            }

            //如果有注解还要判断是否需要登录才能访问
            boolean b = methodAnnotation.needSuccess();
            if (b){
                //必须登录才能访问的
                response.sendRedirect("http://passport.gmall.com:8085/index?returnUrl="+request.getRequestURL());
                return false;
            }else{
                //不一定需要登录就能访问的
                return true;
            }
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
