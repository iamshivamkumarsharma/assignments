package org.nbfc.productwa.service;

import org.nbfc.productwa.dto.LoginRequest;
import org.nbfc.productwa.dto.LoginResponse;
import org.nbfc.productwa.dto.UserRegisterRequest;
import org.nbfc.productwa.dto.UserResponse;

public interface AuthService {

    UserResponse register(UserRegisterRequest request);

    LoginResponse login(LoginRequest request);
}
