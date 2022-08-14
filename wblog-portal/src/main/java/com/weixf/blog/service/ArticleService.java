package com.weixf.blog.service;

import com.weixf.blog.vo.Result;
import com.weixf.blog.vo.params.ArticleParam;
import com.weixf.blog.vo.params.PageParams;

public interface ArticleService {

    /**
     * 分页查询文章列表
     */
    Result listArticle(PageParams pageParams);

    /**
     * 最热文章
     */
    Result hotArticle(int limit);

    /**
     * 最新文章
     */
    Result newArticles(int limit);

    /**
     * 文章归档
     */
    Result listArchives();

    /**
     * 查看文章详情
     */
    Result findArticleById(Long articleId);

    /**
     * 文章发布服务
     */
    Result publish(ArticleParam articleParam);
}
