package com.app.mirrorpage.app.dto.auth;

import java.util.List;

public class LoginResponse {

    public String accessToken;
    public String refreshToken;
    public long exp;             // epoch seconds
    public List<String> roles;     // ["ROLE_REDACAO", ...]
}
