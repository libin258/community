package com.nowcoder.community.service;

import com.nowcoder.community.dao.elasticsearch.DiscussPostRepository;
import com.nowcoder.community.entity.DiscussPost;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ElasticsearchService {

    @Autowired
    private DiscussPostRepository discussRepository;

    @Autowired
    private ElasticsearchTemplate elasticTemplate;

    // 添加
    public void saveDiscussPost(DiscussPost post){
        discussRepository.save(post);
    }

    // 删除
    public void deleteDiscussPost(int id){
        discussRepository.deleteById(id);
    }

    // 搜索   current（当前的页面）:查询时根据第几页查询
    public Page<DiscussPost> searchDiscussPost(String keyword , int current , int limit){

        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                // 构造搜索条件  QueryBuilders.multiMatchQuery :  在title和content中搜索
                .withQuery(QueryBuilders.multiMatchQuery(keyword,"title","content"))
                // 排序条件  先按type倒叙排（置顶的排前面），都置顶就按score排，分数一样就按时间来排
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                // 分页查询 从0页开始 一个10条数据
                .withPageable(PageRequest.of(current,limit))
                // 那些词高亮显示  前置标签<em> 后置标签</em>
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build(); // build()方法执行返回searchQuery接口实现类

        // 得到的结果会交给SearchResultMapper处理
//        Page<DiscussPost> page =
            return elasticTemplate.queryForPage(searchQuery, DiscussPost.class, new SearchResultMapper() {
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
                // 通过response获取这次命中的数据
                SearchHits hits = response.getHits();
                if (hits.getTotalHits() < 0){
                    return  null;
                }

                List<DiscussPost> list = new ArrayList<>();
                for(SearchHit hit : hits){
                    DiscussPost post = new DiscussPost();
                    // 调用map
                    String id = hit.getSourceAsMap().get("id").toString();
                    post.setId(Integer.valueOf(id));

                    String userId = hit.getSourceAsMap().get("userId").toString();
                    post.setUserId(Integer.valueOf(userId));

                    String title = hit.getSourceAsMap().get("title").toString();
                    post.setTitle(title);

                    String content = hit.getSourceAsMap().get("content").toString();
                    post.setContent(content);

                    String status = hit.getSourceAsMap().get("status").toString();
                    post.setStatus(Integer.valueOf(status));

                    String createTime = hit.getSourceAsMap().get("createTime").toString();
                    post.setCreateTime(new Date(Long.valueOf(createTime))); // es存日期是转成Long来存的

                    String commentCount = hit.getSourceAsMap().get("commentCount").toString();
                    post.setCommentCount(Integer.valueOf(commentCount));

//                    String score = hit.getSourceAsMap().get("score").toString();
//                    post.setScore(Double.valueOf(score));

                    // 处理高亮显示的结果
                    //  获取与title有关的的高亮显示内容
                    HighlightField titleField = hit.getHighlightFields().get("title");
                    if(titleField != null){
                        // getFragments()[0]返回的是一个数组 只要第一段覆盖
                        post.setTitle(titleField.getFragments()[0].toString());
                    }

                    HighlightField contentField = hit.getHighlightFields().get("content");
                    if(contentField != null){
                        // getFragments()[0]返回的是一个数组 只要第一段覆盖
                        post.setContent(contentField.getFragments()[0].toString());
                    }
                    list.add(post);
                }
                return new AggregatedPageImpl(list,pageable
                        ,hits.getTotalHits(),response.getAggregations(),
                        response.getScrollId(),hits.getMaxScore());
            }
        });
    }

}
