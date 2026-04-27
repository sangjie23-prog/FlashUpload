<template>
  <el-container>
    <el-header>
      <h1>FlashUpload - 文件上传系统</h1>
    </el-header>
    <el-main>
      <FileUpload />
      <FileList />
    </el-main>
  </el-container>
</template>

<script setup>
import FileUpload from './components/FileUpload.vue'
import FileList from './components/FileList.vue'
</script>

<style>
body {
  margin: 0;
  padding: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.el-header {
  background-color: #409eff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
}

.el-header h1 {
  margin: 0;
  font-size: 24px;
}

.el-main {
  max-width: 1200px;
  margin: 20px auto;
  padding: 20px;
}
</style>
