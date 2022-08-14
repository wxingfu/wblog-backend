package com.weixf.blog.controller;


import com.weixf.blog.common.aop.LogAnnotation;
import com.weixf.blog.common.cache.Cache;
import com.weixf.blog.service.ArticleService;
import com.weixf.blog.vo.Result;
import com.weixf.blog.vo.params.ArticleParam;
import com.weixf.blog.vo.params.PageParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/articles")
public class ArticleController {


    @Autowired
    private ArticleService articleService;


    /**
     * 首页文章列表
     */
    // @Cache(expire = 5 * 60 * 1000, name = "list_article")
    @LogAnnotation(module = "文章", operation = "获取文章列表")
    @PostMapping
    public Result listArticle(@RequestBody PageParams pageParams) {
        return articleService.listArticle(pageParams);
    }

    /**
     * 首页最热文章
     */
    @PostMapping("hot")
    @Cache(expire = 5 * 60 * 1000, name = "hot_article")
    public Result hotArticle() {
        int limit = 5;
        return articleService.hotArticle(limit);
    }

    /**
     * 首页最新文章
     */
    @PostMapping("new")
    @Cache(expire = 5 * 60 * 1000, name = "new_article")
    public Result newArticles() {
        int limit = 5;
        return articleService.newArticles(limit);
    }

    /**
     * 最新文章
     */
    @PostMapping("listArchives")
    public Result listArchives() {
        return articleService.listArchives();

    }

    /**
     * 文章详情
     */
    @PostMapping("view/{id}")
    public Result findArticleById(@PathVariable("id") Long articleId) {
        return articleService.findArticleById(articleId);
    }

    /**
     * `@RequestBody`主要用来接收前端传递给后端的json字符串中的数据的(请求体中的数据的)；
     * 而最常用的使用请求体传参的无疑是POST请求了，所以使用@RequestBody接收数据时，一般都用POST方式进行提交。
     */
    @PostMapping("publish")
    public Result publish(@RequestBody ArticleParam articleParam) {
        return articleService.publish(articleParam);
    }


}
