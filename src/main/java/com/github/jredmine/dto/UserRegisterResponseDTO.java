package com.github.jredmine.dto;

import lombok.Data;

@Data
public class UserRegisterResponseDTO {
    private String login;
    private String firstname;
    private String lastname;
}
