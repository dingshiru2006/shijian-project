package com.kb.system.module.kb.file.service;

import com.kb.system.common.BusinessException;
import com.kb.system.module.kb.document.repository.KbDocumentRepository;
import com.kb.system.module.kb.file.entity.KbFile;
import com.kb.system.module.kb.file.repository.KbFileRepository;
import com.kb.system.module.kb.file.vo.FileVO;
import com.kb.system.security.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 附件文件服务：上传 / 下载 / 删除（本地磁盘存储）
 */
@Service
public class KbFileService {

    private final KbFileRepository fileRepository;
    private final KbDocumentRepository documentRepository;
    private final CurrentUser currentUser;

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    @Value("${app.file.max-size}")
    private long maxSize;

    public KbFileService(KbFileRepository fileRepository,
                         KbDocumentRepository documentRepository,
                         CurrentUser currentUser) {
        this.fileRepository = fileRepository;
        this.documentRepository = documentRepository;
        this.currentUser = currentUser;
    }

    /**
     * 上传文件：按 年/月 分目录存储，元数据入库
     */
    @Transactional
    public FileVO upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        if (file.getSize() > maxSize) {
            throw new BusinessException("文件大小超过限制（" + (maxSize / 1024 / 1024) + "MB）");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new BusinessException("文件名不能为空");
        }
        String sanitized = originalName.replaceAll("[\\\\/:*?\"<>|]", "_");
        String extension = "";
        int dotIndex = sanitized.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = sanitized.substring(dotIndex);
        }
        String subDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String storedName = UUID.randomUUID().toString().replace("-", "") + extension;
        try {
            Path targetDir = Paths.get(uploadDir).resolve(subDir).normalize();
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(storedName).normalize();
            // 路径穿越防护：确保最终路径位于上传目录内
            if (!targetPath.startsWith(Paths.get(uploadDir).toAbsolutePath().normalize())) {
                throw new BusinessException("非法的文件路径");
            }
            file.transferTo(targetPath.toFile());
            KbFile kbFile = new KbFile();
            kbFile.setOriginalName(sanitized);
            kbFile.setStoredName(storedName);
            kbFile.setFilePath(subDir + "/" + storedName);
            kbFile.setFileSize(file.getSize());
            kbFile.setFileType(file.getContentType());
            kbFile.setUploaderId(currentUser.id());
            return FileVO.from(fileRepository.save(kbFile));
        } catch (IOException e) {
            throw new BusinessException("文件保存失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件资源（下载/预览）
     */
    public FileResource load(Long id) {
        KbFile file = fileRepository.findById(id)
                .orElseThrow(() -> new BusinessException("文件不存在"));
        try {
            Path path = Paths.get(uploadDir).resolve(file.getFilePath()).normalize();
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException("文件不存在或已被删除");
            }
            return new FileResource(resource, file.getOriginalName(), file.getFileType());
        } catch (MalformedURLException e) {
            throw new BusinessException("文件路径非法");
        }
    }

    @Transactional
    public void delete(Long id) {
        KbFile file = fileRepository.findById(id)
                .orElseThrow(() -> new BusinessException("文件不存在"));
        long referenced = documentRepository.findByFileId(id).size();
        if (referenced > 0) {
            throw new BusinessException("该文件已被 " + referenced + " 个文档引用，无法删除");
        }
        fileRepository.delete(file);
        try {
            Path path = Paths.get(uploadDir).resolve(file.getFilePath()).normalize();
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // 物理文件删除失败不影响元数据删除结果
        }
    }

    /**
     * 文件下载资源
     */
    public record FileResource(Resource resource, String originalName, String contentType) {
    }
}
