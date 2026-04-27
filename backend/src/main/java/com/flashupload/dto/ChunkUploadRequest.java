package com.flashupload.dto;

/**
 * 第五阶段：分片上传请求 DTO
 * 前端上传分片时携带的元数据信息
 */
public class ChunkUploadRequest {

    /** 文件 MD5 值 */
    private String fileMd5;

    /** 分片索引，从 0 开始 */
    private Integer chunkIndex;

    /** 总分片数 */
    private Integer totalChunks;

    public String getFileMd5() {
        return fileMd5;
    }

    public void setFileMd5(String fileMd5) {
        this.fileMd5 = fileMd5;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public Integer getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(Integer totalChunks) {
        this.totalChunks = totalChunks;
    }
}
