package com.flashupload.repository;

import com.flashupload.entity.FileChunk;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 第四阶段：分片信息仓储
 * 提供按文件 MD5 查询已上传分片的能力
 */
public interface FileChunkRepository extends JpaRepository<FileChunk, Long> {

    /**
     * 根据文件 MD5 查询所有已上传的分片
     */
    List<FileChunk> findByFileMd5(String fileMd5);

    /**
     * 根据文件 MD5 和分片索引查询（用于判断分片是否已上传）
     */
    FileChunk findByFileMd5AndChunkIndex(String fileMd5, Integer chunkIndex);
}
