package com.nowcoder.community.service;

import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class DataService {

    @Autowired
    private RedisTemplate redisTemplate;

    // 实例化一个简单的日期
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

    // 将指定的IP计入UV
    public  void recordUV(String ip){
        String redisKey = RedisKeyUtil.getUVKey(dateFormat.format(new Date()));
        redisTemplate.opsForHyperLogLog().add(redisKey,ip);

    }

    // 统计指定日期范围内的UV
    public long calculateUV(Date start , Date end){
        if(start == null || end == null){
            throw new IllegalArgumentException("参数不能为空");
        }
        // 整理该日期范围内的key
        List<String> keyList = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        // 设置日期为起始日期
        calendar.setTime(start);

        // 开始日期不晚于结束的日期的就循环
        while (!calendar.getTime().after(end)){
            String key = RedisKeyUtil.getUVKey(dateFormat.format(calendar.getTime()));

            // 把key加到集合
            keyList.add(key);

            //加一天
            calendar.add(Calendar.DATE , 1);
        }

        // 合并这些数据 先声明合并后的key
         String redisKey = RedisKeyUtil.getUVKey(dateFormat.format(start) , dateFormat.format(end));

        //keyList.toArray() 将集合对象转成数组对象
        redisTemplate.opsForHyperLogLog().union(redisKey , keyList.toArray());

        // 返回统计的结果
        return  redisTemplate.opsForHyperLogLog().size(redisKey);

    }

    // 将指定用户计入DAU  (每记录一个用户到DAU，就以userId为offset把该二进制串上该位置的0(false)换成1(true))
    public void recordDAU(int userId){
        String redisKey = RedisKeyUtil.getDAUKey(dateFormat.format(new Date()));
        redisTemplate.opsForValue().setBit(redisKey,userId,true);
    }

    // 统计指定日期范围内的DAU  在某个日期范围内活跃一天就算活跃
    public long calculateDAU(Date start , Date end){

        if(start == null || end == null){
            throw new IllegalArgumentException("参数不能为空");
        }
        // 整理该日期范围内的key
        // bitmap运算需要的key的类型是byte数组
        List<byte[]> keyList = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        // 设置日期为起始日期
        calendar.setTime(start);

        // 开始时间不晚于结束的时间的就循环
        while (!calendar.getTime().after(end)){
           String key = RedisKeyUtil.getDAUKey(dateFormat.format(calendar.getTime()));
            // 转化成byte数组存入
           keyList.add(key.getBytes());
            //加一天
            calendar.add(Calendar.DATE , 1);
        }

        // 进行OR运算  execute()默认返回是Object 转成long
        return (long) redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                String redisKey = RedisKeyUtil.getDAUKey(dateFormat.format(start),dateFormat.format(end));
                //keyList.toArray(new byte[0][0]) 转成数组并声明它要转化的类型
                connection.bitOp(RedisStringCommands.BitOperation.OR,redisKey.getBytes(),keyList.toArray(new byte[0][0]));
                return connection.bitCount(redisKey.getBytes());
            }
        });
    }
}
