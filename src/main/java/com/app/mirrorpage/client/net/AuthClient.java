package com.app.mirrorpage.client.net;

import com.app.mirrorpage.app.dto.auth.LoginRequest;
import com.app.mirrorpage.app.dto.auth.LoginResponse;
import com.app.mirrorpage.app.framework.Log;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class AuthClient {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper om = new ObjectMapper();
    private final String baseUrl;

    public AuthClient(String baseUrl) {
        // remove barra final se houver
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public LoginResponse login(String user, String pass) throws Exception {
        var body = om.writeValueAsString(new LoginRequest(user, pass));
        var uri = URI.create(baseUrl + "/api/auth/login");

        var req = HttpRequest.newBuilder(uri)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            int status = resp.statusCode();

            if (status == 200) {
                return om.readValue(resp.body(), LoginResponse.class);
            }

            if (status == 401 || status == 403) {
                throw new AuthException("Credenciais inválidas (" + status + ")");
            }

            throw new AuthException("Falha no login (" + status + "): " + resp.body());

        } catch (UnknownHostException | ConnectException e) {
            Log.registrarErro_noEx("Servidor não encontrado: " + e.getMessage());
            throw new IOException("Não foi possível conectar ao servidor: " + e.getMessage(), e);

        } catch (IOException e) {
            Log.registrarErro_noEx("Erro de rede: " + e.getMessage());
            throw e;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.registrarErro_noEx("Login interrompido");
            throw e;
        }
    }

    public static class AuthException extends Exception {
        public AuthException(String msg) { super(msg); }
    }
}
