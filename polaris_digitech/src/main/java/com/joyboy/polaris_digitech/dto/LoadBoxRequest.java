package com.joyboy.polaris_digitech.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class LoadBoxRequest {

    @NotEmpty(message = "Items list cannot be empty")
    @Valid
    private List<ItemRequest> items;
}