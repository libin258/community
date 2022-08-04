package com.nowcoder.community.util;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveFilter {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);

    //替换的符号
    private static final String REPLACEMENT = "***";

    // 初始化根节点
    private TrieNode rootNode = new TrieNode();

    /*
    * @PostConstruct : 这是一个初始化方法，当容器实例化SensitiveFilter这个bean以后
    * 在调用这个bean的构造器之后，init（）这个方法被自动调用，这个bean在服务器启动就被初始化
    * init（）这个方法在服务启动时就被调用
    * */
    @PostConstruct
    public void init(){

        try (
                //加载字节流
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                //在字节流里读文字不方便，转化为字符流 再 转化为缓冲流
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                ){
            //放置敏感词的变量
            String keyword;
            //reader.readLine() 配置文件里是一行一个敏感词
            while((keyword = reader.readLine()) != null){
                // 把敏感词添加到前缀树
                this.addKeyWord(keyword);
            }

        } catch (Exception e) {
            logger.error("加载敏感词文件失败" + e.getMessage());
        }
    }

    // 将一个敏感词添加到前缀数中去
    private void addKeyWord(String keyword){
            //让这个指针默认指向根
            TrieNode tempNode = rootNode;
            for(int i = 0; i < keyword.length();i++){
                // 得到的字符
                char c = keyword.charAt(i);
                TrieNode subNode = tempNode.getSubNode(c);
                if(subNode == null){
                    // 初始化子节点
                    subNode = new TrieNode();
                    // 把子节点挂到当前节点之下
                    tempNode.addSubNode(c , subNode);
                }
                // 让指针指向子节点，进入下一循环
                tempNode = subNode;

                // 设置结束的标识
                if(i == keyword.length()-1){
                    tempNode.setKeywordEnd(true);
                }
            }
    }

    /*
    *  过滤敏感词
    *
    * @param text 待过滤文本
    * @return 过滤后的文本
    * */
    public  String filter(String text){
        if(StringUtils.isBlank(text)){
            return  null;
        }

        // 指针1
        TrieNode tempNode = rootNode;
        // 指针2
        int begin = 0;
        // 指针3
        int position = 0;
        // 结果 不断追加的字符串用StringBuffer
        StringBuffer stringBuffer = new StringBuffer();
        while(begin < text.length()){
            char c = text.charAt(position);

            //跳过符号
            if(isSymbol(c)){
                // 若指针1指向根节点,将此符号计入结果，让指针2向下走
                if(tempNode == rootNode){
                    stringBuffer.append(c);
                    begin++;
                }
                // 无论符号在开头或中间，指针3都向下走一步
                position++;
                continue; //执行continue语句,continue语句会终止本次循环,循环体中continue之后的语句将不再执行,接着进行下次循环
            }
            // 检查下级节点
            tempNode = tempNode.getSubNode(c);
            if(tempNode == null){
                // 以begin开头的字符串不是敏感词
                stringBuffer.append(text.charAt(begin));
                // 进入下一阶段
                position = ++begin;
                //重新指向根节点
                tempNode = rootNode;
            } else if (tempNode.isKeywordEnd()) {
                // 发现敏感词,将begin-position中间的字符串替换掉
                stringBuffer.append(REPLACEMENT);
                // 进入下一位置
                begin = ++position;
                //重新指向根节点
                tempNode = rootNode;
            }else {
                if(position < text.length()-1) {
                    // 检查下一个字符
                    position++;
//                    continue;
                }
//                stringBuffer.append(text.charAt(begin));
//                position  = ++begin;
//                tempNode = rootNode;
            }
        }
        // 将最后一批字符计入结果
        stringBuffer.append(text.substring(begin));

        return  stringBuffer.toString();
    }
    // 判断是否为符号
    private boolean isSymbol(Character c){
        /*
        * CharUtils.isAsciiAlphanumeric: 如果是普通字符返回true
        * 特殊符号返回false
        * 0x2E80~0x2E80 是东亚文字
        * */
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);
    }

    // 前缀树
    //描述前缀树节点
    private class TrieNode{

        // 关键词结束的标识
        private boolean isKeywordEnd = false;

        // 字节点(key是下级字符，value是下级节点)
        private Map<Character , TrieNode> subNodes = new HashMap<>();

        public boolean isKeywordEnd() {
            return isKeywordEnd;
        }

        public void setKeywordEnd(boolean keywordEnd) {
            isKeywordEnd = keywordEnd;
        }

        // 添加子节点的方法
        public void addSubNode(Character c , TrieNode node){
            subNodes.put(c , node);
        }

        //获取子节点 通过key取value
        public TrieNode getSubNode(Character c){
            return subNodes.get(c);
        }
    }
}
