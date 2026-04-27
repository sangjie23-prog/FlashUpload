<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>文件上传</span>
        <el-tag v-if="uploadStatus === 'uploading'" type="primary">上传中</el-tag>
        <el-tag v-else-if="uploadStatus === 'paused'" type="warning">已暂停</el-tag>
        <el-tag v-else-if="uploadStatus === 'completed'" type="success">已完成</el-tag>
        <el-tag v-else-if="uploadStatus === 'exist'" type="success">秒传成功</el-tag>
      </div>
    </template>

    <el-upload
      drag
      :auto-upload="false"
      :limit="1"
      :on-change="handleFileChange"
      :disabled="uploadStatus === 'uploading' || uploadStatus === 'checking'"
    >
      <el-icon class="el-icon--upload"><upload-filled /></el-icon>
      <div class="el-upload__text">
        拖拽文件到此处，或 <em>点击选择文件</em>
      </div>
      <template #tip>
        <div class="el-upload__tip">
          支持分片上传、断点续传和秒传，单文件最大 100MB
        </div>
      </template>
    </el-upload>

    <div v-if="selectedFile" class="upload-info">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="文件名">{{ selectedFile.name }}</el-descriptions-item>
        <el-descriptions-item label="文件大小">{{ formatFileSize(selectedFile.size) }}</el-descriptions-item>
        <el-descriptions-item label="文件类型">{{ selectedFile.type || '未知' }}</el-descriptions-item>
        <el-descriptions-item label="MD5">{{ fileMd5 || '计算中...' }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <div v-if="uploadStatus === 'md5-calculating'" class="status-message info">
      <el-icon class="is-loading"><loading /></el-icon>
      <span>正在计算文件 MD5...</span>
      <el-progress :percentage="md5Progress" :show-text="false" style="margin-top: 10px;" />
    </div>

    <div v-if="uploadStatus === 'checking'" class="status-message info">
      <el-icon class="is-loading"><loading /></el-icon>
      <span>正在检查文件是否存在...</span>
    </div>

    <div v-if="uploadStatus === 'exist'" class="status-message success">
      <el-icon><success-filled /></el-icon>
      <span>文件已存在，秒传成功！无需重复上传</span>
    </div>

    <div v-if="uploadStatus === 'uploading' || uploadStatus === 'paused'" class="progress-section">
      <el-progress
        :percentage="uploadProgress"
        :status="uploadProgress === 100 ? 'success' : ''"
        :stroke-width="20"
      />
      <div class="progress-details">
        <span>上传进度：{{ uploadProgress }}%</span>
        <span>上传速度：{{ uploadSpeed }}</span>
        <span>剩余时间：{{ remainingTime }}</span>
      </div>
      <div class="chunk-info">
        <el-tag size="small" type="success">已上传：{{ uploadedChunks.length }}</el-tag>
        <el-tag size="small">总分片：{{ totalChunks }}</el-tag>
        <el-tag size="small" type="info">分片大小：{{ formatFileSize(CHUNK_SIZE) }}</el-tag>
      </div>
    </div>

    <div v-if="uploadStatus === 'completed'" class="status-message success">
      <el-icon><success-filled /></el-icon>
      <span>文件上传成功！</span>
    </div>

    <div v-if="uploadStatus === 'error'" class="status-message error">
      <el-icon><circle-close-filled /></el-icon>
      <span>{{ errorMessage }}</span>
    </div>

    <div class="action-buttons">
      <el-button
        type="primary"
        :disabled="!fileMd5 || uploadStatus === 'uploading' || uploadStatus === 'checking' || uploadStatus === 'md5-calculating'"
        @click="startUpload"
      >
        开始上传
      </el-button>
      <el-button
        v-if="uploadStatus === 'uploading'"
        type="warning"
        @click="pauseUpload"
      >
        暂停
      </el-button>
      <el-button
        v-if="uploadStatus === 'paused'"
        type="success"
        @click="resumeUpload"
      >
        继续
      </el-button>
      <el-button
        :disabled="uploadStatus === 'checking' || uploadStatus === 'md5-calculating'"
        @click="resetUpload"
      >
        重置
      </el-button>
    </div>
  </el-card>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Loading, SuccessFilled, CircleCloseFilled } from '@element-plus/icons-vue'
import SparkMD5 from 'spark-md5'
import { checkFile, uploadChunk, mergeChunks } from '../api/fileApi'

const selectedFile = ref(null)
const fileMd5 = ref('')
const uploadStatus = ref('idle')
const uploadProgress = ref(0)
const uploadedChunks = ref([])
const totalChunks = ref(0)
const isPaused = ref(false)
const errorMessage = ref('')
const md5Progress = ref(0)
const uploadSpeed = ref('0 KB/s')
const remainingTime = ref('计算中...')

const CHUNK_SIZE = 1 * 1024 * 1024

let uploadStartTime = 0
let uploadedBytes = 0

const handleFileChange = (file) => {
  selectedFile.value = file.raw
  fileMd5.value = ''
  uploadStatus.value = 'md5-calculating'
  uploadProgress.value = 0
  uploadedChunks.value = []
  md5Progress.value = 0
  errorMessage.value = ''
  calculateMD5(file.raw)
}

const calculateMD5 = (file) => {
  const spark = new SparkMD5.ArrayBuffer()
  const fileReader = new FileReader()
  const chunks = Math.ceil(file.size / CHUNK_SIZE)
  let currentChunk = 0

  fileReader.onload = (e) => {
    spark.append(e.target.result)
    currentChunk++
    md5Progress.value = Math.round((currentChunk / chunks) * 100)

    if (currentChunk < chunks) {
      loadNextChunk()
    } else {
      fileMd5.value = spark.end()
      uploadStatus.value = 'idle'
      ElMessage.success('MD5 计算完成')
    }
  }

  fileReader.onerror = () => {
    uploadStatus.value = 'error'
    errorMessage.value = 'MD5 计算失败，请重试'
    ElMessage.error('MD5 计算失败')
  }

  const loadNextChunk = () => {
    const start = currentChunk * CHUNK_SIZE
    const end = Math.min(start + CHUNK_SIZE, file.size)
    fileReader.readAsArrayBuffer(file.slice(start, end))
  }

  loadNextChunk()
}

const startUpload = async () => {
  if (!selectedFile.value || !fileMd5.value) {
    ElMessage.warning('请先选择文件')
    return
  }

  uploadStatus.value = 'checking'
  isPaused.value = false
  uploadedBytes = 0
  uploadStartTime = Date.now()

  try {
    const response = await checkFile({
      fileName: selectedFile.value.name,
      fileMd5: fileMd5.value,
      fileSize: selectedFile.value.size,
      totalChunks: Math.ceil(selectedFile.value.size / CHUNK_SIZE)
    })

    const checkResult = response.data

    if (checkResult.isExist) {
      uploadStatus.value = 'exist'
      uploadProgress.value = 100
      ElMessage.success('文件已存在，秒传成功！')
      return
    }

    if (checkResult.status === 'UPLOADING' && checkResult.uploadedChunks.length > 0) {
      uploadedChunks.value = checkResult.uploadedChunks
      uploadedBytes = uploadedChunks.value.length * CHUNK_SIZE
      uploadProgress.value = Math.round((uploadedChunks.value.length / Math.ceil(selectedFile.value.size / CHUNK_SIZE)) * 100)
      ElMessage.info(`发现未完成的上传，已恢复 ${uploadedChunks.value.length} 个分片`)
    }

    await uploadChunks()
  } catch (error) {
    uploadStatus.value = 'error'
    errorMessage.value = '检查文件失败：' + (error.response?.data?.message || error.message)
    ElMessage.error(errorMessage.value)
  }
}

const uploadChunks = async () => {
  uploadStatus.value = 'uploading'
  totalChunks.value = Math.ceil(selectedFile.value.size / CHUNK_SIZE)

  for (let i = 0; i < totalChunks.value; i++) {
    if (uploadedChunks.value.includes(i)) {
      continue
    }

    while (isPaused.value) {
      await new Promise(resolve => setTimeout(resolve, 100))
    }

    if (uploadStatus.value !== 'uploading') {
      return
    }

    const start = i * CHUNK_SIZE
    const end = Math.min(start + CHUNK_SIZE, selectedFile.value.size)
    const chunkBlob = selectedFile.value.slice(start, end)
    const chunkSize = end - start

    try {
      await uploadChunk(chunkBlob, fileMd5.value, i, totalChunks.value)
      uploadedChunks.value.push(i)
      uploadedBytes += chunkSize

      uploadProgress.value = Math.round((uploadedChunks.value.length / totalChunks.value) * 100)
      updateUploadSpeed()
    } catch (error) {
      uploadStatus.value = 'error'
      errorMessage.value = `分片 ${i} 上传失败：${error.message}`
      ElMessage.error(errorMessage.value)
      return
    }
  }

  if (uploadStatus.value === 'uploading') {
    await mergeChunksFile()
  }
}

const mergeChunksFile = async () => {
  try {
    await mergeChunks({
      fileMd5: fileMd5.value,
      fileName: selectedFile.value.name,
      totalChunks: totalChunks.value,
      fileSize: selectedFile.value.size,
      contentType: selectedFile.value.type
    })

    uploadStatus.value = 'completed'
    uploadProgress.value = 100
    ElMessage.success('文件上传成功！')
  } catch (error) {
    uploadStatus.value = 'error'
    errorMessage.value = '合并分片失败：' + error.message
    ElMessage.error(errorMessage.value)
  }
}

const updateUploadSpeed = () => {
  const elapsed = (Date.now() - uploadStartTime) / 1000
  if (elapsed > 0) {
    const speed = uploadedBytes / elapsed
    uploadSpeed.value = formatFileSize(speed) + '/s'

    const remainingBytes = selectedFile.value.size - uploadedBytes
    const remainingSeconds = remainingBytes / speed
    remainingTime.value = formatTime(remainingSeconds)
  }
}

const formatTime = (seconds) => {
  if (seconds < 60) {
    return Math.round(seconds) + ' 秒'
  } else if (seconds < 3600) {
    return Math.round(seconds / 60) + ' 分钟'
  } else {
    return Math.round(seconds / 3600) + ' 小时'
  }
}

const pauseUpload = () => {
  isPaused.value = true
  uploadStatus.value = 'paused'
  ElMessage.info('上传已暂停')
}

const resumeUpload = () => {
  isPaused.value = false
  uploadStatus.value = 'uploading'
  ElMessage.info('上传已继续')
  uploadChunks()
}

const resetUpload = () => {
  selectedFile.value = null
  fileMd5.value = ''
  uploadStatus.value = 'idle'
  uploadProgress.value = 0
  uploadedChunks.value = []
  totalChunks.value = 0
  isPaused.value = false
  errorMessage.value = ''
  md5Progress.value = 0
  uploadSpeed.value = '0 KB/s'
  remainingTime.value = '计算中...'
  uploadedBytes = 0
}

const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  font-weight: bold;
}

.upload-info {
  margin-top: 20px;
}

.status-message {
  margin-top: 20px;
  padding: 15px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-radius: 4px;
}

.status-message.info {
  background-color: #ecf5ff;
  color: #409eff;
}

.status-message.success {
  background-color: #f0f9ff;
  color: #67c23a;
}

.status-message.error {
  background-color: #fef0f0;
  color: #f56c6c;
}

.progress-section {
  margin-top: 20px;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.progress-details {
  margin-top: 15px;
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #606266;
}

.chunk-info {
  margin-top: 10px;
  display: flex;
  gap: 10px;
  justify-content: center;
}

.action-buttons {
  margin-top: 20px;
  display: flex;
  gap: 10px;
  justify-content: center;
}
</style>
