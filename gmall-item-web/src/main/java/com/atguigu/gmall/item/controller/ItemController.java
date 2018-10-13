package com.atguigu.gmall.item.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuSaleAttrValue;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.service.SkuServer;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ItemController {

    @Reference
    SkuServer skuService;

    @RequestMapping("{skuId}.html")
    public String index(@PathVariable String skuId, ModelMap map){
       SkuInfo skuInfo= skuService.getSkuById(skuId);

        if(null!=skuInfo){
            map.put("skuInfo",skuInfo);
            String spuId = skuInfo.getSpuId();

            // 查询销售属性列表
            List<SpuSaleAttr> spuSaleAttrs =  skuService.getSpuSaleAttrListCheckBySku(spuId,skuInfo.getId());
            map.put("spuSaleAttrListCheckBySku",spuSaleAttrs);

            // 查询sku的兄弟姐妹的hash表hashMap
            // {"176|180":"95","176|180":"96","176|180":"97","176|180":"98","176|180":"99"}
            Map<String,String> skuMap = new HashMap<String,String>();
            List<SkuInfo> skuInfos =  skuService.selectSkuSaleAttrValueListBySpu(spuId);
            for (SkuInfo info : skuInfos) {
                String v = info.getId();
                String k = "";
                List<SkuSaleAttrValue> skuSaleAttrValueList = info.getSkuSaleAttrValueList();
                for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                    String saleAttrValueId = skuSaleAttrValue.getSaleAttrValueId();
                    k = k + "|" +  saleAttrValueId;
                }
                skuMap.put(k,v);
            }

            // 用json工具将hashmap转化成json字符串
            String skuMapJson = JSON.toJSONString(skuMap);
            map.put("skuMapJson",skuMapJson);
            System.out.println(skuMapJson);
        }
        return "item";
    }

}
