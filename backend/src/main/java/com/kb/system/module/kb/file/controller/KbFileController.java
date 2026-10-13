package com.kb.system.module.kb.file.controller;

import com.kb.system.aop.annotation.OperationLog;
import com.kb.system.common.OperationType;
import com.kb.system.common.Result;
import com.kb.system.module.kb.file.service.KbFileService;
import com.kb.system.module.kb.file.vo.FileVO;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 附件文件控制器：上传 / 下载 / 删除
 */
@RestController
@RequestMapping("/kb/files")
public class KbFileController {

    private final KbFileService fileService;

    public KbFileController(KbFileService fileService) {
        this.fileService = fileService;
    }

    @OperationLog(module = "附件管理", type = OperationType.UPLOAD, description = "上传附件")
    @PreAuthorize("hasAuthority('kb:file:upload')")
    @PostMapping
    public Result<FileVO> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(fileService.upload(file));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) throws Exception {
        KbFileService.FileResource fileResource = fileService.load(id);
        String encodedName = URLEncoder.encode(fileResource.originalName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        fileResource.contentType() == null
                                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                                : fileResource.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedName)
                .body(fileResource.resource());
    }

    @OperationLog(module = "附件管理", type = OperationType.DELETE, description = "删除附件")
    @PreAuthorize("hasAuthority('kb:file:delete')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        fileService.delete(id);
        return Result.ok();
    }
}
