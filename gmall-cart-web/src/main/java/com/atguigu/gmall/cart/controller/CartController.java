package com.atguigu.gmall.cart.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.atguigu.gmall.annotation.LoginRequire;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.SkuServer;
import com.atguigu.gmall.util.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.cookie.Cookie;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class CartController {


    @Reference
    SkuServer skuServer;

    @Reference
    CartService cartService;




    @LoginRequire(needSuccess = false)
    @RequestMapping("addToCart")
    public String addToCart(HttpServletResponse response, HttpServletRequest request, @RequestParam Map<String, String> map){
        //声明一个处理后的购物车集合对象
        List<CartInfo> cartInfos = new ArrayList<>();


        String skuId = map.get("skuId");
        String skuNum = map.get("num") ;
        //根据skuid查询出skuInfo对象
        SkuInfo skuInfo = skuServer.getSkuById(skuId);

        //创建一个购物车对象 把skuinfo信息放入购物车对象
        CartInfo cartInfo = new CartInfo();
        cartInfo.setCartPrice(skuInfo.getPrice().multiply(new BigDecimal(skuNum)));
        cartInfo.setSkuNum(Integer.parseInt(skuNum));
        cartInfo.setIsChecked("1");
        cartInfo.setSkuId(skuId);
        cartInfo.setSkuName(skuInfo.getSkuName());
        cartInfo.setSkuPrice(skuInfo.getPrice());
        cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());


        String userId=(String)request.getAttribute("userId");

        //判断用户是否登录
        if (StringUtils.isBlank(userId)){
            //用户未登录时添加购物车 cookie
            cartInfo.setUserId("");
            String cookieValue = CookieUtil.getCookieValue(request, "listCartCookie", true);

            if (StringUtils.isBlank(cookieValue)){
                //cookie中没数据
                cartInfos.add(cartInfo);

            }else{
                //cookie中有数据
                cartInfos = JSON.parseArray(cookieValue,CartInfo.class);

                //判断是更新还是添加购物车
                boolean b = if_new_cart(cartInfos,cartInfo);

                if (b){
                    //新增
                    cartInfos.add(cartInfo);
                }else{
                    //更新
                    for (CartInfo info : cartInfos) {
                        if (info.getSkuId().equals(cartInfo.getSkuId())){
                            info.setSkuNum(info.getSkuNum()+cartInfo.getSkuNum());
                            info.setCartPrice(info.getSkuPrice().multiply(new BigDecimal(info.getSkuNum())));
                        }
                    }
                }
            }
            //将数据放入cookie中
            CookieUtil.setCookie(request,response,"listCartCookie",JSON.toJSONString(cartInfos),1000*60*60*24,true);

        }else{
            //用户已登录时添加购物车 db
            cartInfo.setUserId(userId);

           CartInfo cartInfoDb=  cartService.ifCartExits(cartInfo);

           if (cartInfoDb != null){
               //更新
               cartInfoDb.setSkuNum(cartInfoDb.getSkuNum()+cartInfo.getSkuNum());
               cartInfoDb.setCartPrice(cartInfoDb.getSkuPrice().multiply(new BigDecimal(cartInfoDb.getSkuNum())));

               cartService.updateCart(cartInfoDb);

           }else{
               //新增
               cartService.insertCart(cartInfo);
           }

           //同步redis缓存
            cartService.flushCartCacheByUserId(userId);
        }

        return "redirect:/cartSuccess";
    }


    @LoginRequire(needSuccess = false)
    @RequestMapping("cartSuccess")
    public String cartSuccess(){

        return "success";
    }


    /**
     * 判断购物车是更新还是新增
     * @param listCartCookie
     * @param cartInfo
     * @return
     */
    private boolean if_new_cart(List<CartInfo> listCartCookie, CartInfo cartInfo) {

        Boolean b = true;

        for (CartInfo info : listCartCookie) {
            if (info.getSkuId().equals(cartInfo.getSkuId())){
                b = false;
            }
        }
        return b;
    }


    /**
     * 转发到购物车列表页面
     * @param request
     * @param map
     * @return
     */
    @LoginRequire(needSuccess = false)
    @RequestMapping("cartList")
    public String cartList(HttpServletRequest request,ModelMap map){


       List<CartInfo> cartInfos =  new ArrayList<>();

        //取购物车集合
        String userId=(String)request.getAttribute("userId");

        if (StringUtils.isBlank(userId)){
            //从cookie中取出集合
            String listCartCookie = CookieUtil.getCookieValue(request, "listCartCookie", true);
            if (StringUtils.isNotBlank(listCartCookie)){
                cartInfos = JSON.parseArray(listCartCookie, CartInfo.class);
            }

        }else{
            //redis中取
            cartInfos = cartService.getCartInfosFromCacheByUserId(userId);
        }

        BigDecimal cartPrice = getCartPrice(cartInfos);


        map.put("cartList",cartInfos);
        map.put("cartPrice",cartPrice);
        return "cartList";

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
    @LoginRequire(needSuccess = false)
    @RequestMapping("checkcart")
    public String checkcart(CartInfo cartInfo,ModelMap map,HttpServletRequest request,HttpServletResponse response){
        String skuId = cartInfo.getSkuId();
        String isChecked = cartInfo.getIsChecked();

        List<CartInfo> cartInfos = new ArrayList<>();

        String userId=(String)request.getAttribute("userId");
        //修改购物车的勾选状态
        if (StringUtils.isNotBlank(userId)){
            //修改db
            cartInfo.setUserId(userId);
            cartService.updateCartByUserId(cartInfo);
            cartInfos = cartService.getCartInfosFromCacheByUserId(userId);

        }else{
            //修改cookie
            String listCartCookie = CookieUtil.getCookieValue(request, "listCartCookie", true);
            cartInfos = JSON.parseArray(listCartCookie, CartInfo.class);

            for (CartInfo info : cartInfos) {
                if (info.getSkuId().equals(skuId)){
                    info.setIsChecked(cartInfo.getIsChecked());
                }
            }
            //覆盖浏览器
            CookieUtil.setCookie(request,response,"listCartCookie",JSON.toJSONString(cartInfos),1000*60*60*24,true);

        }

        //返回购物车列表中的最新数据
        BigDecimal cartPrice = new BigDecimal(0);
        cartPrice = getCartPrice(cartInfos);

        map.put("cartList",cartInfos);
        map.put("cartPrice",cartPrice);
        return "cartListInner";
    }

}
