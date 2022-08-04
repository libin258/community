package com.nowcoder.community.Controller;

import com.nowcoder.community.service.AlphaService;
import com.nowcoder.community.util.CommunityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@Controller
@RequestMapping("/alpha")
public class AlphaController {

    @Autowired
    private AlphaService alphaService; //调用AlphaService  就把AlphaService注入给AlphaController

    @RequestMapping("/hello")
    @ResponseBody
    public String sayHello(){
        return "Hello Sping Boot";
    }

    @RequestMapping("/data")
    @ResponseBody
    public String getData(){
       return alphaService.find();
    }


    @RequestMapping("/http")
    public void http(HttpServletRequest request, HttpServletResponse response){
        //获取请求数据
        System.out.println(request.getMethod());
        System.out.println(request.getServletPath());
        Enumeration<String> enumeration =  request.getHeaderNames();
        while(enumeration.hasMoreElements()){
           String name =  enumeration.nextElement();
           String value = request.getHeader(name);
            System.out.println(name + ":"+value);
        }
        System.out.println(request.getParameter("code"));
        //返回响应数据
        response.setContentType("text/html;charset=utf-8");
        // try后加个（） 把writer写入  会自动调用close方法
        try ( PrintWriter writer =  response.getWriter();
        ){
            writer.write("<h1>牛客网</h1>");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //获取请求数据
    //GET请求
    // /students?current=1&limit=20
    @RequestMapping(path = "/students",method = RequestMethod.GET)
    @ResponseBody  //返回一个字符串
    public String getStudents(
            @RequestParam(name ="current" ,required = false,defaultValue = "1") int current,
            @RequestParam(name ="limit" ,required = false,defaultValue = "10") int limit){
        System.out.println(current);
        System.out.println(limit);
        return "some students";
    }

    // /student/123
    @RequestMapping(path = "/student/{id}",method = RequestMethod.GET)
    @ResponseBody
    public String getStudent(@PathVariable("id") int id){
        System.out.println(id);
        return "a student";
    }

    //POST请求
    @RequestMapping(path = "/student" , method = RequestMethod.POST)
    @ResponseBody
    public String saveStudent(String name , int age){
        System.out.println(name);
        System.out.println(age);
        return "success";
    }

    //返回响应数据
    //响应HTML数据
    //第一种方法
    @RequestMapping(path = "/teacher", method = RequestMethod.GET)
    public ModelAndView getTeacher(){
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("name","张三");
        modelAndView.addObject("age",30);
        modelAndView.setViewName("/demo/view"); //templates默认模板为html文件，后缀不用写
        return modelAndView;
    }

    //第二种方法更简单一点
    @RequestMapping(path = "/school", method = RequestMethod.GET)
    public String getSchool(Model model){
        model.addAttribute("name","潍坊学院");
        model.addAttribute("age",80);
        return "/demo/view";
    }

    // 响应JSON数据(一般在异步请求当中)
    // java对象返回给浏览器  浏览器解析用的是JS 希望得到JS对象
    // java对象转成一个JSON字符串  然后传给浏览器 浏览器将其转化成JS对象
    @RequestMapping(path = "/emp" ,method = RequestMethod.GET)
    @ResponseBody
    //返回一个员工
    public Map<String,Object> getEmp(){
        Map<String , Object> map = new HashMap<>();
        map.put("name","李四");
        map.put("age",24);
        map.put("salary",10000);
        return map;
    }

    //返回全部员工
    @RequestMapping(path = "/emps" ,method = RequestMethod.GET)
    @ResponseBody
    //返回一个员工
    public List<Map<String,Object>> getEmps(){
        List<Map<String,Object>> list = new ArrayList<>();

        Map<String , Object> map = new HashMap<>();
        map.put("name","李四");
        map.put("age",24);
        map.put("salary",10000);
        list.add(map);

        map.put("name","张三");
        map.put("age",23);
        map.put("salary",11000);
        list.add(map);

        map.put("name","王五");
        map.put("age",22);
        map.put("salary",12000);
        list.add(map);

        return list;
    }

    // cookie示例
    @RequestMapping(path = "/cookie/set" , method = RequestMethod.GET)
    @ResponseBody
    public String setCookie(HttpServletResponse response){
        //创建cookie
        //必须传入参数，没有无参构造器 每一个参数都是字符串
        Cookie cookie = new Cookie("code" , CommunityUtil.generateUUID());
        //cookie生效的范围
        cookie.setPath("/community/alpha");
        //cookie生存时间 （cookie默认存在浏览器内存里 ， 设置时间后会存在硬盘里）
        cookie.setMaxAge(60 * 10);
        //发送cookie
        response.addCookie(cookie);

        return "set cookie";
    }

    @RequestMapping(path = "/cookie/get" , method = RequestMethod.GET)
    @ResponseBody
    public String getCookie(@CookieValue("code") String code){
        System.out.println(code);
        return  "get cookie";
    }

    // session示例
    @RequestMapping(path = "/session/set" , method = RequestMethod.GET)
    @ResponseBody
    // 声明后 springMVC会自动注入
    public String setSession(HttpSession session){
            session.setAttribute("id",1);
            session.setAttribute("name" , "Test");
            return  "set session";
    }

    @RequestMapping(path = "/session/get" , method = RequestMethod.GET)
    @ResponseBody
    public String getSession(HttpSession session){
        System.out.println(session.getAttribute("id"));
        System.out.println(session.getAttribute("name"));
        return "get session";
    }

    // ajax实例
    @RequestMapping(path = "/ajax" , method = RequestMethod.POST)
    @ResponseBody
    public String testAjax(String name , int age){
        System.out.println(name);
        System.out.println(age);
        return CommunityUtil.getJSONString(0,"操作成功");
    }

}
