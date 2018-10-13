package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.CartInfo;

import java.util.List;

public interface CartService {
    CartInfo ifCartExits(CartInfo cartInfo);

    void updateCart(CartInfo cartInfoDb);

    void insertCart(CartInfo cartInfo);

    void flushCartCacheByUserId(String userId);

    List<CartInfo> getCartInfosFromCacheByUserId(String userId);

    void updateCartByUserId(CartInfo cartInfo);

    void combine(String userId, List<CartInfo> cartInfos);

    void deteleCart(String join,String userId);
}


