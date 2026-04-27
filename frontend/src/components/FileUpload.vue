<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>文件上传</span>
      </div>
    </template>

    <el-upload
      drag
      :auto-upload="false"
      :limit="1"
      :on-change="handleFileChange"
    >
      <el-icon class="el-icon--upload"><upload-filled /></el-icon>
      <div class="el-upload__text">
        拖拽文件到此处，或 <em>点击选择文件</em>
      </div>
    </el-upload>

    <div v-if="selectedFile" class="upload-info">
      <p><strong>文件名：</strong>{{ selectedFile.name }}</p>
      <p><strong>文件大小：</strong>{{ formatFileSize(selectedFile.size) }}</p>
      <p><strong>MD5：</strong>{{ fileMd5 || '计算中...' }}</p>
    </div>

    <div v-if="fileMd5 && uploadStatus === 'checking'" class="status-message">
      <el-icon class="is-loading"><loading /></el-icon>
      <span>正在检查文件是否存在...</span>
    </div>

    <div v-if="uploadStatus === 'exist'" class="status-message success">
      <el-icon><success-filled /></el-icon>
      <span>文件已存在，秒传成功！</span>
    </div>

    <div v-if="uploadStatus === 'uploading'" class="progress-section">
      <el-progress :percentage="uploadProgress" :status="uploadProgress === 100 ? 'success' : ''" />
      <p>上传进度：{{ uploadProgress }}%</p>
      <p>已上传分片：{{ uploadedChunks.length }} / {{ totalChunks }}</p>
    </div>

    <div class="action-buttons">
      <el-button
        type="primary"
        :disabled="!fileMd5 || uploadStatus === 'uploading' || uploadStatus === 'checking'"
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
        :disabled="uploadStatus === 'checking'"
        @click="resetUpload"
      >
        重置
      </el-button>
    </div>
  </el-card>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Loading, SuccessFilled } from '@element-plus/icons-vue'
import SparkMD5 from 'spark-md5'
import { checkFile, uploadChunk, mergeChunks } from '../api/fileApi'

const selectedFile = ref(null)
const fileMd5 = ref('')
const uploadStatus = ref('idle')
const uploadProgress = ref(0)
const uploadedChunks = ref([])
const totalChunks = ref(0)
const isPaused = ref(false)

const CHUNK_SIZE = 1 * 1024 * 1024

const handleFileChange = (file) => {
  selectedFile.value = file.raw
  fileMd5.value = ''
  uploadStatus.value = 'idle'
  uploadProgress.value = 0
  uploadedChunks.value = []
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

    if (currentChunk < chunks) {
      loadNextChunk()
    } else {
      fileMd5.value = spark.end()
      ElMessage.success('MD5 计算完成')
    }
  }

  fileReader.onerror = () => {
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
      ElMessage.success('文件已存在，秒传成功！')
      return
    }

    if (checkResult.status === 'UPLOADING' && checkResult.uploadedChunks.length > 0) {
      uploadedChunks.value = checkResult.uploadedChunks
      ElMessage.info(`发现未完成的上传，已恢复 ${uploadedChunks.value.length} 个分片`)
    }

    await uploadChunks()
  } catch (error) {
    ElMessage.error('检查文件失败：' + (error.response?.data?.message || error.message))
    uploadStatus.value = 'idle'
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

    try {
      await uploadChunk(chunkBlob, fileMd5.value, i, totalChunks.value)
      uploadedChunks.value.push(i)
      uploadProgress.value = Math.round((uploadedChunks.value.length / totalChunks.value) * 100)
    } catch (error) {
      ElMessage.error(`分片 ${i} 上传失败`)
      uploadStatus.value = 'idle'
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
    ElMessage.error('合并分片失败')
    uploadStatus.value = 'idle'
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
  align-items: center;
  font-size: 18px;
  font-weight: bold;
}

.upload-info {
  margin-top: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.upload-info p {
  margin: 8px 0;
}

.status-message {
  margin-top: 20px;
  padding: 15px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-radius: 4px;
}

.status-message.success {
  background-color: #f0f9ff;
  color: #67c23a;
}

.progress-section {
  margin-top: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.progress-section p {
  margin: 8px 0;
  text-align: center;
}

.action-buttons {
  margin-top: 20px;
  display: flex;
  gap: 10px;
  justify-content: center;
}
</style>
