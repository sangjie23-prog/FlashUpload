package com.flashupload.service;

import com.flashupload.dto.FileUploadResponse;
import com.flashupload.util.Md5Utils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 第二阶段先专注于普通上传，把文件保存到本地磁盘并返回最基础元数据。
 */
@Service
public class FileStorageService {

    private static final DateTimeFormatter DIRECTORY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Value("${flash-upload.storage.path}")
    private String storagePath;

    public FileUploadResponse upload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String originalFileName = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "unknown"));
        String safeFileName = buildSafeFileName(originalFileName);
        String fileMd5;

        try (InputStream inputStream = file.getInputStream()) {
            fileMd5 = Md5Utils.md5Hex(inputStream);
        }

        Path targetDirectory = Path.of(storagePath, LocalDateTime.now().format(DIRECTORY_FORMATTER));
        Files.createDirectories(targetDirectory);

        String storedFileName = UUID.randomUUID() + "-" + safeFileName;
        Path targetFilePath = targetDirectory.resolve(storedFileName);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
        }

        FileUploadResponse response = new FileUploadResponse();
        response.setFileName(originalFileName);
        response.setFileMd5(fileMd5);
        response.setFileSize(file.getSize());
        response.setContentType(file.getContentType());
        response.setStoragePath(targetFilePath.toString());
        response.setUploadedAt(LocalDateTime.now());
        return response;
    }

    /**
     * 先做最小化文件名清洗，避免路径穿越和明显非法的文件名输入。
     */
    private String buildSafeFileName(String originalFileName) {
        String fileName = originalFileName.replace("\\", "_").replace("/", "_");
        if (!StringUtils.hasText(fileName)) {
            return "unnamed-file";
        }
        return fileName;
    }
}
