package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.BaseAttrInfo;
import com.atguigu.gmall.bean.BaseAttrValue;
import com.atguigu.gmall.service.AttrService;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class AttrController {

    @Reference
    AttrService attrService;


    @RequestMapping("/getAttrListByCtg3")
    @ResponseBody
    public List<BaseAttrInfo> getAttrListByCtg3(@RequestParam Map<String,String> map){
        String catalog3Id = map.get("catalog3Id");
        List<BaseAttrInfo> list =  attrService.getAttrInfo(catalog3Id);
        return list;
    }

    @RequestMapping("/saveAttr")
    @ResponseBody
    public String saveAttr(BaseAttrInfo baseAttrInfo){

        attrService.saveAttr(baseAttrInfo);

        return "succeed";
    }

}
