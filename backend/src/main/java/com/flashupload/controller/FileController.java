package com.flashupload.controller;

import com.flashupload.dto.ChunkUploadRequest;
import com.flashupload.dto.ChunkUploadResponse;
import com.flashupload.dto.FileCheckRequest;
import com.flashupload.dto.FileCheckResponse;
import com.flashupload.dto.FileUploadResponse;
import com.flashupload.service.FileStorageService;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 第二阶段只提供普通文件上传接口，保证 MVP 主流程先跑通。
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * 第四阶段：检查文件是否已存在，支持秒传和断点续传
     */
    @PostMapping("/check")
    public FileCheckResponse checkFile(@RequestBody FileCheckRequest request) {
        return fileStorageService.checkFile(request);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileUploadResponse upload(@RequestPart("file") MultipartFile file) throws IOException {
        return fileStorageService.upload(file);
    }

    /**
     * 第五阶段：上传单个分片，支持断点续传
     * 请求参数：
     * - file: 分片文件
     * - fileMd5: 文件 MD5
     * - chunkIndex: 分片索引
     * - totalChunks: 总分片数
     */
    @PostMapping(value = "/upload-chunk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChunkUploadResponse uploadChunk(
            @RequestPart("file") MultipartFile chunkFile,
            @RequestParam("fileMd5") String fileMd5,
            @RequestParam("chunkIndex") Integer chunkIndex,
            @RequestParam("totalChunks") Integer totalChunks) throws IOException {
        ChunkUploadRequest request = new ChunkUploadRequest();
        request.setFileMd5(fileMd5);
        request.setChunkIndex(chunkIndex);
        request.setTotalChunks(totalChunks);
        return fileStorageService.uploadChunk(chunkFile, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 400);
        body.put("message", exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
