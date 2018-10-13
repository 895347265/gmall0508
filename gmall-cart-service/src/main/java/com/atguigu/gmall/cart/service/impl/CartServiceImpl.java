package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class CartServiceImpl implements CartService {

    @Autowired
    CartInfoMapper cartInfoMapper;

    @Autowired
    RedisUtil redisUtil;


    /**
     * 根据userId和skuId查询购物车
     * @param cartInfo
     * @return
     */
    @Override
    public CartInfo ifCartExits(CartInfo cartInfo) {

        Example example = new Example(CartInfo.class);
        Example.Criteria criteria = example.createCriteria();

        criteria.andEqualTo("userId",cartInfo.getUserId()).andEqualTo("skuId",cartInfo.getSkuId());

        CartInfo cartInfoReturn = cartInfoMapper.selectOneByExample(example);


        return cartInfoReturn;
    }


    /**
     * 修改购物车数据
     * @param cartInfoDb
     */
    @Override
    public void updateCart(CartInfo cartInfoDb) {
        cartInfoMapper.updateByPrimaryKeySelective(cartInfoDb);
        //同步到redis中
        flushCartCacheByUserId(cartInfoDb.getUserId());
    }

    /**
     * 新增购物车数据
     * @param cartInfo
     */
    @Override
    public void insertCart(CartInfo cartInfo) {

        cartInfoMapper.insertSelective(cartInfo);
        //同步到redis中
        flushCartCacheByUserId(cartInfo.getUserId());
    }


    /**
     * 同步到redis中
     * @param userId
     */
    @Override
    public void flushCartCacheByUserId(String userId) {
        //查询userId对应的购物车集合
        CartInfo cartInfo = new CartInfo();
        cartInfo.setUserId(userId);
        List<CartInfo> cartInfos = cartInfoMapper.select(cartInfo);

        if(cartInfos != null && cartInfos.size()>0){

            //将购物车集合转化为map
            Map<String,String> map = new HashMap<>();
            for (CartInfo info : cartInfos) {
                map.put(info.getId(), JSON.toJSONString(info));
            }
            Jedis jedis = redisUtil.getJedis();
            jedis.del("cart:"+userId+":list");
            //将购物车的集合放入redis中（redis中的hash数据结构）
            jedis.hmset("cart:"+userId+":list",map);
            jedis.close();
        }else{
            //清理redis
            Jedis jedis = redisUtil.getJedis();
            // 将购物车的hashMap放入redis
            Map<String,String> map = new HashMap<>();
            jedis.del("cart:" + userId + ":list");
            jedis.close();
        }

    }

    /**
     * 从redis中取出数据
     * @param userId
     * @return
     */
    @Override
    public List<CartInfo> getCartInfosFromCacheByUserId(String userId) {

        List<CartInfo> cartInfos =  new ArrayList<>();

        Jedis jedis = redisUtil.getJedis();
        List<String> hvals = jedis.hvals("cart:" + userId + ":list");
        if (hvals!=null && hvals.size()>0){
            for (String hval : hvals) {
                CartInfo cartInfo = JSON.parseObject(hval, CartInfo.class);
                cartInfos.add(cartInfo);
            }
        }

        jedis.close();

        return cartInfos;
    }


    /**
     * 修改购物车勾选状态
     * @param cartInfo
     */
    @Override
    public void updateCartByUserId(CartInfo cartInfo) {

        Example example = new Example(CartInfo.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("userId",cartInfo.getUserId()).andEqualTo("skuId",cartInfo.getSkuId());
        cartInfoMapper.updateByExampleSelective(cartInfo,example);

        //同步到redis中
        flushCartCacheByUserId(cartInfo.getUserId());

    }


    /**
     * 从cookie中转存到db中 （合并购物车）
     * @param userId
     * @param listCartCookie
     */
    @Override
    public void combine(String userId, List<CartInfo> listCartCookie) {

        CartInfo cartInfo = new CartInfo();
        cartInfo.setUserId(userId);

        List<CartInfo> listCartDb = cartInfoMapper.select(cartInfo);
        if(listCartCookie!=null&&listCartCookie.size()>0){
            for (CartInfo cartCookie : listCartCookie) {
                String SkuIdCookie = cartCookie.getSkuId();
                boolean b = if_new_cart(listCartDb, cartCookie);
                if(!b){
                    //更新
                    CartInfo cartDb = new CartInfo();
                    for (CartInfo info : listCartDb) {
                        if (info.getSkuId().equals(cartCookie.getSkuId())){
                            cartDb=info;

                        }
                    }
                    cartDb.setSkuNum(cartCookie.getSkuNum());
                    cartDb.setIsChecked(cartCookie.getIsChecked());
                    cartDb.setCartPrice(cartDb.getSkuPrice().multiply(new BigDecimal(cartDb.getSkuNum())));
                    cartInfoMapper.updateByPrimaryKeySelective(cartDb);
                }else{
                    //添加
                    cartCookie.setUserId(userId);
                    cartInfoMapper.insertSelective(cartCookie);
                }
        }

        }

        //同步刷新缓存到redis中
        flushCartCacheByUserId(userId);


    }

    /**
     * 删除购物车数据
     * @param join
     */
    @Override
    public void deteleCart(String join,String userId) {
        //删除额购物车已经下单的数据
        cartInfoMapper.deleteCartsById(join);

        //同步购物车缓存
        flushCartCacheByUserId(userId);
    }

    private boolean if_new_cart(List<CartInfo> listCartDb, CartInfo cartInfo) {

        Boolean b = true;

        for (CartInfo info : listCartDb) {
            if (info.getSkuId().equals(cartInfo.getSkuId())){
                b = false;
            }
        }
        return b;
    }

}
