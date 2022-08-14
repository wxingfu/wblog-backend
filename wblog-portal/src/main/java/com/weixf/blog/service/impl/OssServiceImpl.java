package com.weixf.blog.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.DeleteObjectsRequest;
import com.aliyun.oss.model.DeleteObjectsResult;
import com.weixf.blog.common.exception.GlobalException;
import com.weixf.blog.config.OSSProperties;
import com.weixf.blog.service.OssService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class OssServiceImpl implements OssService {

    @Override
    public String uploadFile(MultipartFile file) {

        String endpoint = OSSProperties.END_POINT;
        String accessKeyId = OSSProperties.ACCESS_KEY_ID;
        String accessKeySecret = OSSProperties.ACCESS_KEY_SECRET;
        String bucketName = OSSProperties.BUCKET_NAME;

        try {
            // 创建OSS实例。
            OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            //获取上传文件输入流
            InputStream inputStream = file.getInputStream();
            //获取文件名称
            String fileName = file.getOriginalFilename();
            //1 在文件名称里面添加随机唯一的值
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            // yuy76t5rew01.jpg
            fileName = uuid + fileName;
            //2 把文件按照日期进行分类
            //获取当前日期
            //   2019/11/12
            String datePath = new DateTime().toString("yyyyMMdd");
            //拼接
            //  2019/11/12/ewtqr313401.jpg
            fileName = datePath + "/" + fileName;
            //调用oss方法实现上传
            //第一个参数  Bucket名称
            //第二个参数  上传到oss文件路径和文件名称   aa/bb/1.jpg
            //第三个参数  上传文件输入流
            ossClient.putObject(bucketName, fileName, inputStream);
            // 关闭OSSClient。
            ossClient.shutdown();
            //把上传之后文件路径返回
            //需要把上传到阿里云oss路径手动拼接出来
            //  https://edu-guli-1010.oss-cn-beijing.aliyuncs.com/01.jpg
            return "https://" + bucketName + "." + endpoint + "/" + fileName;
        } catch (Exception e) {
            e.printStackTrace();
            throw new GlobalException(-999, "上传失败");
        }
    }

    @Override
    public List<String> removeFile(String cover) {
        // // 工具类获取值
        String endpoint = OSSProperties.END_POINT;
        String accessKeyId = OSSProperties.ACCESS_KEY_ID;
        String accessKeySecret = OSSProperties.ACCESS_KEY_SECRET;
        String bucketName = OSSProperties.BUCKET_NAME;

        // https://edu-guli-pics.oss-cn-beijing.aliyuncs.com/20210223/3a5d9531e0f44bbf90a19f0c6647f2c7666.jpg
        String coverName = cover.substring(cover.lastIndexOf("/", 50));
        coverName = coverName.substring(1);
        log.info(coverName);
        try {
            // // 创建OSSClient实例。
            // OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            // // 删除文件。如需删除文件夹，请将ObjectName设置为对应的文件夹名称。
            // // 如果文件夹非空，则需要将文件夹下的所有object删除后才能删除该文件夹。
            // ossClient.deleteObject(bucketName, coverName);
            // // 关闭OSSClient。
            // ossClient.shutdown();

            // 创建OSSClient实例。
            OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            // 删除文件。key等同于ObjectName，
            // 表示删除OSS文件时需要指定包含文件后缀在内的完整路径，例如abc/efg/123.jpg。
            List<String> keys = new ArrayList<>();
            keys.add(coverName);
            DeleteObjectsResult deleteObjectsResult =
                    ossClient.deleteObjects(new DeleteObjectsRequest(bucketName).withKeys(keys));
            List<String> deletedObjects = deleteObjectsResult.getDeletedObjects();
            // 关闭OSSClient。
            ossClient.shutdown();
            return deletedObjects;
        } catch (Exception e) {
            throw new GlobalException(-999, "删除失败");
        }
    }
}
