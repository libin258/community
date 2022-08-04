package com.nowcoder.community.util;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CommunityUtil {

    // 生成随机字符串
    public static String generateUUID(){
        return UUID.randomUUID().toString().replaceAll("-","");//把所有的-替换成空的字符串

    }

    //MD5加密
    //  + salt
    public static String md5(String key){
        if(StringUtils.isBlank(key)){
            return  null;
        }
        return DigestUtils.md5DigestAsHex(key.getBytes());
    }

    public static String getJSONString(int code , String msg , Map<String , Object> map){
        JSONObject json = new JSONObject();
        // 把参数封装到json对象中
        json.put("code",code);
        json.put("msg",msg);
        if(map != null){
            for(String key: map.keySet()){
                json.put(key,map.get(key));
            }
        }
        // 转成字符串
        return  json.toJSONString();
    }
    // 方法重载
    public static String getJSONString(int code , String msg){
        return getJSONString(code , msg , null);
    }

    public static String getJSONString(int code){
        return  getJSONString(code , null , null);
    }

    public static void main(String[] args) {
        Map<String , Object> map = new HashMap<>();
        map.put("name" , "zhangsan");
        map.put("age" ,25);
        System.out.println(getJSONString(0,"ok",map));
    }
}
