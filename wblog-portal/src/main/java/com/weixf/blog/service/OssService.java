package com.weixf.blog.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface OssService {

    String uploadFile(MultipartFile file);

    List<String> removeFile(String cover);

}
