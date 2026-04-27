package com.flashupload.dto;

/**
 * 第四阶段：秒传检查请求 DTO
 * 前端在上传前调用此接口，传入文件基本信息检查是否已存在
 */
public class FileCheckRequest {

    /** 原始文件名 */
    private String fileName;

    /** 文件 MD5 值 */
    private String fileMd5;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 总分片数（普通上传时为 1） */
    private Integer totalChunks;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileMd5() {
        return fileMd5;
    }

    public void setFileMd5(String fileMd5) {
        this.fileMd5 = fileMd5;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Integer getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(Integer totalChunks) {
        this.totalChunks = totalChunks;
    }
}
