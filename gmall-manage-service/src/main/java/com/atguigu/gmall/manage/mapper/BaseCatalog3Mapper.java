package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.BaseCatalog2;
import com.atguigu.gmall.bean.BaseCatalog3;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BaseCatalog3Mapper extends Mapper<BaseCatalog3> {
    public List<BaseCatalog2> getCatalog();
}
