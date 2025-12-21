package com.github.jredmine.dto.response;

import lombok.Data;

@Data
public class ApiResponse<T> {

    private String status;
    private String message;
    private T data;
    private ErrorDetails error;

    public ApiResponse(String status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public ApiResponse(String status, String message, ErrorDetails error) {
        this.status = status;
        this.message = message;
        this.error = error;
    }
}

