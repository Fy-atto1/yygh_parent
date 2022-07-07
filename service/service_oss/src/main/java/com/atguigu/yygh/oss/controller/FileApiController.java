package com.atguigu.yygh.oss.controller;

import com.atguigu.yygh.common.result.Result;
import com.atguigu.yygh.oss.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/oss/file")
public class FileApiController {

    private final FileService fileService;

    @Autowired
    public FileApiController(FileService fileService) {
        this.fileService = fileService;
    }

    // 上传文件到阿里云oss
    @PostMapping("fileUpload")
    public Result<String> fileUpload(MultipartFile file) {
        // 获取上传的文件
        String fileUrl = fileService.upload(file);
        return Result.ok(fileUrl);
    }

}
