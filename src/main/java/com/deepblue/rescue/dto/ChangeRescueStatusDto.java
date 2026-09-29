package com.deepblue.rescue.dto;

import com.deepblue.rescue.domain.RescueStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeRescueStatusDto(@NotNull RescueStatus newStatus) {
}
