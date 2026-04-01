package com.example.url_shortener.controller;

import com.example.url_shortener.dto.ErrorResponseDto;
import com.example.url_shortener.exception.UrlNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(UrlNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleUrlNotFoundException(UrlNotFoundException ex, HttpServletRequest request){

        ErrorResponseDto responseDto=new ErrorResponseDto(
                404,
                "Not Found",
                ex.getMessage(),
                request.getRequestURI(),
                Instant.now()
        );

        log.error(ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(responseDto);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {



        ErrorResponseDto response = new ErrorResponseDto(
                400,
                "BAD_REQUEST",
                ex.getMessage(),
                request.getRequestURI(),
                Instant.now()
        );

        return ResponseEntity.badRequest().body(response);
    }

}
