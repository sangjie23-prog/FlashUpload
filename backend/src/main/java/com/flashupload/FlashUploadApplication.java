package com.flashupload;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 第一阶段先搭建最小可运行后端骨架，后续上传能力都在此基础上逐步扩展。
 */
@SpringBootApplication
public class FlashUploadApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlashUploadApplication.class, args);
    }
}
