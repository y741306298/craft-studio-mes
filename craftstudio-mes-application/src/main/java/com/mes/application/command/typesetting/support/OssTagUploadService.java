package com.mes.application.command.typesetting.support;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.piliofpala.craftstudio.shared.infra.cloud.platforms.alicloud.AliCloudAuthService;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@Service
public class OssTagUploadService {
    private final AliCloudAuthService aliCloudAuthService;
    @Value("${ali-cloud.oss.endpoint:${spring.cloud.alicloud.oss.endpoint:}}")
    private String ossEndpoint;
    @Value("${ali-cloud.oss.raw-bucket:${spring.cloud.alicloud.oss.bucket-name:}}")
    private String defaultBucket;
    @Value("${ali-cloud.oss.save-path:}")
    private String ossSavePath;

    public OssTagUploadService(AliCloudAuthService aliCloudAuthService) {
        this.aliCloudAuthService = aliCloudAuthService;
    }

    public String uploadTagSvg(String businessId, byte[] bytes) {
        return uploadTagFile(businessId, bytes, "svg", "image/svg+xml");
    }

    public String uploadTagPng(String businessId, byte[] bytes) {
        return uploadTagFile(businessId, bytes, "png", "image/png");
    }

    public String uploadTagPng(String businessId, byte[] bytes, String subDir) {
        return uploadTagFile(businessId, bytes, "png", "image/png", subDir);
    }

    public String uploadLagPng(String businessId, byte[] bytes) {
        return uploadTagFile(businessId, bytes, "png", "image/png", "lag");
    }

    private String uploadTagFile(String businessId, byte[] bytes, String extension, String contentType) {
        return uploadTagFile(businessId, bytes, extension, contentType, "tag");
    }

    private String uploadTagFile(String businessId, byte[] bytes, String extension, String contentType, String subDir) {
        Object tempAuthConfig = aliCloudAuthService.getObjectStorageTempAuthConfig(businessId);
        JSONObject tempAuthJson = JSON.parseObject(JSON.toJSONString(tempAuthConfig));
        JSONObject stsToken = tempAuthJson.getJSONObject("stsToken");
        if (stsToken == null) {
            throw new IllegalStateException("获取 OSS STS Token 失败");
        }
        String accessKeyId = stsToken.getString("accessKeyId");
        String accessKeySecret = stsToken.getString("accessKeySecret");
        String securityToken = stsToken.getString("securityToken");
        String bucket = defaultBucket;
        String objectKey = buildTagObjectKey(extension, subDir);
        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build("https://" + ossEndpoint, accessKeyId, accessKeySecret, securityToken);
            ObjectMetadata metadata = new ObjectMetadata();
            if (StringUtils.isNotBlank(contentType)) {
                metadata.setContentType(contentType);
            }
            ossClient.putObject(bucket, objectKey, new ByteArrayInputStream(bytes), metadata);
            return buildCompleteOssUrl(objectKey);
        } catch (Exception e) {
            throw new IllegalStateException("上传标签条到 OSS 失败", e);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    private String buildTagObjectKey(String extension, String subDir) {
        StringBuilder keyBuilder = new StringBuilder();
        if (StringUtils.isNotBlank(ossSavePath)) {
            keyBuilder.append(trimSlashes(ossSavePath)).append("/");
        }
        keyBuilder.append(trimSlashes(subDir)).append("/").append(UUID.randomUUID()).append(".").append(extension);
        return keyBuilder.toString();
    }

    private String trimSlashes(String value) {
        String result = value;
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String buildCompleteOssUrl(String imageUrl) {
        if (StringUtils.isBlank(imageUrl)) {
            return imageUrl;
        }
        String trimmed = imageUrl.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        String normalizedPath = trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
        if (StringUtils.isBlank(defaultBucket) || StringUtils.isBlank(ossEndpoint)) {
            return normalizedPath;
        }
        return "https://" + defaultBucket + "." + ossEndpoint + "/" + normalizedPath;
    }
}
