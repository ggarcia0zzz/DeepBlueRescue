package com.deepblue.rescue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssignTrackingDeviceDto(@NotBlank @Size(max = 50) String trackingDeviceCode) {
}
