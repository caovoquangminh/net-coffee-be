package com.netcoffee.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /**
     * Lưu file upload vào thư mục cục bộ và trả về URL công khai.
     *
     * @param file file được upload
     * @return URL công khai (ví dụ: http://localhost:8080/uploads/uuid.jpg)
     */
    String store(MultipartFile file);
}
