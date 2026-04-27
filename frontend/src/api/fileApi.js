import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 60000
})

export function checkFile(data) {
  return api.post('/files/check', data)
}

export function uploadChunk(chunkFile, fileMd5, chunkIndex, totalChunks, onProgress) {
  const formData = new FormData()
  formData.append('file', chunkFile)
  formData.append('fileMd5', fileMd5)
  formData.append('chunkIndex', chunkIndex)
  formData.append('totalChunks', totalChunks)

  return api.post('/files/upload-chunk', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: onProgress
  })
}

export function mergeChunks(data) {
  return api.post('/files/merge', data)
}

export function getFileList(page = 0, size = 10, keyword = '') {
  const params = { page, size }
  if (keyword) {
    params.keyword = keyword
  }
  return api.get('/files', { params })
}

export function downloadFile(id) {
  return api.get(`/files/${id}/download`, {
    responseType: 'blob'
  })
}

export function deleteFile(id) {
  return api.delete(`/files/${id}`)
}

export default api
