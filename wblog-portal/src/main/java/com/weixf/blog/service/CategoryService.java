package com.weixf.blog.service;


import com.weixf.blog.vo.CategoryVo;
import com.weixf.blog.vo.Result;

public interface CategoryService {

    CategoryVo findCategoryById(Long categoryId);

    Result findAll();

    Result findAllDetail();

    Result categoryDetailById(Long id);
}
