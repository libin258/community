package com.nowcoder.community.config;


import com.nowcoder.community.quartz.AlphaJob;
import com.nowcoder.community.quartz.PostScoreRefreshJob;
import com.nowcoder.community.quartz.WKImageDeleteJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

// 配置  第一次读取到 封装的信息被初始化到数据库中  以后访问数据库来调用
@Configuration
public class quartzConfig {

    // FactoryBean : 可简化Bean的实例化过程
    // 1.通过FactoryBean封装Bean的实例化过程
    // 2.将FactoryBean装配到Spring容器里
    // 3.将FactoryBean注入给其他的Bean
    // 4.该Bean得到的是FactoryBean所管理的对象实例

    // 配置JobDetail
    //@Bean
    public JobDetailFactoryBean alphaJobDetail(){
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();

        // 管理的Bean
        factoryBean.setJobClass(AlphaJob.class);
        // 给任务取名字
        factoryBean.setName("alphaJob");
        // 组名
        factoryBean.setGroup("alphaJobGroup");
        // 任务持久保存
        factoryBean.setDurability(true);
        // 任务可恢复
        factoryBean.setRequestsRecovery(true);
        return factoryBean;
    }

    // 配置Trigger(SimpleTriggerFactoryBean,CronTriggerFactoryBean)
    //@Bean
    public SimpleTriggerFactoryBean alphaTrigger(JobDetail alphaJobDetail){
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        // Trigger对哪个Job做的触发器
        factoryBean.setJobDetail(alphaJobDetail);
        // Trigger的名字
        factoryBean.setName("alphaTrigger");
        factoryBean.setGroup("alphaTriggerGroup");
        // 频率  多长时间执行一次任务  3000毫秒
        factoryBean.setRepeatInterval(3000);
        // 存储Job的一些状态  初始化一个默认的new JobDataMap()
        factoryBean.setJobDataMap(new JobDataMap());
        return factoryBean;
    }

    //刷新帖子分数的任务
    @Bean
    public JobDetailFactoryBean postScoreRefreshJobDetail(){
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();

        // 管理的Bean
        factoryBean.setJobClass(PostScoreRefreshJob.class);
        // 给任务取名字
        factoryBean.setName("PostScoreRefreshJob");
        // 组名
        factoryBean.setGroup("communityJobGroup");
        // 任务持久保存
        factoryBean.setDurability(true);
        // 任务可恢复
        factoryBean.setRequestsRecovery(true);
        return factoryBean;
    }


    @Bean
    public SimpleTriggerFactoryBean postScoreRefreshTrigger(JobDetail postScoreRefreshJobDetail){
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        // Trigger对哪个Job做的触发器
        factoryBean.setJobDetail(postScoreRefreshJobDetail);
        // Trigger的名字
        factoryBean.setName("postScoreRefreshTrigger");
        factoryBean.setGroup("communityTriggerGroup");
        // 频率  多长时间执行一次任务  5分钟
        factoryBean.setRepeatInterval(1000 * 60 *5);
        // 存储Job的一些状态  初始化一个默认的new JobDataMap()
        factoryBean.setJobDataMap(new JobDataMap());
        return factoryBean;
    }

    // 删除WK图片任务
    @Bean
    public JobDetailFactoryBean WkImageDeleteDetail(){
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(WKImageDeleteJob.class);
        factoryBean.setName("WkImageDeleteJob");
        factoryBean.setGroup("communityJobGroup");
        factoryBean.setDurability(true);
        factoryBean.setRequestsRecovery(true);
        return  factoryBean;
    }

    // 删除WK图片触发器
    @Bean
    public  SimpleTriggerFactoryBean WkImageDeleteTrigger(JobDetail WkImageDeleteDetail){
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(WkImageDeleteDetail);
        factoryBean.setName("WkImageDeleteTrigger");
        factoryBean.setGroup("communityTriggerGroup");
        factoryBean.setRepeatInterval(1000 * 60 * 4);
        factoryBean.setJobDataMap(new JobDataMap());
        return factoryBean;
    }
}
