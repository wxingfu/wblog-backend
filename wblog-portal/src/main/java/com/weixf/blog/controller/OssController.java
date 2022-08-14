package com.weixf.blog.controller;

import com.weixf.blog.common.exception.GlobalException;
import com.weixf.blog.service.OssService;
import com.weixf.blog.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/oss/file")
public class OssController {

    @Autowired
    private OssService ossService;

    @PostMapping
    public Result upload(@RequestParam("image") MultipartFile file) {
        try {
            String url = ossService.uploadFile(file);
            return Result.success(url);
        } catch (GlobalException e) {
            e.printStackTrace();
            return Result.fail(e.getCode(), e.getMsg());
        }
    }

    @PostMapping("/remove")
    public Result removeOssFile(String cover) {
        try {
            List<String> name = ossService.removeFile(cover);
            return Result.success(name);
        } catch (GlobalException e) {
            e.printStackTrace();
            return Result.fail(e.getCode(), e.getMsg());
        }
    }
}
