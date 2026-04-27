# FlashUpload

高性能文件上传系统，支持普通上传、MD5 秒传、分片上传、断点续传、并发上传、异步合并、MinIO 对象存储和文件预览。

## 1. 项目简介

FlashUpload 是一个前后端分离的文件上传管理系统，核心目标是实现大文件的高效上传与存储。系统通过 MD5 秒传避免重复上传，通过分片上传和断点续传提升大文件上传的可靠性，通过并发上传加速传输，通过异步合并避免接口阻塞，最终将文件存储到 MinIO 对象存储中。

## 2. 已实现功能

| 功能 | 说明 |
|------|------|
| 普通文件上传 | 拖拽或点击选择文件，上传到本地后转存 MinIO |
| MD5 秒传 | 计算文件 MD5，若已存在则直接返回，无需重复上传 |
| 分片上传 | 大文件按 1MB 分片逐个上传 |
| 断点续传 | 上传中断后可从已上传分片继续 |
| 分片合并 | 所有分片上传完成后合并为完整文件 |
| 文件列表 | 分页展示已上传文件，支持按文件名搜索 |
| 文件下载 | 从 MinIO 读取文件流供用户下载 |
| 文件删除 | 同时删除 MinIO 文件和数据库记录 |
| MinIO 对象存储 | 合并后的文件自动上传到 MinIO，清理本地临时文件 |
| 并发分片上传 | 前端默认 5 并发上传分片，单分片失败最多重试 3 次 |
| 异步合并大文件 | merge 接口只提交任务不阻塞，后端线程池异步合并 |
| 文件预览 | 支持图片、PDF、文本文件在线预览 |

## 3. 技术栈

### 后端
- Java 17
- Spring Boot 3.3.5
- Spring Data JPA
- MySQL 8.x（默认配置，也可用 H2 快速启动）
- MinIO 8.5.7
- Maven

### 前端
- Vue 3 (Composition API)
- Vite
- Element Plus
- Axios
- SparkMD5（文件 MD5 计算）

## 4. 项目目录结构

```
FlashUpload/
├── backend/                          # 后端项目
│   ├── pom.xml                       # Maven 依赖配置
│   └── src/
│       └── main/
│           ├── java/com/flashupload/
│           │   ├── FlashUploadApplication.java   # Spring Boot 启动类
│           │   ├── config/
│           │   │   ├── AsyncConfig.java          # 异步合并线程池配置
│           │   │   └── MinIOConfig.java          # MinIO 客户端配置
│           │   ├── controller/
│           │   │   ├── FileController.java       # 文件操作接口
│           │   │   └── HealthController.java     # 健康检查接口
│           │   ├── dto/
│           │   │   ├── ChunkUploadRequest.java   # 分片上传请求
│           │   │   ├── ChunkUploadResponse.java  # 分片上传响应
│           │   │   ├── FileCheckRequest.java     # 文件检查请求
│           │   │   ├── FileCheckResponse.java    # 文件检查响应
│           │   │   ├── FileUploadResponse.java   # 文件上传响应
│           │   │   └── MergeRequest.java         # 合并请求
│           │   ├── entity/
│           │   │   ├── FileInfo.java             # 文件元数据实体
│           │   │   └── FileChunk.java            # 分片信息实体
│           │   ├── repository/
│           │   │   ├── FileInfoRepository.java   # 文件元数据 DAO
│           │   │   └── FileChunkRepository.java  # 分片信息 DAO
│           │   ├── service/
│           │   │   ├── FileStorageService.java   # 核心业务逻辑
│           │   │   └── MinIOService.java         # MinIO 操作封装
│           │   └── util/
│           │       └── Md5Utils.java             # MD5 工具类
│           └── resources/
│               ├── application.yml               # 主配置文件
│               ├── application-local.yml         # 本地 MySQL 配置
│               └── schema.sql                    # 数据库初始化脚本
│
├── frontend/                         # 前端项目
│   ├── index.html                    # 入口 HTML
│   ├── vite.config.js                # Vite 配置
│   ├── package.json                  # npm 依赖
│   └── src/
│       ├── main.js                   # Vue 入口
│       ├── App.vue                   # 根组件
│       ├── api/
│       │   └── fileApi.js            # Axios 封装和 API 调用
│       └── components/
│           ├── FileUpload.vue        # 文件上传组件
│           └── FileList.vue          # 文件列表组件
│
└── .gitignore
```

## 5. 核心业务流程

### 5.1 普通上传流程

```
用户选择文件
  → 前端计算文件 MD5
  → POST /api/files/upload（multipart/form-data）
  → 后端接收文件，计算 MD5
  → 保存到本地临时目录
  → 写入 file_info 表（status=COMPLETED）
  → 返回文件信息
```

### 5.2 分片上传 + 秒传 + 断点续传流程

```
用户选择大文件
  → 前端按 1MB 分片计算 MD5
  → POST /api/files/check 检查文件是否存在
    → 已存在（status=COMPLETED）：秒传，直接返回
    → 上传中（status=UPLOADING）：返回已上传分片列表，断点续传
    → 不存在：开始上传
  → 并发上传分片（默认 5 并发）
    → POST /api/files/upload-chunk
    → 后端检查分片是否已存在（幂等）
    → 保存分片到本地 chunks/{fileMd5}/chunk-{index}
    → 写入 file_chunk 表
  → 所有分片上传完成
  → POST /api/files/merge 提交合并任务
  → 后端校验分片完整性
  → 更新 merge_status=MERGING，立即返回
  → 线程池异步执行合并
    → 按索引顺序合并分片
    → 上传到 MinIO
    → 更新 merge_status=SUCCESS，storagePath 指向 MinIO 对象名
    → 清理本地分片文件和分片记录
  → 前端轮询 GET /api/files/merge-status/{fileMd5}
    → SUCCESS：上传完成
    → FAILED：显示错误信息
```

### 5.3 异步合并流程

```
merge 接口
  → 校验分片完整性
  → 创建/更新 FileInfo（merge_status=MERGING）
  → 提交任务到 ThreadPoolExecutor
  → 立即返回 FileInfo

executeMergeTask（异步）
  → 按 chunkIndex 排序分片
  → 创建临时合并文件
  → 顺序写入所有分片
  → 上传到 MinIO（objectName = fileMd5 + "/" + storedFileName）
  → 更新 FileInfo（merge_status=SUCCESS, storagePath=minioObjectName）
  → 删除本地分片文件和数据库分片记录
  → 删除本地临时合并文件
  → 失败时更新 merge_status=FAILED, mergeError=错误信息
```

### 5.4 文件预览流程

```
用户点击预览按钮
  → 前端判断文件类型
    → 图片（image/*）：直接显示 img 标签
    → PDF（application/pdf）：使用 iframe 嵌入
    → 文本（text/*, json, xml）：请求文本内容显示
  → GET /api/files/{id}/preview
  → 后端从 MinIO 读取文件流
  → 设置正确的 Content-Type 返回
  → 不支持预览的类型，按钮禁用
```

## 6. 数据库表说明

### 6.1 file_info 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT (PK, AUTO_INCREMENT) | 主键 |
| file_name | VARCHAR(255) NOT NULL | 原始文件名 |
| file_md5 | VARCHAR(32) NOT NULL, UNIQUE | 文件 MD5 值 |
| file_size | BIGINT NOT NULL | 文件大小（字节） |
| content_type | VARCHAR(255) | MIME 类型 |
| storage_path | VARCHAR(500) NOT NULL | 存储路径（MinIO 对象名） |
| status | VARCHAR(32) NOT NULL | 状态：COMPLETED / UPLOADING |
| merge_status | VARCHAR(32) | 合并状态：MERGING / SUCCESS / FAILED |
| merge_error | VARCHAR(500) | 合并失败时的错误信息 |
| created_at | DATETIME NOT NULL | 创建时间 |
| updated_at | DATETIME NOT NULL | 更新时间 |

### 6.2 file_chunk 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT (PK, AUTO_INCREMENT) | 主键 |
| file_md5 | VARCHAR(32) NOT NULL | 所属文件的 MD5 |
| chunk_index | INT NOT NULL | 分片索引（从 0 开始） |
| chunk_path | VARCHAR(500) NOT NULL | 分片本地存储路径 |
| total_chunks | INT NOT NULL | 总分片数 |
| created_at | DATETIME NOT NULL | 创建时间 |

## 7. 后端核心类说明

### 7.1 FileController

文件操作 REST 控制器，提供以下接口：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/files/check | 检查文件是否存在（秒传/断点续传） |
| POST | /api/files/upload | 普通文件上传 |
| POST | /api/files/upload-chunk | 上传单个分片 |
| POST | /api/files/merge | 提交合并任务（异步） |
| GET | /api/files/merge-status/{fileMd5} | 查询合并状态 |
| GET | /api/files | 分页查询文件列表（支持 keyword 搜索） |
| DELETE | /api/files/{id} | 删除文件 |
| GET | /api/files/{id}/download | 下载文件 |
| GET | /api/files/{id}/preview | 预览文件 |

### 7.2 FileStorageService

核心业务逻辑类，包含：

- `upload()` - 普通文件上传
- `checkFile()` - 文件存在性检查（秒传/断点续传）
- `uploadChunk()` - 分片上传（幂等，支持断点续传）
- `mergeChunks()` - 提交异步合并任务
- `executeMergeTask()` - 异步执行合并（排序、合并、上传 MinIO、清理）
- `getMergeStatus()` - 查询合并状态
- `downloadFile()` - 从 MinIO 下载文件
- `previewFile()` - 预览文件（图片、PDF、文本）
- `deleteFile()` - 删除文件（MinIO + 数据库）
- `searchFiles()` - 分页搜索文件

### 7.3 MinIOService

MinIO 操作封装类：

- `uploadFile()` - 上传文件到 MinIO
- `downloadFile()` - 从 MinIO 下载文件流
- `deleteFile()` - 删除 MinIO 对象
- `initBucket()` - 初始化 bucket（@PostConstruct 自动执行）

### 7.4 AsyncConfig

异步合并线程池配置：

- 核心线程数：2
- 最大线程数：4
- 队列容量：100
- 拒绝策略：CallerRunsPolicy（调用者运行）

### 7.5 DTO 类

| 类 | 说明 |
|------|------|
| ChunkUploadRequest | 分片上传请求参数（fileMd5, chunkIndex, totalChunks） |
| ChunkUploadResponse | 分片上传响应（chunkIndex, uploadedChunks, isComplete） |
| FileCheckRequest | 文件检查请求（fileMd5, fileName, fileSize, totalChunks） |
| FileCheckResponse | 文件检查响应（isExist, fileId, status, uploadedChunks） |
| FileUploadResponse | 普通上传响应（完整 FileInfo 字段） |
| MergeRequest | 合并请求（fileMd5, fileName, totalChunks, fileSize, contentType） |

## 8. 前端核心页面和组件说明

### 8.1 FileUpload.vue

文件上传组件，功能包括：

- 拖拽/点击选择文件
- SparkMD5 计算文件 MD5（分块读取，显示进度）
- 调用 check 接口判断秒传/断点续传
- 并发上传分片（MAX_CONCURRENT = 5）
- 单分片失败重试（MAX_RETRY = 3）
- 上传暂停/继续
- 上传进度、速度、剩余时间显示
- 提交合并任务后轮询合并状态（每秒一次，最多 60 次）
- 分片大小：1MB（CHUNK_SIZE = 1 * 1024 * 1024）

### 8.2 FileList.vue

文件列表组件，功能包括：

- 分页展示文件（默认每页 10 条）
- 按文件名关键字搜索
- 预览按钮（图片/ PDF / 文本支持，其他类型禁用）
- 下载按钮
- 删除按钮（带确认弹窗）
- 预览对话框：
  - 图片：img 标签直接显示
  - PDF：iframe 嵌入显示
  - 文本：pre 标签格式化显示

## 9. 接口文档与 curl 示例

### 9.1 检查文件（秒传/断点续传）

```bash
curl.exe -X POST "http://localhost:8080/api/files/check" ^
  -H "Content-Type: application/json" ^
  -d "{\"fileMd5\":\"abc123\",\"fileName\":\"test.txt\",\"fileSize\":1024,\"totalChunks\":1}"
```

### 9.2 普通上传

```bash
curl.exe -X POST "http://localhost:8080/api/files/upload" ^
  -F "file=@test.txt"
```

### 9.3 上传分片

```bash
curl.exe -X POST "http://localhost:8080/api/files/upload-chunk" ^
  -F "file=@chunk0.bin" ^
  -F "fileMd5=abc123" ^
  -F "chunkIndex=0" ^
  -F "totalChunks=3"
```

### 9.4 提交合并任务

```bash
curl.exe -X POST "http://localhost:8080/api/files/merge" ^
  -H "Content-Type: application/json" ^
  -d "{\"fileMd5\":\"abc123\",\"fileName\":\"test.txt\",\"totalChunks\":3,\"fileSize\":3072,\"contentType\":\"text/plain\"}"
```

### 9.5 查询合并状态

```bash
curl.exe "http://localhost:8080/api/files/merge-status/abc123"
```

### 9.6 文件列表（分页 + 搜索）

```bash
curl.exe "http://localhost:8080/api/files?page=0&size=10&keyword=test"
```

### 9.7 下载文件

```bash
curl.exe -O -J "http://localhost:8080/api/files/1/download"
```

### 9.8 预览文件

```bash
curl.exe "http://localhost:8080/api/files/1/preview"
```

### 9.9 删除文件

```bash
curl.exe -X DELETE "http://localhost:8080/api/files/1"
```

## 10. 本地启动步骤

### 10.1 环境要求

- JDK 17+
- Maven 3.6+
- Node.js 16+
- MySQL 8.x（可选，默认使用 H2 内存数据库）
- MinIO（文件存储必需）

### 10.2 启动 MinIO

使用 Docker 启动：

```bash
docker run -d ^
  -p 9000:9000 ^
  -p 9001:9001 ^
  --name minio ^
  -e "MINIO_ROOT_USER=minioadmin" ^
  -e "MINIO_ROOT_PASSWORD=minioadmin" ^
  minio/minio server /data --console-address ":9001"
```

启动后访问 http://localhost:9001 查看 MinIO 控制台。

### 10.3 启动 MySQL（可选）

```bash
docker run -d ^
  -p 3306:3306 ^
  --name mysql ^
  -e MYSQL_ROOT_PASSWORD=123456 ^
  -e MYSQL_DATABASE=flash_upload ^
  mysql:8
```

如果使用 MySQL，修改 `application.yml` 中的数据库配置，或启动时指定 profile：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 10.4 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认启动在 http://localhost:8080。

### 10.5 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认启动在 http://localhost:5173。

## 11. 测试指南

### 11.1 普通上传测试

```bash
curl.exe -X POST "http://localhost:8080/api/files/upload" ^
  -F "file=@test.txt"
```

预期返回文件信息，status 为 COMPLETED。

### 11.2 秒传测试

上传同一文件两次，第二次调用 check 接口：

```bash
curl.exe -X POST "http://localhost:8080/api/files/check" ^
  -H "Content-Type: application/json" ^
  -d "{\"fileMd5\":\"<文件MD5>\",\"fileName\":\"test.txt\",\"fileSize\":132,\"totalChunks\":1}"
```

预期返回 isExist=true。

### 11.3 分片上传测试

```bash
# 上传分片 0
curl.exe -X POST "http://localhost:8080/api/files/upload-chunk" ^
  -F "file=@chunk0.bin" ^
  -F "fileMd5=test123" ^
  -F "chunkIndex=0" ^
  -F "totalChunks=2"

# 上传分片 1
curl.exe -X POST "http://localhost:8080/api/files/upload-chunk" ^
  -F "file=@chunk1.bin" ^
  -F "fileMd5=test123" ^
  -F "chunkIndex=1" ^
  -F "totalChunks=2"

# 提交合并
curl.exe -X POST "http://localhost:8080/api/files/merge" ^
  -H "Content-Type: application/json" ^
  -d "{\"fileMd5\":\"test123\",\"fileName\":\"test.bin\",\"totalChunks\":2,\"fileSize\":2048,\"contentType\":\"application/octet-stream\"}"

# 查询合并状态
curl.exe "http://localhost:8080/api/files/merge-status/test123"
```

### 11.4 文件预览测试

上传一个文本文件后：

```bash
curl.exe "http://localhost:8080/api/files/<文件ID>/preview"
```

预期返回文件内容。

### 11.5 前端完整测试

1. 打开 http://localhost:5173
2. 拖拽一个文件到上传区域
3. 观察 MD5 计算进度
4. 点击开始上传，观察分片上传进度
5. 上传完成后在文件列表查看文件
6. 点击预览按钮（如果是图片/ PDF / 文本）
7. 点击下载按钮
8. 点击删除按钮

## 12. 常见问题

### Q1: 启动后端报 MinIO 连接失败

确保 MinIO 已启动且配置正确：

```bash
docker ps | grep minio
```

检查 `application.yml` 中 minio.endpoint 是否为 http://localhost:9000。

### Q2: 启动后端报 MySQL 连接失败

默认配置使用 MySQL。如果未安装 MySQL，可以临时改用 H2 内存数据库：

修改 `application.yml` 中的 datasource 配置为 H2：

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:flash_upload
    driver-class-name: org.h2.Driver
```

### Q3: 分片上传后合并一直显示 MERGING

检查后端日志，查看异步合并任务是否执行成功。如果合并失败，merge_status 会变为 FAILED，merge_error 会记录错误原因。

### Q4: 文件预览显示乱码

确保文件 content_type 正确。文本文件预览使用 UTF-8 编码，如果文件是其他编码可能显示乱码。

### Q5: 前端跨域问题

开发模式下 Vite 已配置代理到后端 8080 端口。如果修改了端口，需要同步修改 `vite.config.js` 中的 proxy 配置。

## 13. 当前限制

1. **单文件大小限制**：默认 100MB（spring.servlet.multipart.max-file-size），可在配置中调整
2. **并发上传数**：前端固定 5 并发，不可动态调整
3. **分片大小**：固定 1MB，不可配置
4. **线程池配置**：异步合并线程池参数硬编码在 AsyncConfig 中
5. **MinIO 配置**：仅支持单 endpoint，不支持多节点集群
6. **预览支持**：仅支持图片、PDF、文本，不支持 Office 文档、视频等
7. **无用户系统**：所有文件共享同一命名空间，无权限控制
8. **无文件加密**：MinIO 存储未启用服务端加密
9. **错误处理**：部分异常仅打印到 stderr，未统一异常处理机制
10. **无监控指标**：缺少上传成功率、平均速度等监控数据

## 14. 后续优化建议

### 性能优化
- 分片大小可配置（小文件用小分片，大文件用大分片）
- 并发数可配置（根据网络状况动态调整）
- 使用 Web Worker 计算 MD5，避免阻塞主线程
- 合并任务使用消息队列（RabbitMQ / Kafka）替代内存线程池
- 引入 Redis 缓存文件元数据，减少数据库查询

### 功能增强
- 用户认证和权限控制（JWT + Spring Security）
- 文件夹/目录结构支持
- 批量上传和批量操作
- 更多预览格式（Office 文档、视频、音频）
- 文件分享和链接功能
- 上传进度 WebSocket 实时推送
- 文件版本管理

### 运维优化
- 统一异常处理和全局错误码
- 结构化日志（JSON 格式）
- 健康检查增强（MinIO 连接状态、数据库连接池状态）
- Prometheus 指标暴露
- Docker Compose 一键启动
- CI/CD 流水线

### 代码质量
- 单元测试覆盖核心业务逻辑
- 集成测试覆盖完整上传流程
- 代码规范检查（Checkstyle / SpotBugs）
- API 文档（SpringDoc / Swagger）

## 15. 维护者快速上手清单

- [ ] 克隆仓库：`git clone https://github.com/sangjie23-prog/FlashUpload.git`
- [ ] 安装 JDK 17、Maven、Node.js
- [ ] 启动 MinIO（Docker 或本地）
- [ ] 启动 MySQL（或改用 H2）
- [ ] 修改 `application.yml` 中的数据库和 MinIO 配置
- [ ] 启动后端：`cd backend && mvn spring-boot:run`
- [ ] 启动前端：`cd frontend && npm install && npm run dev`
- [ ] 浏览器访问 http://localhost:5173 测试上传功能
- [ ] 阅读 `FileStorageService.java` 理解核心业务逻辑
- [ ] 阅读 `FileUpload.vue` 理解前端上传流程
- [ ] 使用 curl 示例测试各个接口
- [ ] 查看 MinIO 控制台（http://localhost:9001）确认文件存储
