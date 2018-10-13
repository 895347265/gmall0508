package com.atguigu.gmall.manage.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.BaseCatalog1;
import com.atguigu.gmall.bean.BaseCatalog2;
import com.atguigu.gmall.bean.BaseCatalog3;
import com.atguigu.gmall.service.BaseCatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class CatalogController {

    @Reference
    BaseCatalogService baseCatalogService;

    @RequestMapping("getCatalog1")
    @ResponseBody
    public List<BaseCatalog1> getCatalog1(){

        List<BaseCatalog1> baseCatalog1s = baseCatalogService.getCatalog();

         return baseCatalog1s;
    }

    @RequestMapping("getCatalog2")
    @ResponseBody
    public List<BaseCatalog2> getCatalog2(@RequestParam("catalog1Id") String id){

        List<BaseCatalog2> baseCatalog1s = baseCatalogService.getCatalog2(id);

        return baseCatalog1s;
    }

    @RequestMapping("getCatalog3")
    @ResponseBody
    public List<BaseCatalog3> getCatalog(@RequestParam("catalog2Id") String id){

        List<BaseCatalog3> baseCatalog1s = baseCatalogService.getCatalog3(id);

        return baseCatalog1s;
    }

}
