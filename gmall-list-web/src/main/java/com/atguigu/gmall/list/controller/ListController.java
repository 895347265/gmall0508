package com.atguigu.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.AttrService;
import com.atguigu.gmall.service.ListService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {

    @Reference
    ListService listService;


    @Reference
    AttrService attrService;

    @RequestMapping("list.html")
    public String list(SkuLsParam skuLsParam, ModelMap map){
        List<SkuLsInfo> skuLsInfoList= listService.search(skuLsParam);

        // 去重复取出所有平台属性值
        HashSet<String> strings = new HashSet<>();
        for (SkuLsInfo skuLsInfo : skuLsInfoList) {
            List<SkuLsAttrValue> skuAttrValueList = skuLsInfo.getSkuAttrValueList();

            for (SkuLsAttrValue skuLsAttrValue : skuAttrValueList) {
                String valueId = skuLsAttrValue.getValueId();
                if(valueId!=null){
                    strings.add(valueId);
                }

            }
        }

        String join = StringUtils.join(strings, ",");
        List<BaseAttrInfo> baseAttrInfos = attrService.getAttrListByValueId(join);

        // 删除当前请求中所包含的属性
        // 制作当前请求的面包屑
        List<Crumb> crumbs = new ArrayList<>();
        String[] valueId = skuLsParam.getValueId();

        if(null!=valueId&&valueId.length>0){
            for (String sid : valueId) {
                // 制作面包屑
                Crumb crumb = new Crumb();

                // 排除属性
                Iterator<BaseAttrInfo> iterator = baseAttrInfos.iterator();
                while(iterator.hasNext()){
                    BaseAttrInfo baseAttrInfo = iterator.next();

                    List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();

                    // 删除当前的valueId所关联的属性对象
                    for (BaseAttrValue baseAttrValue : attrValueList) {
                        String id = baseAttrValue.getId();
                        if(id.equals(sid)){
                            // 设置面包屑名称
                            crumb.setValueName(baseAttrValue.getValueName());
                            // 删除排除属性
                            iterator.remove();
                        }
                    }
                }
                // 制作面包屑
                String crumbsUrlParam = getCrumbsUrlParam(skuLsParam,sid);
                crumb.setUrlParam(crumbsUrlParam);
                crumbs.add(crumb);
            }
        }



        map.put("skuLsInfoList",skuLsInfoList);
        map.put("attrList",baseAttrInfos);
        // 拼接当前请求
        String urlParam = getUrlParam(skuLsParam);
        map.put("urlParam",urlParam);
        map.put("attrValueSelectedList",crumbs);
        return "list";
    }

    /***
     * 制作面包屑的url
     * @param skuLsParam
     * @param sid
     * @return
     */
    private String getCrumbsUrlParam(SkuLsParam skuLsParam, String sid) {


        String[] valueId = skuLsParam.getValueId();
        String catalog3Id = skuLsParam.getCatalog3Id();
        String keyword = skuLsParam.getKeyword();

        String urlParam = "";

        if(StringUtils.isNotBlank(catalog3Id)){
            if(StringUtils.isNotBlank(urlParam)){
                urlParam = urlParam +"&" ;
            }
            urlParam = urlParam  + "catalog3Id="+catalog3Id;
        }

        if(StringUtils.isNotBlank(keyword)){
            if(StringUtils.isNotBlank(urlParam)){
                urlParam = urlParam +"&" ;
            }
            urlParam = urlParam + "keyword="+keyword;
        }

        if(null!=valueId&&valueId.length>0){
            for (String id : valueId) {
                if(!id.equals(sid)){
                    urlParam = urlParam +"&" + "valueId="+id;
                }
            }
        }


        return urlParam;

    }

    /***
     * 地址栏中的请求url
     * @param skuLsParam
     * @return
     */
    private String getUrlParam(SkuLsParam skuLsParam) {

        String[] valueId = skuLsParam.getValueId();
        String catalog3Id = skuLsParam.getCatalog3Id();
        String keyword = skuLsParam.getKeyword();

        String urlParam = "";

        if(StringUtils.isNotBlank(catalog3Id)){
            if(StringUtils.isNotBlank(urlParam)){
                urlParam = urlParam +"&" ;
            }
            urlParam = urlParam  + "catalog3Id="+catalog3Id;
        }

        if(StringUtils.isNotBlank(keyword)){
            if(StringUtils.isNotBlank(urlParam)){
                urlParam = urlParam +"&" ;
            }
            urlParam = urlParam + "keyword="+keyword;
        }

        if(null!=valueId&&valueId.length>0){
            for (String id : valueId) {
                urlParam = urlParam +"&" + "valueId="+id;
            }
        }


        return urlParam;
    }
}
