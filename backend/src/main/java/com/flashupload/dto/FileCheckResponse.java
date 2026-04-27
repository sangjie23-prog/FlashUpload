package com.flashupload.dto;

import java.util.List;

/**
 * 第四阶段：秒传检查响应 DTO
 * 返回文件是否已存在以及已上传的分片列表（用于断点续传）
 */
public class FileCheckResponse {

    /** 文件是否已存在（秒传） */
    private Boolean isExist;

    /** 文件 ID（如果已存在） */
    private Long fileId;

    /** 已上传的分片索引列表 */
    private List<Integer> uploadedChunks;

    /** 文件状态：COMPLETED/UPLOADING */
    private String status;

    public Boolean getIsExist() {
        return isExist;
    }

    public void setIsExist(Boolean isExist) {
        this.isExist = isExist;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public List<Integer> getUploadedChunks() {
        return uploadedChunks;
    }

    public void setUploadedChunks(List<Integer> uploadedChunks) {
        this.uploadedChunks = uploadedChunks;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
