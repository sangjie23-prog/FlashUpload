# FlashUpload

一个采用迭代方式开发的文件上传与秒传系统原型项目。

## 项目进度

- ✅ 阶段1：搭建 Spring Boot 后端项目
- ✅ 阶段2：实现普通上传和下载
- ✅ 阶段3：创建 MySQL 表并保存文件元数据
- ✅ 阶段4：实现 MD5 秒传检查
- ⏳ 阶段5：实现分片上传
- ⏳ 阶段6：实现分片合并
- ⏳ 阶段7：实现断点续传
- ⏳ 阶段8：搭建前端页面
- ⏳ 阶段9：接入上传进度条、秒传提示和文件列表
- ⏳ 阶段10：补充 README、接口文档和简历亮点

## 阶段4完成功能

- POST /api/files/check - 检查文件是否已存在，支持秒传和断点续传
- 根据文件 MD5 判断是否可秒传
- 返回已上传分片列表（用于后续断点续传）

## 技术栈

- 后端：Java 17 + Spring Boot 3 + Maven + H2/MySQL
- 存储：本地磁盘（后续预留 MinIO）
- 测试：JUnit 5

## 快速启动

```bash
cd backend
mvn spring-boot:run
```

## 测试示例

### 1. 健康检查
```bash
curl http://localhost:8080/health
```

### 2. 普通文件上传
```bash
curl -X POST http://localhost:8080/api/files/upload \
  -F "file=@test.txt"
```

### 3. MD5 秒传检查
```bash
curl -X POST http://localhost:8080/api/files/check \
  -H "Content-Type: application/json" \
  -d '{
    "fileName": "test.txt",
    "fileMd5": "abc123def456",
    "fileSize": 1024,
    "totalChunks": 1
  }'
```
