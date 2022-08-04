$(function(){
	$("#publishBtn").click(publish);
});

function publish() {
	$("#publishModal").modal("hide");

	// 发送AJAX请求之前，将CSRF令牌设置到请求的消息头中
//	var token = $("meta[name='_csrf']").attr("content"); // 获取meta元素中name等于_csrf的meta中content属性的值
//	var header = $("meta[name='_csrf_header']").attr("content");
//	// 对请求作一个设置 通过xhr（发送异步请求的核心的对象）设置header
//    $(document).ajaxSend(function(e,xhr,options){
//            xhr.setRequestHeader(header,token);
//    });


	// 获取标题和内容
	var title = $("#recipient-name").val();
	var content = $("#message-text").val();

	//发送异步请求
	$.post(
	    CONTEXT_PATH + "/discuss/add",
	    {"title":title,"content":content},
	    function(data){
	        data = $.parseJSON(data);
	        // 在提示框当中显示返回的消息
	        $("#hintBody").text(data.msg);
	        // 显示提示框
	        $("#hintModal").modal("show");
	        // 2秒后,自动隐藏提示框
	        setTimeout(function(){
            		$("#hintModal").modal("hide");

            		//刷新页面
            		if(data.code == 0){
            		    window.location.reload();
            		}
            	}, 2000);
	    }
	);

}