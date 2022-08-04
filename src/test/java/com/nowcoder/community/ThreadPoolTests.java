package com.nowcoder.community;


import com.nowcoder.community.service.AlphaService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class ThreadPoolTests {

    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolTests.class);

    // JDK普通的线程池 通过Executors这个工厂来实例化   newFixedThreadPool(5)：里面包含5个线程 反复使用这5个线程
    private ExecutorService executorService = Executors.newFixedThreadPool(5);

    // JDK可执行定时任务的线程池
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);

    // spring普通线程池
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    // spring可执行定时任务的线程池 想让ThreadPoolTaskScheduler生效需要写个配置类
    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Autowired
    private AlphaService alphaService;


    // Test方法单纯执行线程（后面没有其它逻辑）会立马结束    想等它执行完再结束 可以sleep一会
    // m 毫秒
    private  void sleep(long m){
        try {
            Thread.sleep(m);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    // 1.JDK普通线程池
    @Test
    public  void testExecutorService(){
        // 实现一个接口执行任务给它一个线程体  任务就是一个线程体
        Runnable task = new Runnable() {
            @Override
            public void run() {
                // 执行任务的逻辑
                logger.debug("hello ExecutorService");
            }
        };

        // 用线程池来执行 每次分配一个线程
        for (int i = 0; i < 10 ; i++) {
            // 调用submit方法 线程池就会分配一个线程 来执行线程体
            executorService.submit(task);
        }

        sleep(10000);

    }

    // 2. JDK定时任务线程池
    @Test
    public void  testScheduledExecutorService(){

        Runnable task = new Runnable() {
            @Override
            public void run() {
                // 执行任务的逻辑
                logger.debug("hello ScheduledExecutorService");
            }
        };

        // 任务 ， 延迟的时间 ，时间间隔（可以反复执行）, 设置时间的单位  TimeUnit.MILLISECONDS:毫秒
        scheduledExecutorService.scheduleAtFixedRate(task,10000, 1000, TimeUnit.MILLISECONDS);

        sleep(30000);
    }


    //3. spring普通线程池
    @Test
    public  void testThreadPoolTaskExecutor(){
        Runnable task = new Runnable() {
            @Override
            public void run() {
                // 执行任务的逻辑
                logger.debug("hello ThreadPoolTaskExecutor");
            }
        };

        for (int i = 0; i < 10; i++) {
            taskExecutor.submit(task);
        }

        sleep(10000);
    }

    //4. spring定时任务线程池
    @Test
    public void testThreadPoolTaskScheduler(){
        Runnable task = new Runnable() {
            @Override
            public void run() {
                // 执行任务的逻辑
                logger.debug("hello ThreadPoolTaskScheduler");
            }
        };
        // 当前时间 + 10000毫秒  当前时间延迟10000毫秒
        Date startTime = new Date(System.currentTimeMillis() + 10000);

        // 任务 ， 一个具体的时间 ，时间间隔 ， 默认为毫秒不用指定时间单位
        taskScheduler.scheduleAtFixedRate(task, startTime ,1000);

        sleep(30000);
    }

    // 5. spring普通线程池（简化）
    @Test
    public void testThreadPoolTaskExecutorSimple(){
        for (int i = 0; i < 10; i++) {
            alphaService.execute1();
        }

        sleep(10000);
    }

    // 6. spring定时任务线程池(简化) 自动调用execute2()方法
    @Test
    public void testThreadPoolTaskSchedulerSimple(){
        sleep(30000);
    }
}
