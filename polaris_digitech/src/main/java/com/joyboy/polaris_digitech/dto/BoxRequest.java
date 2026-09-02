package com.joyboy.polaris_digitech.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class BoxRequest {

    @NotBlank(message = "txref is required")
    @Size(max = 20, message = "txref must not exceed 20 characters")
    private String txref;

    @NotNull(message = "weightLimit is required")
    @Min(value = 1, message = "weightLimit must be at least 1gr")
    @Max(value = 500, message = "weightLimit must not exceed 500gr")
    private Integer weightLimit;

    @NotNull(message = "batteryCapacity is required")
    @Min(value = 0, message = "batteryCapacity cannot be less than 0")
    @Max(value = 100, message = "batteryCapacity cannot be more than 100")
    private Integer batteryCapacity;
}