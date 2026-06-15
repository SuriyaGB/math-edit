package com.gbrit.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestDTO {
    private String userName;
    private String password;
    private String email;
    private boolean forcePassword;
}
