package com.truongtho.be.service;

import com.truongtho.be.enums.UploadFolder;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String uploadTemp(MultipartFile file, UploadFolder folder);
    String promote(String tmpKey, UploadFolder folder);
    void delete(String url);
}