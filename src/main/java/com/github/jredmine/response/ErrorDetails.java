package com.github.jredmine.response;

import lombok.Data;

@Data
public class ErrorDetails {
    private String timestamp;
    private int status;
    private String error;
    private String path;
}
