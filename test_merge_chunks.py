import requests
import hashlib
import os

# 读取测试文件
test_file_path = 'f:/projects/FlashUpload/large_test.txt'
with open(test_file_path, 'rb') as f:
    file_data = f.read()

# 计算文件 MD5
file_md5 = hashlib.md5(file_data).hexdigest()
print(f"文件 MD5: {file_md5}")
print(f"文件大小: {len(file_data)} 字节")

# 将文件分成 3 个分片
chunk_size = len(file_data) // 3
chunks = [
    file_data[:chunk_size],
    file_data[chunk_size:chunk_size*2],
    file_data[chunk_size*2:]
]

print(f"\n总分片数: {len(chunks)}")
print(f"分片大小: {[len(c) for c in chunks]}")

# 步骤 1：上传所有分片
print("\n=== 步骤 1：上传所有分片 ===")
for i, chunk_data in enumerate(chunks):
    chunk_file = {'file': (f'chunk-{i}', chunk_data)}
    response = requests.post(
        'http://localhost:8080/api/files/upload-chunk',
        files=chunk_file,
        data={
            'fileMd5': file_md5,
            'chunkIndex': i,
            'totalChunks': len(chunks)
        }
    )
    print(f"分片 {i} 上传结果: {response.json()}")

# 步骤 2：调用合并接口
print("\n=== 步骤 2：合并分片 ===")
merge_response = requests.post(
    'http://localhost:8080/api/files/merge',
    json={
        'fileMd5': file_md5,
        'fileName': 'large_test.txt',
        'totalChunks': len(chunks),
        'fileSize': len(file_data),
        'contentType': 'text/plain'
    }
)

if merge_response.status_code == 200:
    print("合并成功！")
    result = merge_response.json()
    print(f"文件 ID: {result.get('id')}")
    print(f"文件名: {result.get('fileName')}")
    print(f"文件大小: {result.get('fileSize')}")
    print(f"存储路径: {result.get('storagePath')}")
    print(f"状态: {result.get('status')}")
    
    # 验证合并后的文件
    storage_path = result.get('storagePath')
    if storage_path and os.path.exists(storage_path):
        with open(storage_path, 'rb') as f:
            merged_data = f.read()
        merged_md5 = hashlib.md5(merged_data).hexdigest()
        print(f"\n合并文件 MD5: {merged_md5}")
        print(f"原始文件 MD5: {file_md5}")
        print(f"MD5 匹配: {merged_md5 == file_md5}")
        print(f"文件大小匹配: {len(merged_data) == len(file_data)}")
    else:
        print(f"警告: 合并文件不存在于路径: {storage_path}")
else:
    print(f"合并失败: {merge_response.status_code}")
    print(merge_response.text)
