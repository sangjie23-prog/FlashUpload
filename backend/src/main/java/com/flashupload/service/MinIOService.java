package com.flashupload.service;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.InputStream;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 阶段11：MinIO 服务类
 * 封装 MinIO 对象存储操作：上传、下载、删除文件
 */
@Service
public class MinIOService {

    private static final Logger log = LoggerFactory.getLogger(MinIOService.class);

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    public MinIOService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * 应用启动后初始化 bucket
     */
    @PostConstruct
    public void initBucket() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build()
                );
                log.info("MinIO bucket 创建成功: {}", bucketName);
            } else {
                log.info("MinIO bucket 已存在: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("MinIO bucket 初始化失败，请确保 MinIO 服务已启动: {}", endpoint, e);
            throw new RuntimeException("MinIO 服务连接失败，请检查 MinIO 是否已启动。" +
                    "启动命令: docker run -p 9000:9000 -p 9001:9001 --name minio " +
                    "-e \"MINIO_ROOT_USER=minioadmin\" -e \"MINIO_ROOT_PASSWORD=minioadmin\" minio/minio server /data", e);
        }
    }

    /**
     * 上传文件到 MinIO
     *
     * @param objectName  对象名称（文件路径）
     * @param inputStream 文件输入流
     * @param contentType 文件内容类型
     * @param fileSize    文件大小
     */
    public void uploadFile(String objectName, InputStream inputStream, String contentType, long fileSize) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, fileSize, -1)
                            .contentType(contentType)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("MinIO 文件上传失败: " + objectName, e);
        }
    }

    /**
     * 从 MinIO 获取文件流
     *
     * @param objectName 对象名称
     * @return 文件输入流
     */
    public InputStream downloadFile(String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("MinIO 文件下载失败: " + objectName, e);
        }
    }

    /**
     * 从 MinIO 删除文件
     *
     * @param objectName 对象名称
     */
    public void deleteFile(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("MinIO 文件删除失败: " + objectName, e);
        }
    }

    /**
     * 获取 bucket 名称
     */
    public String getBucketName() {
        return bucketName;
    }
}
