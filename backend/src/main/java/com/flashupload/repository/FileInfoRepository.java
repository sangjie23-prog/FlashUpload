package com.flashupload.repository;

import com.flashupload.entity.FileInfo;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 第三阶段先提供最小仓储能力，满足普通上传后的文件元数据保存。
 */
public interface FileInfoRepository extends JpaRepository<FileInfo, Long> {

    Optional<FileInfo> findByFileMd5(String fileMd5);

    /**
     * 第七阶段：分页查询文件列表，按创建时间倒序
     */
    Page<FileInfo> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
