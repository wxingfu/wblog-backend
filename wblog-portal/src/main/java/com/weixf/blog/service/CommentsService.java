package com.weixf.blog.service;


import com.weixf.blog.vo.Result;
import com.weixf.blog.vo.params.CommentParam;

public interface CommentsService {

    /**
     * 根据文章id查询所有的评论列表
     */
    Result commentsByArticleId(Long id);

    Result comment(CommentParam commentParam);
}
