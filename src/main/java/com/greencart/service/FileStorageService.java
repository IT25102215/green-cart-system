package com.greencart.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

  private static final long MAX_IMAGE_SIZE = 5L * 1024L * 1024L;
  private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");

  @Value("${app.upload.dir}")
  private String uploadDir;

  public String store(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      return null;
    }

    if (file.getSize() > MAX_IMAGE_SIZE) {
      throw new IllegalArgumentException("Image file must be 5 MB or smaller");
    }

    String contentType = file.getContentType();
    if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
      throw new IllegalArgumentException("Only image files can be uploaded");
    }

    String originalName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
    String extension = originalName.contains(".")
            ? originalName.substring(originalName.lastIndexOf('.')).toLowerCase(Locale.ROOT)
            : "";

    if (!ALLOWED_EXTENSIONS.contains(extension)) {
      throw new IllegalArgumentException("Allowed image types: JPG, JPEG, PNG, WEBP and GIF");
    }

    try {
      Path uploadPath = Paths.get(uploadDir);
      if (!Files.exists(uploadPath)) {
        Files.createDirectories(uploadPath);
      }

      String fileName = UUID.randomUUID() + extension;
      Path targetPath = uploadPath.resolve(fileName).normalize();
      if (!targetPath.startsWith(uploadPath.toAbsolutePath().normalize()) && uploadPath.isAbsolute()) {
        throw new IllegalArgumentException("Invalid upload path");
      }

      Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
      return "/uploads/" + fileName;
    } catch (IllegalArgumentException ex) {
      throw ex;
    } catch (Exception e) {
      throw new RuntimeException("Upload failed: " + e.getMessage(), e);
    }
  }
}
