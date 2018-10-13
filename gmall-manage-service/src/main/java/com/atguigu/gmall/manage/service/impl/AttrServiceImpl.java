package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.BaseAttrInfo;
import com.atguigu.gmall.bean.BaseAttrValue;
import com.atguigu.gmall.manage.mapper.BaseAttrInfoMapper;
import com.atguigu.gmall.manage.mapper.BaseAttrValueMapper;
import com.atguigu.gmall.service.AttrService;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import java.util.HashSet;
import java.util.List;

@Service
public class AttrServiceImpl implements AttrService {
    @Autowired
    BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    BaseAttrValueMapper baseAttrValueMapper;

    @Override
    public List<BaseAttrInfo> getAttrInfo(String catalog3Id) {

        BaseAttrInfo baseAttrInfo = new BaseAttrInfo();
        baseAttrInfo.setCatalog3Id(catalog3Id);
        List<BaseAttrInfo> baseAttrInfoList = baseAttrInfoMapper.select(baseAttrInfo);

        if (baseAttrInfoList !=null && baseAttrInfoList.size()> 0 ){
            for (BaseAttrInfo attrInfo : baseAttrInfoList) {
                String id = attrInfo.getId();
                BaseAttrValue baseAttrValue = new BaseAttrValue();
                baseAttrValue.setAttrId(id);
                List<BaseAttrValue> select = baseAttrValueMapper.select(baseAttrValue);
                attrInfo.setAttrValueList(select);
            }
        }
        return baseAttrInfoList;
    }

    @Override
    public void saveAttr(BaseAttrInfo baseAttrInfo) {

        String id = baseAttrInfo.getId();

        if (id == null){
            baseAttrInfoMapper.insertSelective(baseAttrInfo);
            String attrid = baseAttrInfo.getId();

            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            for (BaseAttrValue baseAttrValue : attrValueList) {
                baseAttrValue.setAttrId(attrid);
                baseAttrValueMapper.insert(baseAttrValue);
            }
        }else{

            String attrid = baseAttrInfo.getId();
            baseAttrInfoMapper.updateByPrimaryKeySelective(baseAttrInfo);

            Example example = new Example(BaseAttrValue.class);
            example.createCriteria().andEqualTo("attrId",attrid);



            baseAttrValueMapper.deleteByPrimaryKey(example);
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            for (BaseAttrValue baseAttrValue : attrValueList) {
                baseAttrValue.setAttrId(attrid);
                baseAttrValueMapper.insert(baseAttrValue);
            }

        }



    }

    @Override
    public List<BaseAttrInfo> getAttrListByValueId(String join) {

       List<BaseAttrInfo> baseAttrInfos =  baseAttrInfoMapper.selectAttrListByValueId(join);

        return baseAttrInfos;
    }


}
