package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SpuSaleAttr;

import java.util.List;

public interface SkuServer {
    List<SkuInfo> skuList(String catalog3Id);

    List<SkuInfo> skuInfoListBySpu(String spuId);

    void saveSku(SkuInfo skuInfo);

    SkuInfo getSkuById(String skuId);

    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(String spuId,String skuId);

    List<SkuInfo> selectSkuSaleAttrValueListBySpu(String spuId);

    List<SkuInfo> getSkuByCatalog3Id(String catalog3Id);
}
