// 页面加载完之后 动态绑定事件
// 页面加载完之后调用
$(function(){
        $("#topBtn").click(setTop);
        $("#wonderfulBtn").click(setWonderful);
        $("#deleteBtn").click(setDelete);
});



function like(btn , entityType , entityId , entityUserId ,postId){

     $.post(
        CONTEXT_PATH + "/like",
        {"entityType":entityType , "entityId":entityId,"entityUserId":entityUserId,"postId":postId},
        function(data){
            data = $.parseJSON(data);
            if(data.code == 0){
                $(btn).children("i").text(data.likeCount);
                $(btn).children("b").text(data.likeStatus==1?'已赞':'赞');
            }else{
                alert(data.msg);
            }

        }
     );
}

// 置顶
function setTop(){
        var id = $("#postId").val();
        $.post(
            CONTEXT_PATH + "/discuss/top",
            {"id":id},
            function(data){
                data = $.parseJSON(data);
                if(data.code == 0){
                    $("#topBtn").text(data.type == 1?'已置顶':'置顶');
                    window.location.reload();
                }else{
                    alert(data.msg);
                }
            }
        );
}

// 加精
function setWonderful(){
        var id = $("#postId").val();
        $.post(
            CONTEXT_PATH + "/discuss/wonderful",
            {"id":id},
            function(data){
                data = $.parseJSON(data);
                if(data.code == 0){
                // 设置不可点击
//                    $("#wonderfulBtn").attr("disabled","disabled");
                    $("#wonderfulBtn").text(data.status == 1?'已加精':'加精');
                    window.location.reload();
                }else{
                    alert(data.msg);
                }
            }
        );
}

// 删除
function setDelete(){
        var id = $("#postId").val();
        $.post(
            CONTEXT_PATH + "/discuss/delete",
            {"id":id},
            function(data){
                data = $.parseJSON(data);
                if(data.code == 0){
                       location.href = CONTEXT_PATH + "/index";
                }else{
                    alert(data.msg);
                }
            }
        );
}
