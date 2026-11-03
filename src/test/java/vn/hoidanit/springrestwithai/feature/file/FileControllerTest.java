package vn.hoidanit.springrestwithai.feature.file;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.FileSystemUtils;

import vn.hoidanit.springrestwithai.config.UploadProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FileControllerTest {

    private static final String BASE_URL = "/api/v1/files";
    private static final byte[] CONTENT = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UploadProperties uploadProperties;

    @AfterEach
    void cleanUp() throws IOException {
        FileSystemUtils.deleteRecursively(Path.of(uploadProperties.baseDir()));
    }

    private static MockMultipartFile image(String originalName, byte[] content) {
        return new MockMultipartFile("file", originalName, MediaType.IMAGE_JPEG_VALUE, content);
    }

    // ========== POST ==========

    @Test
    @DisplayName("POST should return 201, store the file and serve it publicly at fileUrl")
    void uploadFile_validRequest_returns201() throws Exception {
        MvcResult result = mockMvc.perform(multipart(BASE_URL).file(image("avatar.jpg", CONTENT))
                        .param("folder", "avatars").with(jwt()))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Location", matchesPattern("/uploads/avatars/\\d+_avatar\\.jpg")))
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.message").value("Upload file thành công"))
                .andExpect(jsonPath("$.data.fileName").value(matchesPattern("\\d+_avatar\\.jpg")))
                .andExpect(jsonPath("$.data.folder").value("avatars"))
                .andExpect(jsonPath("$.data.size").value(CONTENT.length))
                .andExpect(jsonPath("$.data.uploadedAt").isNotEmpty())
                .andReturn();

        String fileUrl = result.getResponse().getHeader("Location");
        String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        assertTrue(Files.exists(Path.of(uploadProperties.baseDir(), "avatars", fileName)));

        // Static file is public: no JWT required
        mockMvc.perform(get(fileUrl))
                .andExpect(status().isOk())
                .andExpect(content().bytes(CONTENT));
    }

    @Test
    @DisplayName("POST should return 400 when no file part is provided")
    void uploadFile_missingFile_returns400() throws Exception {
        mockMvc.perform(multipart(BASE_URL).param("folder", "avatars").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("POST should return 400 when the file is empty")
    void uploadFile_emptyFile_returns400() throws Exception {
        mockMvc.perform(multipart(BASE_URL).file(image("avatar.jpg", new byte[0]))
                        .param("folder", "avatars").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("File không được để trống"));
    }

    @Test
    @DisplayName("POST should return 400 when the file name has invalid characters")
    void uploadFile_invalidFileName_returns400() throws Exception {
        mockMvc.perform(multipart(BASE_URL).file(image("my photo (1).jpg", CONTENT))
                        .param("folder", "avatars").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    @DisplayName("POST should return 400 when the extension is not allowed")
    void uploadFile_disallowedExtension_returns400() throws Exception {
        mockMvc.perform(multipart(BASE_URL).file(image("shell.php", CONTENT))
                        .param("folder", "avatars").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    @DisplayName("POST should return 400 when the file exceeds 5 MB")
    void uploadFile_tooLarge_returns400() throws Exception {
        mockMvc.perform(multipart(BASE_URL).file(image("big.jpg", new byte[5_242_881]))
                        .param("folder", "avatars").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    @DisplayName("POST should return 400 when folder is not avatars or logos")
    void uploadFile_invalidFolder_returns400() throws Exception {
        mockMvc.perform(multipart(BASE_URL).file(image("avatar.jpg", CONTENT))
                        .param("folder", "documents").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    @DisplayName("POST should return 400 when folder is missing")
    void uploadFile_missingFolder_returns400() throws Exception {
        mockMvc.perform(multipart(BASE_URL).file(image("avatar.jpg", CONTENT)).with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    @DisplayName("POST should return 415 when the request is not multipart")
    void uploadFile_notMultipart_returns415() throws Exception {
        mockMvc.perform(post(BASE_URL).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.statusCode").value(415));
    }

    @Test
    @DisplayName("POST should return 401 when no JWT is provided")
    void uploadFile_noToken_returns401() throws Exception {
        mockMvc.perform(multipart(BASE_URL).file(image("avatar.jpg", CONTENT)).param("folder", "avatars"))
                .andExpect(status().isUnauthorized());
    }
}
