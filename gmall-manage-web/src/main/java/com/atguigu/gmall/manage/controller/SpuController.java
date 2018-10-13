package com.atguigu.gmall.manage.controller;


import ch.qos.logback.core.net.SyslogOutputStream;
import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;

import com.atguigu.gmall.manage.util.FileUploadUtil;


import com.atguigu.gmall.service.SkuServer;
import com.atguigu.gmall.service.SpuService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.awt.font.MultipleMaster;
import java.util.List;

@Controller
public class SpuController {
    @Reference
    SpuService spuService;



    @RequestMapping("baseSaleAttrList")
    @ResponseBody
    public List<BaseSaleAttr> baseSaleAttrList(){

        List<BaseSaleAttr> saleAttrList = spuService.baseSaleAttrList();
        return saleAttrList;
    }

    @RequestMapping("saveSpu")
    @ResponseBody
    public String saveSpu(SpuInfo spuInfo){
        spuService.saveSpu(spuInfo);

        return "succeec";
    }

    @RequestMapping("fileUpload")
    @ResponseBody
    public String fileUpload(@RequestParam("file") MultipartFile file){
        String imgUrl =  FileUploadUtil.uploadImage(file);
        return imgUrl;
    }


    @RequestMapping("spuList")
    @ResponseBody
    public List<SpuInfo> spuList(@RequestParam("catalog3Id")String catalog3Id){

        List<SpuInfo>list =  spuService.spuList(catalog3Id);
        System.out.println("dfdsafdsafsa");
        return list;
    }


    @RequestMapping("spuSaleAttrList")
    @ResponseBody
    public List<SpuSaleAttr> spuSaleAttrList(@RequestParam("spuId")String spuId){

       List<SpuSaleAttr> baseSaleAttrs =   spuService.spuSaleAttrlist(spuId);
        return baseSaleAttrs;
    }

    @RequestMapping("spuImageList")
    @ResponseBody
    public List<SpuImage> spuImageList(@RequestParam("spuId")String spuId){
        System.out.print("spuImageList");
        List<SpuImage> baseSaleAttrs = spuService.spuImageList(spuId);
        return baseSaleAttrs;
    }


}
