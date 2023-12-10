package com.weixf.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weixf.blog.dao.mapper.ArticleMapper;
import com.weixf.blog.dao.mapper.CommentMapper;
import com.weixf.blog.dao.pojo.Article;
import com.weixf.blog.dao.pojo.Comment;
import com.weixf.blog.dao.pojo.SysUser;
import com.weixf.blog.service.CommentsService;
import com.weixf.blog.service.SysUserService;
import com.weixf.blog.utils.UserThreadLocal;
import com.weixf.blog.vo.CommentVo;
import com.weixf.blog.vo.Result;
import com.weixf.blog.vo.UserVo;
import com.weixf.blog.vo.params.CommentParam;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
public class CommentsServiceImpl implements CommentsService {
    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private ArticleMapper articleMapper;

    // 查询作者信息
    @Autowired
    private SysUserService sysUserService;

    @Override
    public Result commentsByArticleId(Long id) {
        /*
         * 1、根据文章id查询评论列表从comment表 里面查询
         * 2、根据作者id查询作者的信息
         * 3、判断如果level==1要去查询它有没有子评论
         * 4、如果有 根据评论id进行查询（parent_id)
         */
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        // 根据文章id进行查询
        queryWrapper.eq(Comment::getArticleId, id);
        // 根据层级关系进行查询
        queryWrapper.eq(Comment::getLevel, 1);
        List<Comment> comments = commentMapper.selectList(queryWrapper);
        List<CommentVo> commentVoList = copyList(comments);
        return Result.success(commentVoList);
    }

    // 登陆
    @Override
    public Result comment(CommentParam commentParam) {
        // 拿到当前登陆用户
        SysUser sysUser = UserThreadLocal.get();
        Comment comment = new Comment();
        Long articleId = commentParam.getArticleId();
        comment.setArticleId(articleId);
        comment.setAuthorId(sysUser.getId());
        comment.setContent(commentParam.getContent());
        comment.setCreateDate(System.currentTimeMillis());
        Long parent = commentParam.getParent();
        if (parent == null || parent == 0) {
            comment.setLevel(1);
        } else {
            comment.setLevel(2);
        }
        // 如果是空，parent就是0
        comment.setParentId(parent == null ? 0 : parent);
        Long toUserId = commentParam.getToUserId();
        comment.setToUid(toUserId == null ? 0 : toUserId);
        int insert = this.commentMapper.insert(comment);
        if (insert > 0) {
            Article article = new Article();
            article.setId(articleId);
            Long commentCount = getCommentCount(articleId);
            article.setCommentCounts(commentCount.intValue());
            articleMapper.updateById(article);
        }
        return Result.success(null);

    }

    private Long getCommentCount(Long articleId) {
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getArticleId, articleId);
        return commentMapper.selectCount(queryWrapper);
    }


    private List<CommentVo> copyList(List<Comment> comments) {
        List<CommentVo> commentVoList = new ArrayList<>();
        for (Comment comment : comments) {
            commentVoList.add(copy(comment));
        }
        return commentVoList;
    }

    private CommentVo copy(Comment comment) {
        CommentVo commentVo = new CommentVo();
        // 只能拷贝类型相同的
        BeanUtils.copyProperties(comment, commentVo);
        commentVo.setId(String.valueOf(comment.getId()));
        // 作者信息
        Long authorId = comment.getAuthorId();
        UserVo userVo = this.sysUserService.findUserVoById(authorId);
        commentVo.setAuthor(userVo);
        // 子评论
        Integer level = comment.getLevel();
        if (level == 1) {
            Long id = comment.getId();
            List<CommentVo> commentVoList = findCommentsByParentId(id);
            commentVo.setChildrens(commentVoList);
        }
        // to user 给谁评论
        if (level > 1) {
            Long toUid = comment.getToUid();
            UserVo touserVo = this.sysUserService.findUserVoById(toUid);
            commentVo.setToUser(touserVo);
        }
        return commentVo;
    }

    private List<CommentVo> findCommentsByParentId(Long id) {
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getParentId, id);
        queryWrapper.eq(Comment::getLevel, 2);
        return copyList(commentMapper.selectList(queryWrapper));
    }
}
