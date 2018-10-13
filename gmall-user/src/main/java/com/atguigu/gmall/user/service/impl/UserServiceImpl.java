package com.atguigu.gmall.user.service.impl;



import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.user.mapper.UserAddressMapper;
import com.atguigu.gmall.user.mapper.UserInfoMapper;
import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Client;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.List;
@Service
public class UserServiceImpl  implements UserService {

    @Autowired
    UserInfoMapper userInfoMapper;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    UserAddressMapper userAddressMapper;

    @Override
    public List<UserInfo> questUserAll() {

        List<UserInfo> list = userInfoMapper.selectAll();
        return list;
    }


    /**
     * 验证用户名密码
     * @param userInfo
     * @return
     */
    @Override
    public UserInfo login(UserInfo userInfo) {
        //判断用户名和密码是否正确
        UserInfo user = userInfoMapper.selectOne(userInfo);

        //同步到redis中
        if(user!=null){
            Jedis jedis = redisUtil.getJedis();
            jedis.setex("user:"+user.getId()+":info",60*1000*60*24,JSON.toJSONString(user));
            jedis.close();
        }
        return user;
    }

    @Override
    public List<UserAddress> getAddressListByUserId(String userId) {

        UserAddress userAddress = new UserAddress();
        userAddress.setUserId(userId);
        List<UserAddress> userAddressList = userAddressMapper.select(userAddress);


        return userAddressList;
    }



    @Override
    public UserAddress getAddressListById(String addressId) {
        UserAddress userAddress = new UserAddress();
        userAddress.setId(addressId);
        UserAddress userAddressl = userAddressMapper.selectOne(userAddress);
        return userAddressl;
    }
}
