package com.campus.controller;

import com.campus.entitys.Result;
import com.campus.exception.BusinessException;
import com.campus.service.RoleService;
import com.campus.utils.CurrentUserInfoUtil;
import com.campus.utils.FileValidationUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/upload")
public class UplodFileController {

    @Value("${real.path.avatar}")
    private String realPathAvatar;
    @Value("${web.prefix.avatar}")
    private String webPrefixAvatar;

    @Value("${real.path.logo}")
    private String realPathLogo;
    @Value("${web.prefix.logo}")
    private String webPrefixLogo;

    // 新增活动封面配置
    @Value("${real.path.cover}")
    private String realPathCover;
    @Value("${web.prefix.cover}")
    private String webPrefixCover;

    @Value("${upload.allowed-extensions}")
    private List<String> allowedExtensions;
    @Value("${upload.allowed-content-types}")
    private List<String> allowedContentTypes;
    @Autowired
    private RoleService roleService;

    /**
     * 上传头像 (登录用户均可)
     */
    @PostMapping("/uploadAvatar")
    public Result<String> uploadAvatarFile(@RequestParam("file") MultipartFile file,
                                           HttpServletRequest request) throws IOException {
        // 头像通常是所有用户都能传，无需额外角色校验
        String url = uploadFile(file, realPathAvatar, webPrefixAvatar, request);
        return Result.success(url);
    }

    /**
     * 上传社团 Logo (仅社团管理员)
     */
    @PostMapping("/uploadLogo")
    public Result<String> uploadLogoFile(@RequestParam("file") MultipartFile file,
                                         HttpServletRequest request) throws IOException {
        checkClubAdminRole();
        String url = uploadFile(file, realPathLogo, webPrefixLogo, request);
        return Result.success(url);
    }

    /**
     * 上传活动封面 (仅社团管理员)
     */
    @PostMapping("/uploadCover")
    public Result<String> uploadCoverFile(@RequestParam("file") MultipartFile file,
                                          HttpServletRequest request) throws IOException {
        checkClubAdminRole();
        String url = uploadFile(file, realPathCover, webPrefixCover, request);
        return Result.success(url);
    }

    // ================= 抽离出的公共上传方法 =================

    private String uploadFile(MultipartFile file, String baseDir, String webPrefix,
                              HttpServletRequest request) throws IOException {
        // 1. 校验文件类型（扩展名 & MIME 类型，确保只允许图片）
        FileValidationUtil.validate(file, allowedExtensions, allowedContentTypes);

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BusinessException("文件名不合法");
        }

        // 2. 生成唯一文件名
        String suffix = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        String fileName = UUID.randomUUID().toString().replace("-", "") + suffix;

        // 3. 确保目录存在 (Files.createDirectories 会自动创建多级目录)
        Path dirPath = Paths.get(baseDir);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath); // 目录不存在就现场建
        }

        // 4. 保存文件
        File dest = dirPath.resolve(fileName).toFile();
        file.transferTo(dest);

        // 5. 动态拼接域名 (获取当前请求的协议、IP、端口)
        String baseUrl = ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath(null)
                .build()
                .toUriString();

        return baseUrl + webPrefix + fileName;
    }

    /**
     * 校验当前用户是否是社团管理员
     */
    private void checkClubAdminRole() {
        Long roleId = CurrentUserInfoUtil.getRoleId();
        Long clubAdminRoleId = roleService.getRoleId("clubAdmin");
        if (!roleId.equals(clubAdminRoleId)) {
            throw new BusinessException("只有社团管理员才能上传此图片");
        }
    }
}
