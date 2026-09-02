package com.joyboy.polaris_digitech.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ItemRequest {

    @NotBlank(message = "Item name is required")
    @Pattern(
            regexp = "^[a-zA-Z0-9_-]+$",
            message = "Name can only contain letters, numbers, hyphen and underscore"
    )
    private String name;

    @NotNull(message = "Weight is required")
    @Min(value = 1, message = "Weight must be at least 1gr")
    private Integer weight;

    @NotBlank(message = "Item code is required")
    @Pattern(
            regexp = "^[A-Z0-9_]+$",
            message = "Code can only contain uppercase letters, numbers and underscore"
    )
    private String code;
}