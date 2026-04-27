import requests
import hashlib
import os

BASE_URL = 'http://localhost:8080/api/files'

print("=" * 60)
print("测试 1：文件列表查询（初始为空）")
print("=" * 60)
response = requests.get(f'{BASE_URL}')
print(f"状态码: {response.status_code}")
data = response.json()
print(f"总记录数: {data['totalElements']}")
print(f"总页数: {data['totalPages']}")
print(f"当前页: {data['number']}")
print(f"内容: {data['content']}")

print("\n" + "=" * 60)
print("测试 2：上传文件（分片上传 + 合并）")
print("=" * 60)

test_file_path = 'f:/projects/FlashUpload/large_test.txt'
with open(test_file_path, 'rb') as f:
    file_data = f.read()

file_md5 = hashlib.md5(file_data).hexdigest()
print(f"文件 MD5: {file_md5}")
print(f"文件大小: {len(file_data)} 字节")

chunk_size = len(file_data) // 3
chunks = [
    file_data[:chunk_size],
    file_data[chunk_size:chunk_size*2],
    file_data[chunk_size*2:]
]

print(f"\n上传 {len(chunks)} 个分片...")
for i, chunk_data in enumerate(chunks):
    chunk_file = {'file': (f'chunk-{i}', chunk_data)}
    response = requests.post(
        f'{BASE_URL}/upload-chunk',
        files=chunk_file,
        data={
            'fileMd5': file_md5,
            'chunkIndex': i,
            'totalChunks': len(chunks)
        }
    )
    print(f"  分片 {i}: {response.json()['uploadedChunks']}")

print("\n合并分片...")
merge_response = requests.post(
    f'{BASE_URL}/merge',
    json={
        'fileMd5': file_md5,
        'fileName': 'large_test.txt',
        'totalChunks': len(chunks),
        'fileSize': len(file_data),
        'contentType': 'text/plain'
    }
)
merge_result = merge_response.json()
print(f"合并成功！文件 ID: {merge_result['id']}")
file_id = merge_result['id']

print("\n" + "=" * 60)
print("测试 3：文件列表查询（应有 1 条记录）")
print("=" * 60)
response = requests.get(f'{BASE_URL}')
data = response.json()
print(f"总记录数: {data['totalElements']}")
if data['content']:
    file_info = data['content'][0]
    print(f"文件名: {file_info['fileName']}")
    print(f"文件大小: {file_info['fileSize']}")
    print(f"状态: {file_info['status']}")
    print(f"MD5: {file_info['fileMd5']}")

print("\n" + "=" * 60)
print("测试 4：文件下载")
print("=" * 60)
download_response = requests.get(f'{BASE_URL}/{file_id}/download')
print(f"状态码: {download_response.status_code}")
print(f"Content-Disposition: {download_response.headers.get('Content-Disposition')}")
print(f"Content-Length: {download_response.headers.get('Content-Length')}")

if download_response.status_code == 200:
    downloaded_data = download_response.content
    downloaded_md5 = hashlib.md5(downloaded_data).hexdigest()
    print(f"下载文件 MD5: {downloaded_md5}")
    print(f"原始文件 MD5: {file_md5}")
    print(f"MD5 匹配: {'✅ 是' if downloaded_md5 == file_md5 else '❌ 否'}")
    print(f"文件大小匹配: {'✅ 是' if len(downloaded_data) == len(file_data) else '❌ 否'}")

print("\n" + "=" * 60)
print("测试 5：秒传检查（文件已存在）")
print("=" * 60)
check_response = requests.post(
    f'{BASE_URL}/check',
    json={
        'fileName': 'large_test.txt',
        'fileMd5': file_md5,
        'fileSize': len(file_data),
        'totalChunks': 3
    }
)
check_result = check_response.json()
print(f"文件是否存在: {check_result['isExist']}")
print(f"状态: {check_result['status']}")
print(f"文件 ID: {check_result['fileId']}")

print("\n" + "=" * 60)
print("测试 6：断点续传检查（模拟中断后重新上传）")
print("=" * 60)

new_file_md5 = hashlib.md5(b"new test file content").hexdigest()
print(f"新文件 MD5: {new_file_md5}")

print("\n上传分片 0 和 1...")
for i in [0, 1]:
    chunk_data = b"chunk data " + str(i).encode()
    chunk_file = {'file': (f'chunk-{i}', chunk_data)}
    response = requests.post(
        f'{BASE_URL}/upload-chunk',
        files=chunk_file,
        data={
            'fileMd5': new_file_md5,
            'chunkIndex': i,
            'totalChunks': 3
        }
    )
    print(f"  分片 {i}: {response.json()['uploadedChunks']}")

print("\n检查文件状态（应有 2 个已上传分片）...")
check_response = requests.post(
    f'{BASE_URL}/check',
    json={
        'fileName': 'new_test.txt',
        'fileMd5': new_file_md5,
        'fileSize': 100,
        'totalChunks': 3
    }
)
check_result = check_response.json()
print(f"文件是否存在: {check_result['isExist']}")
print(f"状态: {check_result['status']}")
print(f"已上传分片: {check_result['uploadedChunks']}")

print("\n上传剩余分片 2...")
chunk_data = b"chunk data 2"
chunk_file = {'file': (f'chunk-2', chunk_data)}
response = requests.post(
    f'{BASE_URL}/upload-chunk',
    files=chunk_file,
    data={
        'fileMd5': new_file_md5,
        'chunkIndex': 2,
        'totalChunks': 3
    }
)
print(f"  分片 2: {response.json()['uploadedChunks']}")

print("\n合并分片...")
merge_response = requests.post(
    f'{BASE_URL}/merge',
    json={
        'fileMd5': new_file_md5,
        'fileName': 'new_test.txt',
        'totalChunks': 3,
        'fileSize': 100,
        'contentType': 'text/plain'
    }
)
print(f"合并结果: {merge_response.json()['id']}")

print("\n" + "=" * 60)
print("测试 7：最终文件列表（应有 2 条记录）")
print("=" * 60)
response = requests.get(f'{BASE_URL}')
data = response.json()
print(f"总记录数: {data['totalElements']}")
for i, file_info in enumerate(data['content']):
    print(f"  {i+1}. {file_info['fileName']} - {file_info['fileSize']} 字节 - {file_info['status']}")

print("\n" + "=" * 60)
print("✅ 所有测试完成！")
print("=" * 60)
