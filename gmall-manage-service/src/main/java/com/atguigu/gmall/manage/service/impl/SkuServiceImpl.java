package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.SkuServer;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;


import java.util.List;

@Service
public class SkuServiceImpl implements SkuServer {

    @Autowired
    SkuInfoMapper skuInfoMapper;

    @Autowired
    SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    SkuImageMapper skuImageMapper;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public List<SkuInfo> skuList(String catalog3Id) {
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setCatalog3Id(catalog3Id);
        List<SkuInfo> skuInfoList = skuInfoMapper.select(skuInfo);

        return skuInfoList;
    }

    @Override
    public List<SkuInfo> skuInfoListBySpu(String spuId) {

        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setSpuId(spuId);

        List<SkuInfo> skuInfoList = skuInfoMapper.select(skuInfo);

        return skuInfoList;
    }

    @Override
    public void saveSku(SkuInfo skuInfo) {
        //保存skuInfo
         skuInfoMapper.insertSelective(skuInfo);
         String skuId = skuInfo.getId();

        //保存平台属性关联属性
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        for (SkuAttrValue skuAttrValue : skuAttrValueList) {
            skuAttrValue.setSkuId(skuId);
            skuAttrValueMapper.insertSelective(skuAttrValue);
        }

        //保存销售属性关联属性
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                skuSaleAttrValue.setSkuId(skuId);
                skuSaleAttrValueMapper.insertSelective(skuSaleAttrValue);
        }

        //保存图片信息
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        for (SkuImage skuImage : skuImageList) {
            skuImage.setSkuId(skuId);
            skuImageMapper.insertSelective(skuImage);
        }

    }

    public SkuInfo getSkuByIdFromDB(String skuId){

        //查询sku信息
        SkuInfo skuInfoParam = new SkuInfo();
        skuInfoParam.setId(skuId);
        SkuInfo skuInfo = skuInfoMapper.selectOne(skuInfoParam);

        //查询图片信息
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuId);
        List<SkuImage> skuImages = skuImageMapper.select(skuImage);
        skuInfo.setSkuImageList(skuImages);

        return skuInfo;
    }


    @Override
    public SkuInfo getSkuById(String skuId) {
        Thread thread = new Thread();
        String name = thread.getName();
        System.out.println(name);
        SkuInfo skuInfo = null;
        String skuKey="sku:"+skuId+":info";
        //缓存redis查询
        Jedis jedis = redisUtil.getJedis();
        String s = jedis.get(skuKey);

        if (StringUtils.isNotBlank(s)&&s.equals("empty")){
            System.out.println("发现数据库没有数据，返回");
            return null;
        }

        if (StringUtils.isNotBlank(s)){
            System.out.println("从redis中获取数据");
             skuInfo = JSON.parseObject(s, SkuInfo.class);
        }else {
            //redis分布式缓存锁
            System.out.println("获取分布式锁");
            String set = jedis.set("sku:" + skuId + ":lock", "1", "nx", "px", 10000);
            //db查询
            if (StringUtils.isNotBlank(set)){
                System.out.println("线程得到分布式锁，开始访问数据库");
                 skuInfo = getSkuByIdFromDB(skuId);
                 if (null!= skuInfo){
                     System.out.println("线程成功访问到数据库，删除分布式锁！");
                     jedis.del("sku:" + skuId + ":lock");
                 }
            }else {
                System.out.println("线程需要访问数据库，但是未得到分布式锁，开始自旋！");
                try {
                    //睡眠3秒
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //自旋
                return getSkuById(skuId);
            }
            //同步到redis
            if (skuInfo != null ||"empty".equals(s) ){
                System.out.println("把数据同步到redis中");
                jedis.set(skuKey,JSON.toJSONString(skuInfo));
            }
        }

        //关闭jedis连接
        jedis.close();

        return skuInfo;
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(String spuId,String SkuId) {

        List<SpuSaleAttr> spuSaleAttrs = skuSaleAttrValueMapper.selectSpuSaleAttrListCheckBySku(Integer.parseInt(spuId),Integer.parseInt(SkuId));
        return spuSaleAttrs;
    }

    @Override
    public List<SkuInfo> selectSkuSaleAttrValueListBySpu(String spuId) {

        List<SkuInfo> skuInfoList = skuSaleAttrValueMapper.selectSkuSaleAttrValueListBySpu(Integer.parseInt(spuId));


        return skuInfoList;
    }

    @Override
    public List<SkuInfo> getSkuByCatalog3Id(String catalog3Id) {
        //查询skuinfo
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setCatalog3Id(catalog3Id);

        List<SkuInfo> SkuInfos = skuInfoMapper.select(skuInfo);

        for (SkuInfo info : SkuInfos) {
            String infoId = info.getId();
            //查询图片集合
            SkuImage skuImage = new SkuImage();
            skuImage.setSkuId(infoId);
            List<SkuImage> skuImages = skuImageMapper.select(skuImage);
            info.setSkuImageList(skuImages);

            //查询平台属性集合

            SkuAttrValue skuAttrValue = new SkuAttrValue();
            skuAttrValue.setSkuId(infoId);
            List<SkuAttrValue> skuAttrValues = skuAttrValueMapper.select(skuAttrValue);
            info.setSkuAttrValueList(skuAttrValues);

        }
        return SkuInfos;
    }

}
