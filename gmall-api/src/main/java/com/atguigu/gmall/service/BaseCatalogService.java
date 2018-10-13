package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.BaseCatalog1;
import com.atguigu.gmall.bean.BaseCatalog2;
import com.atguigu.gmall.bean.BaseCatalog3;

import java.util.List;

public interface BaseCatalogService {
    public List<BaseCatalog1> getCatalog();

    public  List<BaseCatalog2> getCatalog2(String id);

    public  List<BaseCatalog3> getCatalog3(String id);
}
