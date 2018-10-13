package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.BaseCatalog1;
import com.atguigu.gmall.bean.BaseCatalog2;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BaseCatalog2Mapper extends Mapper<BaseCatalog2> {
    public List<BaseCatalog2> getCatalog();
}
