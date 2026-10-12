package com.drone.platform.module.file.controller;

import com.drone.platform.common.BusinessException;
import com.drone.platform.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/file")
public class FileController {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.max-size-mb:20}")
    private long maxSizeMb;

    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new BusinessException("上传文件不能为空");
        if (file.getSize() > maxSizeMb * 1024L * 1024L) {
            throw new BusinessException("文件大小不能超过" + maxSizeMb + "MB");
        }

        String originalName = file.getOriginalFilename();
        String extension = getExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("仅支持 JPG、JPEG、PNG、WEBP 图片");
        }

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path directory = Paths.get(uploadDir, datePath).toAbsolutePath().normalize();
        Files.createDirectories(directory);
        Path target = directory.resolve(fileName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        String relativeUrl = "/uploads/" + datePath + "/" + fileName;
        String accessUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(relativeUrl)
                .toUriString();

        Map<String, Object> data = new HashMap<>();
        data.put("fileName", originalName);
        data.put("url", accessUrl);
        data.put("relativeUrl", relativeUrl);
        data.put("size", file.getSize());
        return Result.ok(data);
    }

    private String getExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') < 0) return "";
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}