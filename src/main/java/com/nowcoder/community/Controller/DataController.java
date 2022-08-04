package com.nowcoder.community.Controller;

import com.nowcoder.community.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
public class DataController {

    @Autowired
    private DataService dataService;

    // 统计页面
    @RequestMapping(path = "/data" , method = {RequestMethod.GET,RequestMethod.POST})
    public String getDataPage(){
        return "/site/admin/data";
    }

    //  统计网站UV
    @RequestMapping(path = "/data/uv" , method = RequestMethod.POST)
    // 告诉服务器传的日期的字符串是什么格式  @DateTimeFormat(pattern = "yyyy-MM-dd")
    public String getUV(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start ,
                        @DateTimeFormat(pattern = "yyyy-MM-dd") Date end ,Model model){

        long uv = dataService.calculateUV(start,end);
        model.addAttribute("uvResult",uv);
        model.addAttribute("uvStart" , start);
        model.addAttribute("uvEnd" , end);

        return "forward:/data";
    }

    // 统计活跃用户
    @RequestMapping(path = "/data/dau" , method = RequestMethod.POST)
    // 告诉服务器传的日期的字符串是什么格式  @DateTimeFormat(pattern = "yyyy-MM-dd")
    public String getDAU(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start ,
                        @DateTimeFormat(pattern = "yyyy-MM-dd") Date end ,Model model){

        long dau = dataService.calculateDAU(start,end);
        model.addAttribute("dauResult",dau);
        model.addAttribute("dauStart" , start);
        model.addAttribute("dauEnd" , end);

        return "forward:/data";
    }

}
