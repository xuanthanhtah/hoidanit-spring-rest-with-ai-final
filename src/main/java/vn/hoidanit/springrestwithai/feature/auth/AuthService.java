package vn.hoidanit.springrestwithai.feature.auth;

import vn.hoidanit.springrestwithai.feature.auth.dto.LoginRequest;
import vn.hoidanit.springrestwithai.feature.auth.dto.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
}
