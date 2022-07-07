package com.atguigu.yygh.oss.service.impl;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.atguigu.yygh.oss.service.FileService;
import com.atguigu.yygh.oss.utils.ConstantOssPropertiesUtils;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {
    // 上传文件到阿里云oss
    @Override
    public String upload(MultipartFile file) {
        String endpoint = ConstantOssPropertiesUtils.ENDPOINT;
        String accessKeyId = ConstantOssPropertiesUtils.ACCESS_KEY_ID;
        String accessKeySecret = ConstantOssPropertiesUtils.SECRET;
        String bucketName = ConstantOssPropertiesUtils.BUCKET;
        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            // 上传文件流
            InputStream inputStream = file.getInputStream();
            String fileName = file.getOriginalFilename();
            // 使用uuid生成随机唯一值，拼接到文件名之前
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            fileName = uuid + fileName;
            // 根据当前日期创建文件夹，将文件上传到文件夹里
            // 例如：2022/02/02/a.jpg
            String time = new DateTime().toString("yyyy/MM/dd");
            fileName = time + "/" + fileName;
            // 创建PutObject请求进行上传
            ossClient.putObject(bucketName, fileName, inputStream);
            // 返回上传之后的文件路径
            return "https://" + bucketName + "." + endpoint + "/" + fileName;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            // 关闭ossClient
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
}
