package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.BaseCatalog1;
import com.atguigu.gmall.bean.BaseCatalog2;
import com.atguigu.gmall.bean.BaseCatalog3;
import com.atguigu.gmall.manage.mapper.BaseCatalog1Mapper;
import com.atguigu.gmall.manage.mapper.BaseCatalog2Mapper;
import com.atguigu.gmall.manage.mapper.BaseCatalog3Mapper;
import com.atguigu.gmall.service.BaseCatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Service
public class BaseCatalogServiceImpl implements BaseCatalogService {
    @Autowired
    BaseCatalog1Mapper baseCatalog1Mapper;

    @Autowired
    BaseCatalog2Mapper baseCatalog2Mapper;

    @Autowired
    BaseCatalog3Mapper baseCatalog3Mapper;


    public List<BaseCatalog1> getCatalog(){

        return baseCatalog1Mapper.selectAll();

    }

    @Override
    public List<BaseCatalog2> getCatalog2(String id) {

        BaseCatalog2 baseCatalog2 = new BaseCatalog2();
        baseCatalog2.setCatalog1Id(id);

        List<BaseCatalog2> list = baseCatalog2Mapper.select(baseCatalog2);

        return list;
    }

    @Override
    public List<BaseCatalog3> getCatalog3(String id) {
        BaseCatalog3 baseCatalog3 = new BaseCatalog3();

        baseCatalog3.setCatalog2Id(id);

        List<BaseCatalog3> catalog3List = baseCatalog3Mapper.select(baseCatalog3);

        return catalog3List;
    }
}
