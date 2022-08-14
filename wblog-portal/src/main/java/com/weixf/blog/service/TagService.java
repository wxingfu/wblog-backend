package com.weixf.blog.service;


import com.weixf.blog.vo.Result;
import com.weixf.blog.vo.TagVo;

import java.util.List;


public interface TagService {

    List<TagVo> findTagsByArticleId(Long articleId);

    Result hots(int limit);

    /**
     * 查询所有文章标签
     */
    Result findAll();

    Result findAllDetail();

    Result findADetailById(Long id);
}
