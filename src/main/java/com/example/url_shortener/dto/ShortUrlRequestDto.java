package com.example.url_shortener.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
public class ShortUrlRequestDto {

    @NotEmpty(message = "please provide valid url")
    @URL(message = "provide a valid url")
    private String longUrl;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime expiredAt;
}
