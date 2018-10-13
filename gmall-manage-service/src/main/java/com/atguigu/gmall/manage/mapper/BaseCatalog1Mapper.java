package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.BaseCatalog1;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BaseCatalog1Mapper extends Mapper<BaseCatalog1> {
    public List<BaseCatalog1> getCatalog();
}
