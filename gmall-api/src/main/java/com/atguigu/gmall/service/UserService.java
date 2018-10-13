package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;

import java.util.List;


public interface UserService {

    public List<UserInfo> questUserAll();


    UserInfo login(UserInfo userInfo);

    List<UserAddress> getAddressListByUserId(String userId);

    UserAddress getAddressListById(String addressId);
}

