package com.joyboy.polaris_digitech.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class BoxUpdateRequest {

    @NotEmpty(message = "itemCodes must not be empty")
    private List<String> itemCodes;
}