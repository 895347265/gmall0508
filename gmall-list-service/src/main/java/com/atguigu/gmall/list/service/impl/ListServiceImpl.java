package com.atguigu.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.BaseAttrInfo;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParam;
import com.atguigu.gmall.service.AttrService;
import com.atguigu.gmall.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.surround.query.FieldsQuery;
import org.apache.lucene.util.QueryBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Service
public class ListServiceImpl implements ListService {
    //
    @Autowired
    JestClient jestClient;



    @Override
    public List<SkuLsInfo> search(SkuLsParam skuLsParam) {
        List<SkuLsInfo> skuLsInfos = new ArrayList<>();

        //new Search.Builder(getMyDsl())这里可以写直接写dsl语句  也可以使用工具类
        Search search =  new Search.Builder(getMyDsl(skuLsParam)).addIndex("gmall0508").addType("skuLsInfo").build();


        SearchResult execute = null;
        try {
            execute = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = execute.getHits(SkuLsInfo.class);

        for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
            SkuLsInfo source = hit.source;
            Map<String, List<String>> highlight = hit.highlight;
            if (highlight != null){
                List<String> skuName = highlight.get("skuName");
                if (StringUtils.isNotBlank(skuName.get(0))){
                    source.setSkuName(skuName.get(0));
                }
            }
            skuLsInfos.add(source);
        }
        return skuLsInfos;
    }


    /**
     * 生成dsl语句的方法
     * @param skuLsParam
     * @return
     */
    public String getMyDsl(SkuLsParam  skuLsParam){

        String catalog3Id = skuLsParam.getCatalog3Id();
        String keyword = skuLsParam.getKeyword();
        String[] valueId = skuLsParam.getValueId();


        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //bool查询
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

        if (StringUtils.isNotBlank(catalog3Id)){
            //过滤
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id",catalog3Id);
            boolQueryBuilder.filter(termQueryBuilder);
        }
        if (null!=valueId && valueId.length>0){
            //加载分类属性的条件
            for (int i = 0; i < valueId.length; i++) {
                //过滤
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId",valueId[i]);
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }


        if (StringUtils.isNotBlank(keyword)){
            //搜索
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName",keyword);
            BoolQueryBuilder must = boolQueryBuilder.must(matchQueryBuilder);
        }

        //设置高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<span style='color:red;font-weigh:bolder'>");
        highlightBuilder.field("skuName");
        highlightBuilder.postTags("</span>");
        searchSourceBuilder.highlight(highlightBuilder);

        //设置聚合
        TermsBuilder groupby_attr = AggregationBuilders.terms("aggs").field("skuAttrValueList.valueId");
        searchSourceBuilder.aggregation(groupby_attr);



        //将属性参数放入查询
        searchSourceBuilder.query(boolQueryBuilder);
        searchSourceBuilder.from(0);
        searchSourceBuilder.size(100);
        System.out.println(searchSourceBuilder.toString());
        return searchSourceBuilder.toString();

    }



}
