package com.flashupload.controller;

import com.flashupload.dto.ChunkUploadRequest;
import com.flashupload.dto.ChunkUploadResponse;
import com.flashupload.dto.FileCheckRequest;
import com.flashupload.dto.FileCheckResponse;
import com.flashupload.dto.FileUploadResponse;
import com.flashupload.dto.MergeRequest;
import com.flashupload.entity.FileInfo;
import com.flashupload.service.FileStorageService;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    /**
     * 第六阶段：合并分片为完整文件
     * 请求参数：
     * - fileMd5: 文件 MD5
     * - fileName: 原始文件名
     * - totalChunks: 总分片数
     * - fileSize: 文件大小
     * - contentType: 文件内容类型
     */
    @PostMapping("/merge")
    public FileInfo mergeChunks(@RequestBody MergeRequest request) throws IOException {
        return fileStorageService.mergeChunks(request);
    }

    /**
     * 第七阶段：查询文件列表，支持分页
     * 第九阶段：支持按文件名关键字搜索
     * 请求参数：
     * - page: 页码，从 0 开始，默认 0
     * - size: 每页大小，默认 10
     * - keyword: 文件名关键字，可选
     */
    @GetMapping
    public Page<FileInfo> listFiles(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        Pageable pageable = PageRequest.of(page, size);
        return fileStorageService.searchFiles(keyword, pageable);
    }

    /**
     * 第九阶段：删除文件
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteFile(@PathVariable Long id) {
        fileStorageService.deleteFile(id);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 200);
        body.put("message", "删除成功");
        return ResponseEntity.ok(body);
    }

    /**
     * 第七阶段：下载文件
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        return fileStorageService.downloadFile(id);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 400);
        body.put("message", exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
