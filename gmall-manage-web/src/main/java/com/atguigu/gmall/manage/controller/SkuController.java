package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.service.SkuServer;

import com.atguigu.gmall.service.SkuServer;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class SkuController {

    @Reference
    SkuServer skuServer;

    @RequestMapping("skuInfoListBySpu")
    @ResponseBody
    public List<SkuInfo> skuInfoListBySpu(@RequestParam("spuId") String spuId){

        List<SkuInfo> skuInfoList = skuServer.skuInfoListBySpu(spuId);

        return  skuInfoList;
    }


    @RequestMapping("saveSku")
    @ResponseBody
    public String saveSku(SkuInfo skuInfo){


        skuServer.saveSku(skuInfo);

        return  "succees";
    }



}
