CREATE TABLE IF NOT EXISTS file_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_md5 VARCHAR(32) NOT NULL UNIQUE COMMENT '文件MD5',
    file_size BIGINT NOT NULL COMMENT '文件大小，单位字节',
    content_type VARCHAR(255) DEFAULT NULL COMMENT '文件内容类型',
    storage_path VARCHAR(500) NOT NULL COMMENT '本地存储路径',
    status VARCHAR(32) NOT NULL COMMENT '上传状态',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='文件信息表';


CREATE TABLE IF NOT EXISTS file_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    file_md5 VARCHAR(32) NOT NULL COMMENT '文件MD5',
    chunk_index INT NOT NULL COMMENT '分片索引，从0开始',
    chunk_path VARCHAR(500) NOT NULL COMMENT '分片存储路径',
    total_chunks INT NOT NULL COMMENT '总分片数',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_file_md5 (file_md5),
    UNIQUE KEY uk_file_md5_chunk_index (file_md5, chunk_index)
) COMMENT='文件分片信息表';
