package com.nowcoder.community.actuator;

import com.nowcoder.community.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
// 给端点起名
@Endpoint(id = "database")
// 自定义一个端点
public class DatabaseEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseEndpoint.class);

    @Autowired
    private DataSource dataSource;

    // @ReadOperation: 这个方法是一个get请求 如何是其他的请求要访问其他的Operation
    @ReadOperation
    public String checkConnection(){
        try(
                Connection conn = dataSource.getConnection();
                ) {
                return CommunityUtil.getJSONString(0,"获取连接成功");
        } catch (SQLException e) {
            logger.error("获取连接失败" + e.getMessage());
            return CommunityUtil.getJSONString(1,"获取连接失败");
        }
    }
}
