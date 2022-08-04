$(function(){
        // 点击submit触发表单的提交事件时 由upload处理
        $("#uploadForm").submit(upload);
});

function upload() {
   $.ajax({
          url:"http://upload.qiniup.com",
          method:"post",
          // 不要把表单的内容转化成字符串
          processData:false,
          // 浏览器自己设置类型
          contentType:false,
        data:new FormData($("#uploadForm")[0]),
        success: function(data) {
            if(data && data.code == 0) {
                // 更新头像访问路径
                $.post(
                    CONTEXT_PATH + "/user/header/url",
                    {"fileName":$("input[name='key']").val()},
                    function(data) {
                        data = $.parseJSON(data);
                        if(data.code == 0) {
                            window.location.reload();
                        } else {
                            alert(data.msg);
                        }
                    }
                );
            } else {
                alert("上传失败!");
            }
        }
    });

     // 上面的逻辑已经处理完请求，不用继续执行
    return false;
}