package vn.hoidanit.springrestwithai.feature.file;

import org.springframework.web.multipart.MultipartFile;

import vn.hoidanit.springrestwithai.feature.file.dto.FileUploadResponse;

public interface FileService {

    FileUploadResponse uploadFile(MultipartFile file, String folder);
}
