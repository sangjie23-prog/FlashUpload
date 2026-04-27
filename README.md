# FlashUpload - 文件上传与秒传系统

一个采用迭代方式开发的文件上传与秒传系统，支持普通上传、分片上传、断点续传、MD5 秒传、上传进度展示和文件下载。

## 📋 项目进度

- ✅ 阶段1：搭建 Spring Boot 后端项目
- ✅ 阶段2：实现普通上传和下载
- ✅ 阶段3：创建 MySQL 表并保存文件元数据
- ✅ 阶段4：实现 MD5 秒传检查
- ✅ 阶段5：实现分片上传
- ✅ 阶段6：实现分片合并
- ✅ 阶段7：实现断点续传
- ✅ 阶段8：搭建 Vue 3 前端页面
- ✅ 阶段9：完善前端交互体验（进度条、搜索、删除）
- ✅ 阶段10：补充 README、接口文档和简历亮点

## 🚀 核心功能

| 功能 | 说明 |
|------|------|
| 普通文件上传 | 支持单文件直接上传 |
| MD5 秒传 | 根据文件 MD5 判断是否已存在，避免重复上传 |
| 分片上传 | 将大文件分割为多个分片并行上传 |
| 断点续传 | 中断后可从上次位置继续上传 |
| 上传进度 | 实时显示上传进度、速度、剩余时间 |
| 文件列表 | 分页展示已上传文件，支持搜索过滤 |
| 文件下载 | 支持已上传文件下载 |
| 文件删除 | 支持删除已上传文件 |

## 🛠 技术栈

### 后端
- **框架**：Java 17 + Spring Boot 3.3.5
- **构建工具**：Maven
- **数据库**：MySQL 8.0（开发环境使用 H2）
- **ORM**：Spring Data JPA / Hibernate
- **存储**：本地磁盘（预留 MinIO 扩展）
- **测试**：JUnit 5 + MockMvc

### 前端
- **框架**：Vue 3 + Composition API
- **UI 组件库**：Element Plus
- **构建工具**：Vite 5
- **HTTP 客户端**：Axios
- **MD5 计算**：Spark-MD5

## 📁 项目结构

```
FlashUpload/
├── backend/                          # 后端项目
│   ├── src/main/java/com/flashupload/
│   │   ├── controller/               # REST 控制器
│   │   │   ├── FileController.java   # 文件操作接口
│   │   │   └── HealthController.java # 健康检查接口
│   │   ├── service/                  # 业务逻辑层
│   │   │   └── FileStorageService.java
│   │   ├── repository/               # 数据访问层
│   │   │   ├── FileInfoRepository.java
│   │   │   └── FileChunkRepository.java
│   │   ├── entity/                   # JPA 实体类
│   │   │   ├── FileInfo.java         # 文件元数据
│   │   │   └── FileChunk.java        # 分片元数据
│   │   ├── dto/                      # 数据传输对象
│   │   │   ├── FileCheckRequest.java
│   │   │   ├── FileCheckResponse.java
│   │   │   ├── FileUploadResponse.java
│   │   │   ├── ChunkUploadRequest.java
│   │   │   ├── ChunkUploadResponse.java
│   │   │   └── MergeRequest.java
│   │   └── util/                     # 工具类
│   │       └── Md5Utils.java
│   ├── src/main/resources/
│   │   ├── application.yml           # 应用配置
│   │   └── schema.sql                # 数据库建表脚本
│   └── pom.xml                       # Maven 依赖配置
├── frontend/                         # 前端项目
│   ├── src/
│   │   ├── api/                      # API 服务封装
│   │   │   └── fileApi.js
│   │   ├── components/               # Vue 组件
│   │   │   ├── FileUpload.vue        # 文件上传组件
│   │   │   └── FileList.vue          # 文件列表组件
│   │   ├── App.vue                   # 主应用组件
│   │   └── main.js                   # 应用入口
│   ├── package.json                  # 前端依赖配置
│   └── vite.config.js                # Vite 配置
└── README.md                         # 项目文档
```

## 🏃 快速启动

### 环境要求
- Java 17+
- Node.js 18+
- MySQL 8.0+（可选，默认使用 H2 内存数据库）

### 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端服务启动在：http://localhost:8080

### 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端服务启动在：http://localhost:3000

### 访问应用

打开浏览器访问：http://localhost:3000

## 📖 接口文档

### 1. 健康检查

**接口**：`GET /api/health`

**响应**：
```json
{
  "code": 200,
  "message": "FlashUpload backend is running",
  "timestamp": "2026-04-27T22:00:00"
}
```

### 2. 检查文件（秒传/断点续传）

**接口**：`POST /api/files/check`

**请求体**：
```json
{
  "fileName": "test.pdf",
  "fileMd5": "abc123def456...",
  "fileSize": 1048576,
  "totalChunks": 10
}
```

**响应**：
```json
{
  "isExist": false,
  "fileId": 1,
  "status": "UPLOADING",
  "uploadedChunks": [0, 1, 2, 5]
}
```

**说明**：
- `isExist: true` 表示文件已存在，可秒传
- `status: "UPLOADING"` 表示文件正在上传中
- `uploadedChunks` 返回已上传的分片索引列表

### 3. 普通文件上传

**接口**：`POST /api/files/upload`

**请求**：`multipart/form-data`
- `file`: 文件

**响应**：
```json
{
  "id": 1,
  "fileName": "test.pdf",
  "fileMd5": "abc123...",
  "fileSize": 1048576,
  "contentType": "application/pdf",
  "storagePath": "/path/to/file",
  "status": "COMPLETED",
  "uploadedAt": "2026-04-27T22:00:00"
}
```

### 4. 上传分片

**接口**：`POST /api/files/upload-chunk`

**请求**：`multipart/form-data`
- `file`: 分片文件
- `fileMd5`: 文件 MD5
- `chunkIndex`: 分片索引（从 0 开始）
- `totalChunks`: 总分片数

**响应**：
```json
{
  "chunkIndex": 0,
  "uploadedChunks": [0, 1, 2],
  "isComplete": false
}
```

### 5. 合并分片

**接口**：`POST /api/files/merge`

**请求体**：
```json
{
  "fileMd5": "abc123...",
  "fileName": "test.pdf",
  "totalChunks": 10,
  "fileSize": 1048576,
  "contentType": "application/pdf"
}
```

**响应**：返回文件元数据（FileInfo）

### 6. 查询文件列表

**接口**：`GET /api/files`

**查询参数**：
- `page`: 页码，从 0 开始，默认 0
- `size`: 每页大小，默认 10
- `keyword`: 文件名搜索关键字（可选）

**响应**：
```json
{
  "content": [...],
  "totalElements": 100,
  "totalPages": 10,
  "number": 0,
  "size": 10
}
```

### 7. 下载文件

**接口**：`GET /api/files/{id}/download`

**响应**：文件流（attachment 下载）

### 8. 删除文件

**接口**：`DELETE /api/files/{id}`

**响应**：
```json
{
  "code": 200,
  "message": "删除成功"
}
```

## 💡 核心设计

### MD5 秒传原理
1. 前端使用 Spark-MD5 计算文件 MD5
2. 调用 `/api/files/check` 接口检查文件是否存在
3. 如果文件已存在（状态为 COMPLETED），直接返回秒传成功
4. 如果文件正在上传中，返回已上传分片列表，支持断点续传

### 分片上传流程
1. 前端使用 `Blob.slice()` 将文件分割为多个分片（默认 1MB/分片）
2. 逐个上传分片到 `/api/files/upload-chunk`
3. 后端保存分片到本地磁盘，并记录到数据库
4. 所有分片上传完成后，调用 `/api/files/merge` 合并分片
5. 后端按分片索引顺序合并文件，更新状态为 COMPLETED
6. 清理临时分片文件和数据库记录

### 断点续传机制
1. 上传前调用 `/api/files/check` 获取已上传分片列表
2. 前端跳过已上传的分片，只上传缺失的分片
3. 支持暂停/继续上传功能

### 数据库表设计

#### file_info 表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| file_name | VARCHAR(255) | 原始文件名 |
| file_md5 | VARCHAR(32) | 文件 MD5，唯一索引 |
| file_size | BIGINT | 文件大小（字节） |
| content_type | VARCHAR(255) | 文件 MIME 类型 |
| storage_path | VARCHAR(500) | 存储路径 |
| status | VARCHAR(32) | 状态：COMPLETED / UPLOADING |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

#### file_chunk 表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| file_md5 | VARCHAR(32) | 文件 MD5 |
| chunk_index | INT | 分片索引，从 0 开始 |
| chunk_path | VARCHAR(500) | 分片存储路径 |
| total_chunks | INT | 总分片数 |
| created_at | DATETIME | 创建时间 |

## 🎯 简历亮点

### 技术亮点
1. **分片上传 + 断点续传**：实现了大文件分片上传和断点续传功能，支持网络中断后从上次位置继续上传，提升用户体验
2. **MD5 秒传**：通过文件 MD5 去重，避免重复上传相同文件，节省存储空间和带宽
3. **前后端分离架构**：采用 Vue 3 + Spring Boot 前后端分离架构，前端使用 Vite 构建，支持热更新
4. **RESTful API 设计**：遵循 RESTful 规范，接口设计清晰，支持分页、搜索、过滤等功能
5. **JPA + Hibernate**：使用 Spring Data JPA 简化数据库操作，支持分页查询和条件搜索
6. **本地存储 + MinIO 预留**：MVP 阶段使用本地磁盘存储，架构设计预留 MinIO 对象存储扩展能力

### 项目亮点
1. **迭代式开发**：采用 MVP 迭代开发模式，每个阶段都可独立运行和测试
2. **完整功能闭环**：从文件上传、秒传检查、分片上传、断点续传到文件列表、下载、删除，形成完整功能闭环
3. **用户体验优化**：前端实现上传进度条、速度显示、剩余时间估算、暂停/继续等交互功能
4. **代码规范**：遵循 Java 编码规范，添加完整中文注释，使用 DTO 模式分离数据传输

### 可扩展方向
1. **MinIO 对象存储**：将本地存储替换为 MinIO，支持分布式存储
2. **并发上传**：支持多分片并发上传，提升上传速度
3. **文件预览**：支持图片、PDF、视频等文件在线预览
4. **权限管理**：添加用户认证和文件权限控制
5. **文件分享**：支持生成分享链接，设置有效期和下载次数限制

## 📝 开发日志

| 阶段 | 日期 | 内容 |
|------|------|------|
| 阶段1 | 2026-04-27 | 搭建 Spring Boot 后端项目，实现健康检查接口 |
| 阶段2 | 2026-04-27 | 实现普通文件上传和下载功能 |
| 阶段3 | 2026-04-27 | 创建 MySQL 表结构，保存文件元数据 |
| 阶段4 | 2026-04-27 | 实现 MD5 秒传检查接口 |
| 阶段5 | 2026-04-27 | 实现分片上传功能 |
| 阶段6 | 2026-04-27 | 实现分片合并功能 |
| 阶段7 | 2026-04-27 | 实现断点续传、文件列表和下载功能 |
| 阶段8 | 2026-04-27 | 搭建 Vue 3 前端项目，实现文件上传和列表组件 |
| 阶段9 | 2026-04-27 | 完善前端交互体验，添加进度条、搜索、删除功能 |
| 阶段10 | 2026-04-27 | 补充 README、接口文档和简历亮点 |

## 📄 License

MIT License
