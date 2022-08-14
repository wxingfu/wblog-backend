package com.weixf.blog.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weixf.blog.dao.pojo.Tag;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TagMapper extends BaseMapper<Tag> {

    /**
     * 根据文章id查询标签列表
     */
    List<Tag> findTagsByArticleId(Long articleId);

    /**
     * 查询最热的标签前n条
     */
    List<Long> findHotsTagIds(int limit);

    List<Tag> findTagsByTagIds(List<Long> tagIds);
}
