package com.truongtho.be.service;

import com.truongtho.be.config.properties.R2Properties;
import com.truongtho.be.config.properties.UploadProperties;
import com.truongtho.be.enums.UploadFolder;
import com.truongtho.be.exception.BadRequestException;
import com.truongtho.be.exception.FileStorageException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class R2FileStorageService implements FileStorageService {
    static String TMP_PREFIX = "tmp/";
    static Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    S3Client s3Client;
    R2Properties props;
    UploadProperties uploadProps;

    @Override
    public String uploadTemp(MultipartFile file, UploadFolder folder) {
        validate(file, folder);
        String key = TMP_PREFIX + folder.getPath() + "/"
                + UUID.randomUUID() + EXTENSIONS.get(file.getContentType());

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(props.getBucket())
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes()));
        } catch (Exception e) {
            throw new FileStorageException("Upload ảnh thất bại", e);
        }
        return key;
    }

    @Override
    public String promote(String tmpKey, UploadFolder folder) {
        String expectedPrefix = TMP_PREFIX + folder.getPath() + "/";
        if (tmpKey == null || !tmpKey.startsWith(expectedPrefix) || tmpKey.contains("..")) {
            throw new BadRequestException("Key ảnh không hợp lệ");
        }
        String finalKey = tmpKey.substring(TMP_PREFIX.length());
        try {
            var tmpObject = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(tmpKey)
                    .build());
            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(props.getBucket())
                            .key(finalKey)
                            .contentType(tmpObject.response().contentType())
                            .build(),
                    RequestBody.fromBytes(tmpObject.asByteArray()));
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(finalKey)
                    .build());
            log.info("Đã chuyển ảnh {} -> {} (bucket {})", tmpKey, finalKey, props.getBucket());
        } catch (NoSuchKeyException e) {
            throw new BadRequestException("Ảnh tạm không tồn tại hoặc đã hết hạn");
        } catch (Exception e) {
            log.error("promote lỗi: tmpKey={}, finalKey={}, bucket={}", tmpKey, finalKey, props.getBucket(), e);
            throw new FileStorageException("Không lưu được ảnh", e);
        }
        return publicBase() + "/" + finalKey;
    }

    @Override
    public void deleteTemp(String tmpKey) {
        if (tmpKey == null || !tmpKey.startsWith(TMP_PREFIX) || tmpKey.contains("..")) return;
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(props.getBucket())
                .key(tmpKey)
                .build());
    }

    @Override
    public void delete(String url) {
        String prefix = publicBase() + "/";
        if (!url.startsWith(prefix)) {
            throw new FileStorageException("URL không thuộc bucket: " + url, null);
        }
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(props.getBucket())
                .key(url.substring(prefix.length()))
                .build());
    }

    private String publicBase() {
        String base = props.getPublicUrl();
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    private void validate(MultipartFile file, UploadFolder folder) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File ảnh rỗng");
        }
        if (!uploadProps.getAllowedTypes().contains(file.getContentType())
                || !EXTENSIONS.containsKey(file.getContentType())) {
            throw new BadRequestException("Chỉ hỗ trợ ảnh JPEG, PNG, WEBP");
        }
        if (file.getSize() > folder.getMaxSize().toBytes()) {
            throw new BadRequestException("Ảnh vượt quá " + folder.getMaxSize().toMegabytes() + "MB");
        }
    }
}