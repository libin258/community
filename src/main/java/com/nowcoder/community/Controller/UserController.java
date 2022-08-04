package com.nowcoder.community.Controller;

import com.nowcoder.community.annotation.LoginRequire;
import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.*;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {

        @Value("${community.path.upload}")
        private String uploadPath;

        @Value("${community.path.domain}")
        private String domain;

        @Value("${server.servlet.context-path}")
        private String contextPath;

        @Autowired
        private UserService userService;

        @Autowired
        private HostHolder hostHolder;

        @Autowired
        private LikeService likeService;

        @Autowired
        private FollowService followService;

        @Autowired
        private DiscussPostService discussPostService;

        @Autowired
        private CommentService commentService;

        @Value("${qiniu.key.access}")
        private String accessKey;

        @Value("${qiniu.key.secret}")
        private String secretKey;

        @Value("${qiniu.bucket.header.name}")
        private String headerBucketName;

        @Value("${qiniu.bucket.header.url}")
        private String headerBucketUrl;


        private static final Logger logger = LoggerFactory.getLogger(UserController.class);

        @LoginRequire
        @RequestMapping(path = "/setting"  , method = RequestMethod.GET)
        public String getSetting(Model model){
                // 生成上传文件的名称
                String fileName = CommunityUtil.generateUUID();
                // 设置响应信息
                StringMap policy = new StringMap();
                // 成功是返回code:0
                policy.put("returnBody",CommunityUtil.getJSONString(0));
                // 生成上传凭证
                Auth auth = Auth.create(accessKey,secretKey);
                String uploadToken = auth.uploadToken(headerBucketName,fileName,3600,policy);

                model.addAttribute("uploadToken",uploadToken);
                model.addAttribute("fileName",fileName);
                return  "/site/setting";
        }

        // 更新头像的路径
        @RequestMapping(path = "/header/url" , method = RequestMethod.POST)
        @ResponseBody
        public String updateHeaderUrl(String fileName){
                if(StringUtils.isBlank(fileName)){
                        return CommunityUtil.getJSONString(1,"文件名不能为空");
                }

                // url就是空间的域名 + / +fileName;
                String url = headerBucketUrl + "/" + fileName;
                userService.updateHeader(hostHolder.getUser().getId(),url);

                return CommunityUtil.getJSONString(0,"成功");
        }

        // 废弃
        @LoginRequire
        @RequestMapping(path = "/upload" , method = RequestMethod.POST)
        public String uploadHeader(MultipartFile multipartFile , Model model){
                if(multipartFile ==  null){
                        model.addAttribute("error" ,"你还没有选择图片" );
                }

                String filename = multipartFile.getOriginalFilename();
                String suffix =  filename.substring(filename.lastIndexOf(".")+1);
                if(StringUtils.isBlank(suffix)){
                        model.addAttribute("error","文件格式不正确");
                        return "/site/setting";
                }

                //生成随机文件名
                filename = CommunityUtil.generateUUID()+suffix;

                //确定文件存放的路径
                File dest = new File(uploadPath + "/" + filename);
                //当前文件内容写入到目标文件
                try {
                        multipartFile.transferTo(dest);
                } catch (IOException e) {
                        logger.error("上传文件失败" + e.getMessage());
                        throw new RuntimeException("上传文件失败，服务器发生异常" ,  e);
                }
                //更新当前用户头像的用户(web访问路径)
                // http://localhost:8080/community/header/xxx.png
                User user = hostHolder.getUser();
                String headerUrl = domain + contextPath + "/user/header/" +filename;
                userService.updateHeader(user.getId(),headerUrl);

                return "redirect:/index";
        }

        // 废弃
        @RequestMapping(path = "/header/{filename}" , method = RequestMethod.GET)
        //@PathVariable("filename") String filename : 从路径中解析filename这个参数
        public void getHeader(@PathVariable("filename") String filename , HttpServletResponse response){
                // 服务器存放路径
               filename = uploadPath + "/" +filename;
                // 文件的后缀
                String suffix = filename.substring(filename.lastIndexOf(".")+1);
                // 响应图片
                response.setContentType("image/"+ suffix);
                try (
                        OutputStream os = response.getOutputStream();
                        // 读取文件得到输入流
                        FileInputStream fis = new FileInputStream(filename);
                        ){
//                        OutputStream os = response.getOutputStream();
//                        // 读取文件得到输入流
//                        FileInputStream fis = new FileInputStream(filename);
                        byte[] buffer = new byte[1024];
                        int b = 0;
                        while((b = fis.read(buffer)) != -1){
                                os.write(buffer , 0 , b);
                        }
                } catch (IOException e) {
                        logger.error("读取图像失败"+e.getMessage());
                }
        }

        @RequestMapping(path = "/updatePwd" , method = RequestMethod.POST)
        public String updatePwd(String pwd , String pwd1 ,Model model){

                User user = hostHolder.getUser();
                Map<String , Object> map = userService.updatePassword(user.getId(),pwd, pwd1);
                if(map.isEmpty() || map==null) {
                        return "redirect:/index";
                }else{
                        model.addAttribute("newPwd",map.get("newPwd"));
                        model.addAttribute("oldPwd",map.get("oldPwd"));
                        return "/site/setting";
                }
        }

        //个人主页
        @RequestMapping(path = "/profile/{userId}" ,method = RequestMethod.GET)
        public String getProfile(@PathVariable("userId") int userId , Model model ){
                User user = userService.findUserById(userId);
                if(user == null){
                        throw new RuntimeException("该用户不存在");
                }

                // 用户
                model.addAttribute("user" , user );
                // 点赞数量
                int likeCount = likeService.findUserLikeCount(userId);
                model.addAttribute("likeCount" , likeCount);

                // 关注数量
                long followeeCount = followService.findFolloweeCount(userId,ENTITY_TYPE_USER);
                model.addAttribute("followeeCount" , followeeCount);
                // 粉丝数量
                long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER , userId);
                model.addAttribute("followerCount" , followerCount);
                // 当前登录是否关注了该实体
                boolean hasFollow = false;
                if(hostHolder.getUser() != null){
                      hasFollow = followService.hasFollowed(hostHolder.getUser().getId(),ENTITY_TYPE_USER,userId);
                }
                model.addAttribute("hasFollow" , hasFollow);
                return "/site/profile";
        }

        // 我的帖子
        @RequestMapping(path = "/post/{userId}" , method = RequestMethod.GET)
        public String getMyPost(@PathVariable("userId") int userId , Page page , Model model){
                User user = userService.findUserById(userId);
                if(user == null){
                        throw new RuntimeException("该用户不存在");
                }
                model.addAttribute("user",user);

                page.setPath("/user/post/" + userId);
                page.setLimit(5);
                page.setRows(discussPostService.findDiscussPostRows(userId));

                // 帖子列表
                List<DiscussPost> list = discussPostService.findDiscussPosts(userId,page.getoffset(),page.getLimit(),1);
                List<Map<String,Object>> discussList = new ArrayList<>();
                if(list != null){
                        for (DiscussPost post : list){
                                Map<String , Object> map = new HashMap<>();
                                map.put("discussPost",post);
                                map.put("likeCount",likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));
                                discussList.add(map);
                        }
                }
                model.addAttribute("discussList",discussList);

                return  "/site/my-post";
        }

        // 我的回复
        @RequestMapping(path = "/reply/{userId}" , method = RequestMethod.GET)
        public String getMyReply(@PathVariable("userId") int userId ,Page page ,Model model){
                User user = userService.findUserById(userId);
                if(user == null){
                        throw  new RuntimeException("该用户不存在");
                }
                model.addAttribute("user",user);

                page.setPath("/user/reply/" + userId);
                page.setLimit(5);
                page.setRows(commentService.findCountByUser(userId));
                List<Comment> list = commentService.findUserComments(userId,page.getoffset(),page.getLimit());
                List<Map<String,Object>> commentList = new ArrayList<>();
                if(list != null){
                        for(Comment comment : list){
                                Map<String,Object> map = new HashMap<>();
                                map.put("comment",comment);
                                DiscussPost post = discussPostService.findDiscussPost(comment.getEntityId());
                                map.put("discussPost",post);
                                commentList.add(map);
                        }
                }
                model.addAttribute("commentList",commentList);
                return "/site/my-reply";
        }
}
