package com.nowcoder.community.dao;

import com.nowcoder.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CommentMapper {

    List<Comment> selectCommentByEntity(int entityType , int entityId , int offset , int limit);

    int selectCountByEntity(int entityType , int entityId);

    int insertComment(Comment comment);

    int selectCountByUser(int userId);

    List<Comment> selectCommentByUser(int userId , int offset ,int limit);

    Comment selectCommentById(int id);

}
