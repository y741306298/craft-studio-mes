package com.mes.application.command.api.vo;

import com.piliofpala.craftstudio.shared.infra.cloud.storage.dto.ObjectStorageTempAuthConfig;
import lombok.Data;

@Data
public class UploadConfig {
    private ObjectStorageTempAuthConfig ossConfig;
    private String uploadPath;

}