package com.atguigu.gmall.list;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.service.SkuServer;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.lucene.util.QueryBuilder;
import org.elasticsearch.action.percolate.PercolateResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallListServiceApplicationTests {

    @Autowired
    JestClient jestClient;


    @Reference
    SkuServer skuServer;


    @Test
    public void contextLoads() throws IOException {
        List<SkuLsInfo> skuLsInfos = new ArrayList<>();
        //new Search.Builder(getMyDsl())这里可以写直接写dsl语句  也可以使用工具类
       Search search =  new Search.Builder(getMyDsl()).addIndex("gmall0508").addType("skuLsInfo").build();

            SearchResult execute = jestClient.execute(search);

        List<SearchResult.Hit<SkuLsInfo, Void>> hits = execute.getHits(SkuLsInfo.class);

        for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
            SkuLsInfo source = hit.source;
            skuLsInfos.add(source);
        }
    }


    /**
     * 把三级分类id为61的（手机）导入到elasticsearch中
     */
    public void addData(){

        //查询skuinfo
        List<SkuInfo> skuInfoList = skuServer.getSkuByCatalog3Id("61");

        //将skuInfo转化成skuLsInfo
        List<SkuLsInfo> skuLsInfos = new ArrayList<>();

        for (SkuInfo skuInfo : skuInfoList) {
            try {
                SkuLsInfo skuLsInfo = new SkuLsInfo();
                //使用apache的BeanUtils工具类把skuInfo对象复制到skuLsInfo对象中
                BeanUtils.copyProperties(skuLsInfo, skuInfo);
                skuLsInfos.add(skuLsInfo);

            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        //将SkuLsInfos插入到elasticSeatch中
        System.out.println(skuLsInfos.size());
        for (SkuLsInfo skuLsInfo : skuLsInfos) {
            Index build = new Index.Builder(skuLsInfo).index("gmall0508").type("skuLsInfo").id(skuLsInfo.getId()).build();

            try {
                jestClient.execute(build);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getMyDsl(){
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();


        //bool查询
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

        //过滤
        TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id","61");
        boolQueryBuilder.filter(termQueryBuilder);


        //搜索
        MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName","小米");
        BoolQueryBuilder must = boolQueryBuilder.must(matchQueryBuilder);



        //将属性参数放入查询
        searchSourceBuilder.query(boolQueryBuilder);
        searchSourceBuilder.from(0);
        searchSourceBuilder.size(100);

        return searchSourceBuilder.toString();

    }

}
