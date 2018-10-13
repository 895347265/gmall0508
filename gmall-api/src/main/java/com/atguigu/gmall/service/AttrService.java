package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.BaseAttrInfo;

import java.util.HashSet;
import java.util.List;

public interface AttrService {
   public  List<BaseAttrInfo> getAttrInfo(String catalog3Id);

    void saveAttr(BaseAttrInfo baseAttrInfo);

    List<BaseAttrInfo> getAttrListByValueId(String join);

}


