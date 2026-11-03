package vn.hoidanit.springrestwithai.feature.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import vn.hoidanit.springrestwithai.config.UploadProperties;
import vn.hoidanit.springrestwithai.exception.FileStorageException;
import vn.hoidanit.springrestwithai.exception.InvalidRequestException;
import vn.hoidanit.springrestwithai.feature.file.dto.FileUploadResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class FileServiceImpl implements FileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    // Letters/digits without diacritics plus - _ . only (API_SPEC) — also blocks path separators
    private static final Pattern VALID_FILE_NAME = Pattern.compile("^[A-Za-z0-9._-]+$");
    // Keeps "{epochMillis}_{name}" within the 255-char avatar/logo columns
    private static final int MAX_FILE_NAME_LENGTH = 200;

    private final UploadProperties uploadProperties;
    private final Path baseDir;

    public FileServiceImpl(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
        this.baseDir = Path.of(uploadProperties.baseDir()).toAbsolutePath().normalize();
    }

    @Override
    public FileUploadResponse uploadFile(MultipartFile file, String folder) {
        validateFolder(folder);
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("File không được để trống");
        }
        String originalName = file.getOriginalFilename();
        validateFileName(originalName);
        validateExtension(originalName);
        validateSize(file.getSize());

        String storedName = System.currentTimeMillis() + "_" + originalName;
        Path target = resolveTarget(folder, storedName);
        writeFile(file, target);

        log.info("Uploaded file {} to folder {} ({} bytes)", storedName, folder, file.getSize());
        return new FileUploadResponse(storedName, folder, "/uploads/" + folder + "/" + storedName,
                file.getSize(), Instant.now());
    }

    private void validateFolder(String folder) {
        if (folder == null || !this.uploadProperties.allowedFolders().contains(folder)) {
            throw new InvalidRequestException("Folder không hợp lệ, chỉ chấp nhận: "
                    + String.join(", ", this.uploadProperties.allowedFolders()));
        }
    }

    private void validateFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new InvalidRequestException("Tên file không được để trống");
        }
        if (fileName.length() > MAX_FILE_NAME_LENGTH) {
            throw new InvalidRequestException("Tên file không được quá " + MAX_FILE_NAME_LENGTH + " ký tự");
        }
        if (!VALID_FILE_NAME.matcher(fileName).matches()) {
            throw new InvalidRequestException(
                    "Tên file chỉ được chứa chữ cái không dấu, chữ số và các ký tự - _ .");
        }
    }

    private void validateExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        String extension = dotIndex > 0 ? fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT) : "";
        if (!this.uploadProperties.allowedExtensions().contains(extension)) {
            throw new InvalidRequestException("Định dạng file không được hỗ trợ, chỉ chấp nhận: "
                    + String.join(", ", this.uploadProperties.allowedExtensions()));
        }
    }

    private void validateSize(long size) {
        if (size > this.uploadProperties.maxSizeBytes()) {
            throw new InvalidRequestException("Kích thước file vượt quá giới hạn cho phép (5 MB)");
        }
    }

    // normalize + prefix check guards against path traversal even if validation above is loosened (ADR-002)
    private Path resolveTarget(String folder, String storedName) {
        Path target = this.baseDir.resolve(folder).resolve(storedName).normalize();
        if (!target.startsWith(this.baseDir.resolve(folder))) {
            throw new InvalidRequestException("Đường dẫn file không hợp lệ");
        }
        return target;
    }

    private void writeFile(MultipartFile file, Path target) {
        try (InputStream inputStream = file.getInputStream()) {
            Files.createDirectories(target.getParent());
            Files.copy(inputStream, target);
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + target.getFileName(), ex);
        }
    }
}
