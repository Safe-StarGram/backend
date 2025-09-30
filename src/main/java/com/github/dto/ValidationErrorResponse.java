package com.github.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationErrorResponse {
    private int status;
    private String error;
    private String message;
    private Map<String, String> errors;
}

