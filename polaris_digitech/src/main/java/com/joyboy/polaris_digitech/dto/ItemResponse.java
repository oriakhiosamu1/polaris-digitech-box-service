package com.joyboy.polaris_digitech.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ItemResponse {
    private Long id;
    private String name;
    private Integer weight;
    private String code;
}