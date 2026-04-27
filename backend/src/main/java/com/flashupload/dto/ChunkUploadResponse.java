package com.flashupload.dto;

import java.util.List;

/**
 * 第五阶段：分片上传响应 DTO
 * 返回分片上传结果和已上传分片列表
 */
public class ChunkUploadResponse {

    /** 当前分片索引 */
    private Integer chunkIndex;

    /** 已上传的分片索引列表 */
    private List<Integer> uploadedChunks;

    /** 是否所有分片都已上传完成 */
    private Boolean isComplete;

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public List<Integer> getUploadedChunks() {
        return uploadedChunks;
    }

    public void setUploadedChunks(List<Integer> uploadedChunks) {
        this.uploadedChunks = uploadedChunks;
    }

    public Boolean getIsComplete() {
        return isComplete;
    }

    public void setIsComplete(Boolean isComplete) {
        this.isComplete = isComplete;
    }
}
