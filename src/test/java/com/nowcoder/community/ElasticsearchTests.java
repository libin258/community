package com.nowcoder.community;

import com.nowcoder.community.dao.DiscussPostMapper;
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class ElasticsearchTests {

    @Autowired
    private DiscussPostMapper discussMapper;

    @Autowired
    private DiscussPostRepository discussRepository;

    @Autowired
    private ElasticsearchTemplate elasticTemplate;

    //  添加一个
    @Test
    public void testInsert() {
        discussRepository.save(discussMapper.selectDiscussPostById(248));
        discussRepository.save(discussMapper.selectDiscussPostById(249));
        discussRepository.save(discussMapper.selectDiscussPostById(234));

    }

    // 添加多个
    @Test
    public void testInsertList(){
        discussRepository.saveAll(discussMapper.selectDiscussPosts(101,0,100,0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(102,0,100,0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(103,0,100,0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(111,0,100,0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(112,0,100,0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(131,0,100,0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(132,0,100,0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(133,0,100,0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(134,0,100,0));

    }

    // 修改数据
    @Test
    public void testUpdate(){
        DiscussPost post = discussMapper.selectDiscussPostById(231);
        post.setContent("我是新人，灌水");
        discussRepository.save(post);
    }

    @Test
    public void testDelete(){
        // 根据id删除一条数据
//        discussRepository.deleteById(231);
         // 删掉所有数据
        discussRepository.deleteAll();
    }

    // 搜索
    @Test
    public void testSearchByRepository(){

        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                // 构造搜索条件  QueryBuilders.multiMatchQuery :  在title和content中搜索
                .withQuery(QueryBuilders.multiMatchQuery("offer","title","content"))
                // 排序条件  先按type倒叙排（置顶的排前面），都置顶就按score排，分数一样就按时间来排
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                // 分页查询 从0页开始 一个10条数据
                .withPageable(PageRequest.of(0,10))
                // 那些词高亮显示  前置标签<em> 后置标签</em>
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build(); // build()方法执行返回searchQuery接口实现类

        // 进行分页查询
        // elasticTemplate.queryForPage(searchQuery, class , SearchResultMapper)：底层调用的方法
        // 底层获取得到了高亮显示的值，但是没有返回
        Page<DiscussPost> page =  discussRepository.search(searchQuery);
        System.out.println(page.getTotalElements()); // 多少数据
        System.out.println(page.getTotalPages());      // 多少页
        System.out.println(page.getNumber());  // 当前为第几页
        System.out.println(page.getSize()); // 每页最多显示多少数据
        for (DiscussPost post : page){
            System.out.println(post);
        }
    }

    @Test
    public void testSearchByTemplate(){
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                // 构造搜索条件  QueryBuilders.multiMatchQuery :  在title和content中搜索
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬","title","content"))
                // 排序条件  先按type倒叙排（置顶的排前面），都置顶就按score排，分数一样就按时间来排
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                // 分页查询 从0页开始 一个10条数据
                .withPageable(PageRequest.of(0,10))
                // 那些词高亮显示  前置标签<em> 后置标签</em>
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build(); // build()方法执行返回searchQuery接口实现类

        // 得到的结果会交给SearchResultMapper处理
        Page<DiscussPost> page = elasticTemplate.queryForPage(searchQuery, DiscussPost.class , new SearchResultMapper() {
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
                // 通过response获取这次命中的数据
                SearchHits hits = response.getHits();
                if (hits.getTotalHits() <= 0) {
                    return null;
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
                    post.setCreateTime(new Date(Long.valueOf(createTime)));

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

        System.out.println(page.getTotalElements()); // 多少数据
        System.out.println(page.getTotalPages());      // 多少页
        System.out.println(page.getNumber());  // 当前为第几页
        System.out.println(page.getSize()); // 每页最多显示多少数据
        for (DiscussPost post : page){
            System.out.println(post);
        }
    }

}
