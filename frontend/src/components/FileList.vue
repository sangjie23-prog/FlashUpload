<template>
  <el-card style="margin-top: 20px;">
    <template #header>
      <div class="card-header">
        <span>文件列表</span>
        <div class="header-actions">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索文件名..."
            clearable
            style="width: 200px; margin-right: 10px;"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix>
              <el-icon><search /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" @click="loadFileList">刷新</el-button>
        </div>
      </div>
    </template>

    <el-table :data="fileList" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="fileName" label="文件名" min-width="200" />
      <el-table-column prop="fileSize" label="文件大小" width="120">
        <template #default="{ row }">
          {{ formatFileSize(row.fileSize) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.status === 'COMPLETED' ? 'success' : 'warning'">
            {{ row.status === 'COMPLETED' ? '已完成' : '上传中' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="上传时间" width="180">
        <template #default="{ row }">
          {{ formatTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button
            type="primary"
            size="small"
            :disabled="row.status !== 'COMPLETED'"
            @click="handleDownload(row)"
          >
            下载
          </el-button>
          <el-popconfirm
            title="确定要删除这个文件吗？"
            confirm-button-text="确定"
            cancel-button-text="取消"
            @confirm="handleDelete(row)"
          >
            <template #reference>
              <el-button type="danger" size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      style="margin-top: 20px; justify-content: center;"
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[5, 10, 20]"
      layout="total, sizes, prev, pager, next"
      @current-change="loadFileList"
      @size-change="loadFileList"
    />
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getFileList, downloadFile, deleteFile } from '../api/fileApi'

const fileList = ref([])
const loading = ref(false)
const currentPage = ref(0)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')

onMounted(() => {
  loadFileList()
})

const loadFileList = async () => {
  loading.value = true
  try {
    const response = await getFileList(currentPage.value, pageSize.value, searchKeyword.value)
    fileList.value = response.data.content
    total.value = response.data.totalElements
  } catch (error) {
    ElMessage.error('获取文件列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 0
  loadFileList()
}

const handleDownload = async (file) => {
  try {
    const response = await downloadFile(file.id)
    const blob = new Blob([response.data])
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = file.fileName
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('下载成功')
  } catch (error) {
    ElMessage.error('下载失败')
  }
}

const handleDelete = async (file) => {
  try {
    await deleteFile(file.id)
    ElMessage.success('删除成功')
    loadFileList()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
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

.header-actions {
  display: flex;
  align-items: center;
}
</style>
