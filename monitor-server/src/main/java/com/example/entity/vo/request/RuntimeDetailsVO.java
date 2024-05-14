package com.example.entity.vo.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RuntimeDetailsVO {
    @NotNull
    long timestamp;
    @NotNull
    double cpuUsage;
    @NotNull
    double memoryUsage;
    @NotNull
    double diskUsage;
    @NotNull
    double networkUpload;
    @NotNull
    double networkDownload;
    @NotNull
    double diskRead;
    @NotNull
    double diskWrite;
}

