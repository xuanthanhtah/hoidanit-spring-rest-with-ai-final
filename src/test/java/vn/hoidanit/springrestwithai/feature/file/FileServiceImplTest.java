package vn.hoidanit.springrestwithai.feature.file;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import vn.hoidanit.springrestwithai.config.UploadProperties;
import vn.hoidanit.springrestwithai.exception.InvalidRequestException;
import vn.hoidanit.springrestwithai.feature.file.dto.FileUploadResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileServiceImplTest {

    private static final long MAX_SIZE = 5_242_880L;
    private static final byte[] CONTENT = {1, 2, 3, 4};

    @TempDir
    private Path baseDir;

    private FileServiceImpl fileService;

    @BeforeEach
    void setUp() {
        UploadProperties properties = new UploadProperties(baseDir.toString(), MAX_SIZE,
                List.of("jpg", "jpeg", "png", "gif", "webp"), List.of("avatars", "logos"));
        fileService = new FileServiceImpl(properties);
    }

    private static MockMultipartFile image(String originalName, byte[] content) {
        return new MockMultipartFile("file", originalName, "image/jpeg", content);
    }

    @Test
    @DisplayName("Should store file under {base-dir}/{folder} with timestamp-prefixed name")
    void uploadFile_validFile_storesAndReturnsMetadata() throws IOException {
        FileUploadResponse result = fileService.uploadFile(image("avatar.jpg", CONTENT), "avatars");

        assertTrue(result.fileName().matches("\\d+_avatar\\.jpg"));
        assertEquals("avatars", result.folder());
        assertEquals("/uploads/avatars/" + result.fileName(), result.fileUrl());
        assertEquals(CONTENT.length, result.size());
        assertNotNull(result.uploadedAt());
        Path stored = baseDir.resolve("avatars").resolve(result.fileName());
        assertArrayEquals(CONTENT, Files.readAllBytes(stored));
    }

    @Test
    @DisplayName("Should accept upper-case extension")
    void uploadFile_upperCaseExtension_accepted() {
        FileUploadResponse result = fileService.uploadFile(image("LOGO.PNG", CONTENT), "logos");

        assertTrue(result.fileName().endsWith("_LOGO.PNG"));
    }

    @Test
    @DisplayName("Should reject folder outside the whitelist")
    void uploadFile_invalidFolder_throwsInvalidRequestException() {
        assertThrows(InvalidRequestException.class,
                () -> fileService.uploadFile(image("avatar.jpg", CONTENT), "../etc"));
        assertFalse(Files.exists(baseDir.resolve("etc")));
    }

    @Test
    @DisplayName("Should reject empty file")
    void uploadFile_emptyFile_throwsInvalidRequestException() {
        assertThrows(InvalidRequestException.class,
                () -> fileService.uploadFile(image("avatar.jpg", new byte[0]), "avatars"));
    }

    @Test
    @DisplayName("Should reject null file")
    void uploadFile_nullFile_throwsInvalidRequestException() {
        assertThrows(InvalidRequestException.class, () -> fileService.uploadFile(null, "avatars"));
    }

    @Test
    @DisplayName("Should reject blank file name")
    void uploadFile_blankFileName_throwsInvalidRequestException() {
        assertThrows(InvalidRequestException.class,
                () -> fileService.uploadFile(image("   ", CONTENT), "avatars"));
    }

    @Test
    @DisplayName("Should reject file names with spaces, diacritics or path separators")
    void uploadFile_specialCharacters_throwsInvalidRequestException() {
        for (String name : List.of("my photo.jpg", "ảnh.jpg", "a(1).jpg", "..\\evil.jpg", "dir/evil.jpg")) {
            assertThrows(InvalidRequestException.class,
                    () -> fileService.uploadFile(image(name, CONTENT), "avatars"), name);
        }
    }

    @Test
    @DisplayName("Should reject file name longer than 200 characters")
    void uploadFile_tooLongFileName_throwsInvalidRequestException() {
        String name = "a".repeat(197) + ".jpg";

        assertThrows(InvalidRequestException.class,
                () -> fileService.uploadFile(image(name, CONTENT), "avatars"));
    }

    @Test
    @DisplayName("Should reject extensions outside the whitelist or missing extension")
    void uploadFile_disallowedExtension_throwsInvalidRequestException() {
        for (String name : List.of("shell.php", "photo.jpg.exe", "noextension", ".jpg")) {
            assertThrows(InvalidRequestException.class,
                    () -> fileService.uploadFile(image(name, CONTENT), "avatars"), name);
        }
    }

    @Test
    @DisplayName("Should reject file larger than 5 MB and accept exactly 5 MB")
    void uploadFile_sizeLimit_enforced() {
        assertThrows(InvalidRequestException.class,
                () -> fileService.uploadFile(image("big.jpg", new byte[(int) MAX_SIZE + 1]), "avatars"));

        FileUploadResponse result = fileService.uploadFile(image("max.jpg", new byte[(int) MAX_SIZE]), "avatars");
        assertEquals(MAX_SIZE, result.size());
    }
}
