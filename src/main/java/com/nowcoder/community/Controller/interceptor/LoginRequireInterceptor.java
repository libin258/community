package com.nowcoder.community.Controller.interceptor;

import com.nowcoder.community.annotation.LoginRequire;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

@Component
public class LoginRequireInterceptor implements HandlerInterceptor {

    @Autowired
    private HostHolder hostHolder;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断拦截的是不是方法 如果是它是HandlerMethod类型
        if(handler instanceof HandlerMethod){
            HandlerMethod handlerMethod = (HandlerMethod)handler;
            //直接获取拦截到的method对象
            Method method = handlerMethod.getMethod();
            //取注解
            LoginRequire loginRequire = method.getAnnotation(LoginRequire.class);
            //不为空即当前方法需要登录  && 等于空即当前没用登录
            if(loginRequire != null && hostHolder.getUser() == null){
                response.sendRedirect(request.getContextPath()+"/login");
                return  false;
            }
        }
        return true;
    }
}
