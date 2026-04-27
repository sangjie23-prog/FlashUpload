package com.flashupload;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 第二阶段增加普通上传测试，确保接口和本地落盘行为可以稳定验证。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "flash-upload.storage.path=target/test-storage/files")
class FileControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldUploadFileSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "hello.txt",
            "text/plain",
            "hello flash upload".getBytes()
        );

        mockMvc.perform(multipart("/api/files/upload").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fileName", is("hello.txt")))
            .andExpect(jsonPath("$.fileSize", is(18)))
            .andExpect(jsonPath("$.contentType", is("text/plain")));

        Path storageRoot = Path.of("target/test-storage/files");
        boolean fileExists = Files.exists(storageRoot) && Files.walk(storageRoot).anyMatch(Files::isRegularFile);
        if (!fileExists) {
            throw new AssertionError("上传测试未在本地生成文件");
        }
    }
}
