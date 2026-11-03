package vn.hoidanit.springrestwithai.feature.file;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import vn.hoidanit.springrestwithai.dto.ApiResponse;
import vn.hoidanit.springrestwithai.feature.file.dto.FileUploadResponse;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    // multipart/form-data has no @RequestBody to @Valid — validation lives in FileService
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam("folder") String folder) {
        FileUploadResponse uploaded = this.fileService.uploadFile(file, folder);
        return ResponseEntity.created(URI.create(uploaded.fileUrl()))
                .body(ApiResponse.created("Upload file thành công", uploaded));
    }
}
