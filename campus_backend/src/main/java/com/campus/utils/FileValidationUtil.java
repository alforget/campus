package com.campus.utils;
/**
 * 文件校验工具类
 */

import com.campus.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class FileValidationUtil {

        // 常见文件类型的魔数（十六进制字符串）
        private static final Map<String, String> MAGIC_NUMBERS = Map.of(
                "jpg", "FFD8FF",
                "jpeg", "FFD8FF",
                "png", "89504E47",
                "pdf", "25504446"   // %PDF
        );

        public static void validate(MultipartFile file,
                                    List<String> allowedExtensions,
                                    List<String> allowedContentTypes) throws IOException {

            // 1. 基础校验
            if (file == null || file.isEmpty()) {
                throw new BusinessException("文件不能为空");
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.contains(".")) {
                throw new BusinessException("文件名为空或缺少后缀");
            }

            // 2. 后缀白名单（统一转小写比对）
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            if (!allowedExtensions.contains(extension)) {
                throw new BusinessException("不支持的后缀: " + extension);
            }

            // 3. MIME 类型白名单（辅助校验，可被伪造，但作为第一道筛子）
            String contentType = file.getContentType();
            if (contentType == null || !allowedContentTypes.contains(contentType.toLowerCase())) {
                throw new BusinessException("不支持的 MIME 类型: " + contentType);
            }

            // 4. 文件头魔数校验（核心，读取前几个字节）
            String expectedMagic = MAGIC_NUMBERS.get(extension);
            if (expectedMagic == null) {
                throw new BusinessException("该后缀未配置魔数规则: " + extension);
            }

            try (InputStream in = file.getInputStream()) {
                byte[] header = new byte[expectedMagic.length() / 2]; // 每两个十六进制字符代表一个字节
                if (in.read(header) != header.length) {
                    throw new BusinessException("文件过小或读取失败");
                }
                String actualMagic = bytesToHex(header);
                if (!actualMagic.equalsIgnoreCase(expectedMagic)) {
                    throw new BusinessException("文件内容与后缀不匹配，可能被伪造");
                }
            }
            //5.文件大小校验
            long maxSize = 5 * 1024 * 1024; // 5MB
            if (file.getSize() > maxSize) {
                throw new BusinessException("文件不能超过 5MB");
            }
        }

        private static String bytesToHex(byte[] bytes) {
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02X", b));
            }
            return sb.toString();
        }

}
