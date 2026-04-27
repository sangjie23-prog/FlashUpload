package com.flashupload.dto;

/**
 * 第六阶段：分片合并请求 DTO
 * 前端在所有分片上传完成后调用此接口合并文件
 */
public class MergeRequest {

    /** 文件 MD5 值 */
    private String fileMd5;

    /** 原始文件名 */
    private String fileName;

    /** 总分片数 */
    private Integer totalChunks;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文件内容类型 */
    private String contentType;

    public String getFileMd5() {
        return fileMd5;
    }

    public void setFileMd5(String fileMd5) {
        this.fileMd5 = fileMd5;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(Integer totalChunks) {
        this.totalChunks = totalChunks;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
