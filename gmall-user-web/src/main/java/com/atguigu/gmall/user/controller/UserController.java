package com.atguigu.gmall.user.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UserController {

    @Reference
    UserService userService;

    @RequestMapping("/get")
    public List<UserInfo> questUserList() {
        List<UserInfo> userAll = userService.questUserAll();
        System.out.println(" 我的 额的的订单");
        return  userAll;
    }

}
