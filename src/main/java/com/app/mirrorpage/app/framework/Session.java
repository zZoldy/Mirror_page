/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.app.mirrorpage.app.framework;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Session {

    private final String baseUrl;
    private String accessToken;
    private final String username;
    private final String refreshToken;
    private final Set<String> roles;

    public Session(String baseUrl, String accessToken, String refreshToken, String username, Collection<String> roles) {
        String u = baseUrl == null ? "" : baseUrl.trim();
        if (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        this.baseUrl = u;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.username = username;
        this.roles = new HashSet<>(roles == null ? List.of() : roles);
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String accessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String username() {
        return username;
    }

    public String refreshToken() {
        return refreshToken;
    }

    public Set<String> roles() {
        return Collections.unmodifiableSet(roles);
    }

    public boolean isAdmin() {
        return roles.contains("ROLE_SUPORTE");
    }

    public boolean canEdit() {
        return roles.contains("ROLE_SUPORTE") || roles.contains("ROLE_REDACAO");
    }

    public boolean canView() {
        return !roles.isEmpty();
    }
}
