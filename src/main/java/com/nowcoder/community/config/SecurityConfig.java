package com.nowcoder.community.config;

import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter implements CommunityConstant {

    @Override
    public void configure(WebSecurity web) throws Exception {

        // 忽略掉对静态资源的拦截
        web.ignoring().antMatchers("/resources/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 授权
        // 有下面这些权限才可以访问以下路径
        http.authorizeRequests()
                .antMatchers(
                        "/user/setting",
                        "/user/upload",
                        "/user/updatePwd",
                        "/discuss/add",
                        "/comment/add/**",
                        "/letter/**",
                        "/notice/**",
                        "/like",
                        "/follow",
                        "/unfollow"
//                        "/user/profile/**"
                )
                .hasAnyAuthority(
                        AUTHORITY_USER,
                        AUTHORITY_ADMIN,
                        AUTHORITY_MODERATOR
                )
                .antMatchers(
                        "/discuss/top",
                        "/discuss/wonderful"
                )
                .hasAnyAuthority(
                        AUTHORITY_MODERATOR
                )
                .antMatchers(
                        "/discuss/delete",
                        "/data/**",
                        "/actuator/**"
                )
                .hasAnyAuthority(
                        AUTHORITY_ADMIN
                )
                // 其他的任何请求都允许
                .anyRequest().permitAll()
                // 不启用csrf攻击的检查
                .and().csrf().disable();

        // 权限不够时的处理
        http.exceptionHandling()
                // 没登录时怎么处理
                .authenticationEntryPoint(new AuthenticationEntryPoint() {
                    @Override
                    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
                        // 判断请求是同步还是异步  先从消息头中取一个字符串
                    String xRequestedWith =  request.getHeader("x-requested-with");
                    //  如何相等就是异步请求
                    if("XMLHttpRequest".equals(xRequestedWith)){
                        // 返回的数据类型是普通的字符串
                            response.setContentType("application/plain;charset=utf-8");
                            // 获取字符流
                        PrintWriter writer = response.getWriter();
                        writer.write(CommunityUtil.getJSONString(403 ,"你还没有登录!"));
                    }else {
                        response.sendRedirect(request.getContextPath()+"/login");
                    }
                    }
                })
                // 权限不够时怎么处理
                .accessDeniedHandler(new AccessDeniedHandler() {
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException, ServletException {

                        String xRequestedWith =  request.getHeader("x-requested-with");

                        if("XMLHttpRequest".equals(xRequestedWith)){

                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(CommunityUtil.getJSONString(403 ,"你没有访问此功能的权限!"));
                        }else {
                            response.sendRedirect(request.getContextPath()+"/denied");
                        }
                    }

                });
        // Security底层默认拦截/logout 请求，进行退出处理
        // 覆盖它的默认的逻辑，才能执行我们自己的退出代码
        //  更改它的默认路径为securitylogout
        http.logout().logoutUrl("/securitylogout");

    }
}
