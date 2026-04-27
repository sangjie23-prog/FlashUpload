package com.flashupload.service;

import com.flashupload.dto.ChunkUploadRequest;
import com.flashupload.dto.ChunkUploadResponse;
import com.flashupload.dto.FileCheckRequest;
import com.flashupload.dto.FileCheckResponse;
import com.flashupload.dto.FileUploadResponse;
import com.flashupload.entity.FileChunk;
import com.flashupload.entity.FileInfo;
import com.flashupload.repository.FileChunkRepository;
import com.flashupload.repository.FileInfoRepository;
import com.flashupload.util.Md5Utils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 第三阶段在本地落盘基础上补充元数据入库，先保证普通上传链路完整闭环。
 */
@Service
public class FileStorageService {

    private static final DateTimeFormatter DIRECTORY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_UPLOADING = "UPLOADING";

    @Value("${flash-upload.storage.path}")
    private String storagePath;

    private final FileInfoRepository fileInfoRepository;
    private final FileChunkRepository fileChunkRepository;

    public FileStorageService(FileInfoRepository fileInfoRepository, FileChunkRepository fileChunkRepository) {
        this.fileInfoRepository = fileInfoRepository;
        this.fileChunkRepository = fileChunkRepository;
    }

    @Transactional
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

        try {
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileName(originalFileName);
            fileInfo.setFileMd5(fileMd5);
            fileInfo.setFileSize(file.getSize());
            fileInfo.setContentType(file.getContentType());
            fileInfo.setStoragePath(targetFilePath.toString());
            fileInfo.setStatus(STATUS_COMPLETED);

            FileInfo savedFileInfo = fileInfoRepository.save(fileInfo);

            FileUploadResponse response = new FileUploadResponse();
            response.setId(savedFileInfo.getId());
            response.setFileName(savedFileInfo.getFileName());
            response.setFileMd5(savedFileInfo.getFileMd5());
            response.setFileSize(savedFileInfo.getFileSize());
            response.setContentType(savedFileInfo.getContentType());
            response.setStoragePath(savedFileInfo.getStoragePath());
            response.setStatus(savedFileInfo.getStatus());
            response.setUploadedAt(savedFileInfo.getCreatedAt());
            return response;
        } catch (DataIntegrityViolationException exception) {
            Files.deleteIfExists(targetFilePath);
            throw new IllegalArgumentException("文件元数据保存失败，可能存在重复 MD5 记录");
        } catch (RuntimeException exception) {
            Files.deleteIfExists(targetFilePath);
            throw exception;
        }
    }

    /**
     * 第五阶段：上传单个分片
     * 1. 检查分片是否已上传（断点续传）
     * 2. 保存分片到本地磁盘
     * 3. 记录分片信息到数据库
     * 4. 返回已上传分片列表
     */
    @Transactional
    public ChunkUploadResponse uploadChunk(MultipartFile chunkFile, ChunkUploadRequest request) throws IOException {
        if (chunkFile == null || chunkFile.isEmpty()) {
            throw new IllegalArgumentException("分片文件不能为空");
        }
        
        if (request.getFileMd5() == null || request.getChunkIndex() == null || request.getTotalChunks() == null) {
            throw new IllegalArgumentException("分片元数据不完整");
        }
        
        // 检查分片是否已上传（断点续传场景）
        FileChunk existingChunk = fileChunkRepository.findByFileMd5AndChunkIndex(
            request.getFileMd5(), request.getChunkIndex()
        );
        
        if (existingChunk != null) {
            // 分片已存在，直接返回已上传列表
            return buildChunkUploadResponse(request.getFileMd5(), request.getChunkIndex());
        }
        
        // 创建分片存储目录
        Path chunkDirectory = Path.of(storagePath, "chunks", request.getFileMd5());
        Files.createDirectories(chunkDirectory);
        
        // 保存分片文件
        String chunkFileName = "chunk-" + request.getChunkIndex();
        Path chunkFilePath = chunkDirectory.resolve(chunkFileName);
        
        try (InputStream inputStream = chunkFile.getInputStream()) {
            Files.copy(inputStream, chunkFilePath, StandardCopyOption.REPLACE_EXISTING);
        }
        
        // 保存分片信息到数据库
        FileChunk fileChunk = new FileChunk();
        fileChunk.setFileMd5(request.getFileMd5());
        fileChunk.setChunkIndex(request.getChunkIndex());
        fileChunk.setChunkPath(chunkFilePath.toString());
        fileChunk.setTotalChunks(request.getTotalChunks());
        
        fileChunkRepository.save(fileChunk);
        
        // 如果文件元数据不存在，创建 UPLOADING 状态的记录
        Optional<FileInfo> fileInfoOpt = fileInfoRepository.findByFileMd5(request.getFileMd5());
        if (fileInfoOpt.isEmpty()) {
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileName("unknown");
            fileInfo.setFileMd5(request.getFileMd5());
            fileInfo.setFileSize(0L);
            fileInfo.setContentType(null);
            fileInfo.setStoragePath("");
            fileInfo.setStatus(STATUS_UPLOADING);
            fileInfoRepository.save(fileInfo);
        }
        
        return buildChunkUploadResponse(request.getFileMd5(), request.getChunkIndex());
    }
    
    /**
     * 构建分片上传响应
     */
    private ChunkUploadResponse buildChunkUploadResponse(String fileMd5, Integer currentChunkIndex) {
        List<FileChunk> chunks = fileChunkRepository.findByFileMd5(fileMd5);
        List<Integer> uploadedChunks = new ArrayList<>();
        for (FileChunk chunk : chunks) {
            uploadedChunks.add(chunk.getChunkIndex());
        }
        
        ChunkUploadResponse response = new ChunkUploadResponse();
        response.setChunkIndex(currentChunkIndex);
        response.setUploadedChunks(uploadedChunks);
        response.setIsComplete(false);
        return response;
    }

    /**
     * 第四阶段：检查文件是否已存在，支持秒传和断点续传
     * 根据文件 MD5 判断：
     * 1. 如果文件已完成上传，返回秒传成功
     * 2. 如果文件正在上传中，返回已上传的分片列表
     * 3. 如果文件不存在，返回空列表
     */
    public FileCheckResponse checkFile(FileCheckRequest request) {
        FileCheckResponse response = new FileCheckResponse();
        
        // 根据 MD5 查询文件是否存在
        Optional<FileInfo> fileInfoOpt = fileInfoRepository.findByFileMd5(request.getFileMd5());
        
        if (fileInfoOpt.isPresent()) {
            FileInfo fileInfo = fileInfoOpt.get();
            
            // 文件已完成上传，可以秒传
            if (STATUS_COMPLETED.equals(fileInfo.getStatus())) {
                response.setIsExist(true);
                response.setFileId(fileInfo.getId());
                response.setStatus(STATUS_COMPLETED);
                response.setUploadedChunks(new ArrayList<>());
                return response;
            }
            
            // 文件正在上传中，返回已上传的分片列表（断点续传）
            if (STATUS_UPLOADING.equals(fileInfo.getStatus())) {
                List<FileChunk> chunks = fileChunkRepository.findByFileMd5(request.getFileMd5());
                List<Integer> uploadedChunks = new ArrayList<>();
                for (FileChunk chunk : chunks) {
                    uploadedChunks.add(chunk.getChunkIndex());
                }
                
                response.setIsExist(false);
                response.setFileId(fileInfo.getId());
                response.setStatus(STATUS_UPLOADING);
                response.setUploadedChunks(uploadedChunks);
                return response;
            }
        }
        
        // 文件不存在
        response.setIsExist(false);
        response.setFileId(null);
        response.setStatus(null);
        response.setUploadedChunks(new ArrayList<>());
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
