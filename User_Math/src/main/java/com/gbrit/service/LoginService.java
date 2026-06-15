package com.gbrit.service;

import com.gbrit.dto.LoginRequestDTO;
import com.gbrit.exception.APIResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public interface LoginService {
    ResponseEntity<APIResponse> login(LoginRequestDTO loginRequestDTO);
}
