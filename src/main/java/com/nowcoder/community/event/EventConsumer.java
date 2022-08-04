package com.nowcoder.community.event;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

@Component
public class EventConsumer implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private DiscussPostService discussPostService;

    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @Value("${wk.image.command}")
    private String wkImageCommand;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.share.name}")
    private String shareBucketName;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @KafkaListener(topics = {TOPIC_COMMENT,TOPIC_LIKE,TOPIC_FOLLOE})
    public void handleCommentMessage(ConsumerRecord record){
        if(record == null || record.value()==null){
            logger.error("消息的内容为空");
            return;
        }

        // 把接受到的json字符串恢复成Event对象
        // record.value()取出来的是个对象：因为编译器无法识别生产者产生的消息类型
        // 具体指定字符串对应的类型 Event.class
        Event event = JSONObject.parseObject(record.value().toString(),Event.class);
        if(event == null){
            logger.error("消息格式错误");
            return;
        }

        // 发送站内通知，主要是构造Message对象
        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());

        Map<String,Object> content = new HashMap<>();
        content.put("userId",event.getUserId());
        content.put("entityType",event.getEntityType());
        content.put("entityId",event.getEntityId());

        if(!event.getData().isEmpty()){
            for (Map.Entry<String,Object> entry : event.getData().entrySet()){
                content.put(entry.getKey(),entry.getValue());
            }
        }

        // 把content转成json字符串存到message中
        message.setContent(JSONObject.toJSONString(content));
        messageService.addMessage(message);

    }

    // 消费发帖事件
    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord record){

        if(record == null || record.value()==null){
            logger.error("消息的内容为空");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(),Event.class);
        if(event == null){
            logger.error("消息格式错误");
            return;
        }

        DiscussPost post = discussPostService.findDiscussPost(event.getEntityId());
        elasticsearchService.saveDiscussPost(post);
    }

    // 消费删贴事件
    @KafkaListener(topics = {TOPIC_DELETE})
    public void handleDeleteMessage(ConsumerRecord record){

        if(record == null || record.value()==null){
            logger.error("消息的内容为空");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(),Event.class);
        if(event == null){
            logger.error("消息格式错误");
            return;
        }

        elasticsearchService.deleteDiscussPost(event.getEntityId());
    }

    //消费分享事件
    @KafkaListener(topics = {TOPIC_SHARE})
    public  void handleShareMessage(ConsumerRecord record){
        if(record == null || record.value()==null){
            logger.error("消息的内容为空");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(),Event.class);
        if(event == null){
            logger.error("消息格式错误");
            return;
        }

        String htmlUrl = (String) event.getData().get("htmlUrl");
        String fileName = (String) event.getData().get("fileName");
        String suffix = (String) event.getData().get("suffix");

        String cmd = wkImageCommand + " --quality 75  "
                +htmlUrl +" "+wkImageStorage+"/"+ fileName + suffix;

        try {
            // 生成图片需要消耗时间  所以Runtime.getRuntime().exec(cmd);之后的命令先执行
            Runtime.getRuntime().exec(cmd);
            logger.info("生成长图成功!" + cmd);
        } catch (IOException e) {
            logger.error("生成长图失败!"+e.getMessage());
        }

        // 把图片上传到七牛云 但是可能图片还没生成
        // 人为启动一个定时器,监视该图片，一旦生成，就上传到七牛云
        //  消息发出之后 只有一个Consumer抢到  别的Consumer就不会处理了  所以可以用ThreadPoolTaskScheduler
        UploadTask task = new UploadTask(fileName,suffix);
        // 启动定时器由一个返回值 Future 封装的定时器的状态 还能停止定时器
        // 500毫秒为时间间隔
        Future future = taskScheduler.scheduleAtFixedRate(task,500);

        task.setFuture(future);
    }

    class UploadTask implements  Runnable{

        // 文件名称
        private String fileName;
        // 文件后缀
        private String suffix;
        // 启动任务的返回值
        private Future future;
        // 开始时间
        private long startTime;
        // 上传次数
        private int uploadTimes;

        public UploadTask(String fileName, String suffix) {
            this.fileName = fileName;
            this.suffix = suffix;
            this.startTime = System.currentTimeMillis();
        }

        public void setFuture(Future future) {
            this.future = future;
        }

        @Override
        public void run(){
            // 通常是生成图片失败
            if(System.currentTimeMillis() - startTime >30000){
                logger.error("执行时间过长，终止任务" + fileName);
                future.cancel(true);
                return;
            }
            // 上传失败
            if(uploadTimes >= 3){
                logger.error("上传次数过多，终止任务" + fileName);
                future.cancel(true);
                return;
            }

            String path = wkImageStorage + "/" +fileName + suffix;
            File file = new File(path);
            if(file.exists()){
                // ++uploadTimes 对uploadTimes进行了操作
                logger.info(String.format("开始第%d次上传[%s]" , ++uploadTimes , fileName));

                // 设置响应信息
                StringMap policy = new StringMap();
                policy.put("returnBody", CommunityUtil.getJSONString(0));

                // 生成凭证
                Auth auth = Auth.create(accessKey,secretKey);
                // 3600秒
                String uploadToken = auth.uploadToken(shareBucketName,fileName,3600,policy);

                // 指定上传机房
                UploadManager manager = new UploadManager(new Configuration(Region.huadong()));
                try {
                    // 开始上传图片
                    Response response = manager.put(
                            path ,fileName ,uploadToken,null,"image/" + suffix.substring(suffix.indexOf(".")+1) ,false);
                    // 处理响应结果
                    // json字符串转化成json对象
                    JSONObject json =  JSONObject.parseObject(response.bodyString());
                    if (json == null || json.get("code") == null || !json.get("code").toString().equals("0")) {
                        logger.info(String.format("第%d次上传失败[%s]",uploadTimes,fileName));
                    }else{
                        logger.info(String.format("第%d次上传成功[%s]",uploadTimes,fileName));
                        future.cancel(true);
                    }
                }catch (QiniuException e){
                   logger.info(String.format("第%d次上传失败[%s]",uploadTimes,fileName));
                }
            }else{
                logger.info("等待图片生成!" + fileName);
            }
        }
    }
}
