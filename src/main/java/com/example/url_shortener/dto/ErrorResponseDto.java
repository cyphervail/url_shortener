package com.example.url_shortener.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ErrorResponseDto {
    private int status;
    private String error;
    private String message;
    private String path;
    private Instant timestamp;
}
