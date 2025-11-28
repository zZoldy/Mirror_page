package com.app.mirrorpage.client.net;

import com.app.mirrorpage.client.dto.ChangeBatchDto;
import com.app.mirrorpage.client.dto.TreeNodeDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;   // [ADD]
import java.util.function.Supplier;   // [ADD]

public class ApiClient {

    // coloque aqui ↓↓↓
    public static class LockResult {

        public final boolean granted;
        public final String owner;

        public LockResult(boolean granted, String owner) {
            this.granted = granted;
            this.owner = owner;
        }
    }

    // 👇 ADICIONE ISSO AQUI
    public static class LockConflictResponse {

        public String message;
        public String owner;

        // Jackson precisa de construtor default
        public LockConflictResponse() {
        }

        public String getMessage() {
            return message;
        }

        public String getOwner() {
            return owner;
        }
    }

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    private final String baseUrl;   // sempre sem barra final e com esquema

    /**
     * Sempre guardar aqui apenas o token "cru", sem o prefixo "Bearer ". O
     * prefixo é adicionado somente em addAuth().
     */
    private volatile String bearer; // [CHG] interpretado como token cru (sem "Bearer ")

    // fornecedores/callback para refresh automático
    private final Supplier<String> refreshTokenProvider;
    private final Consumer<String> onTokenRefreshed;

    private final ObjectMapper json = new ObjectMapper();

    // ====== Construtores ======
    public ApiClient(String baseUrl) {
        this(baseUrl, (String) null);
    }

    public ApiClient(String baseUrl, String bearer) {
        this(baseUrl, bearer, null, null); // compat
    }

    // preferido: com supplier/callback
    public ApiClient(String baseUrl, String bearer,
            Supplier<String> refreshTokenProvider,
            Consumer<String> onTokenRefreshed) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        setBearer(bearer); // [CHG] usa o normalizador abaixo
        this.refreshTokenProvider = refreshTokenProvider;
        this.onTokenRefreshed = onTokenRefreshed;
    }

    // Permite atualizar o token após login/refresh manual
    public void setBearer(String bearer) {
        if (bearer == null || bearer.isBlank()) {
            this.bearer = null;
        } else if (bearer.startsWith("Bearer ")) { // [CHG] se vier com prefixo, remove
            this.bearer = bearer.substring("Bearer ".length()).trim();
        } else {
            this.bearer = bearer.trim();
        }
    }

    // ====== API pública ======
    public List<TreeNodeDto> getTree(String path) throws IOException, InterruptedException {
        String p = normalizePath(path);
        String qs = URLEncoder.encode(p, StandardCharsets.UTF_8);
        String endpoint = "/api/tree?path=" + qs;

        HttpResponse<String> resp = sendGETWithAutoRefresh(endpoint);
        return mapper.readValue(resp.body(), new TypeReference<List<TreeNodeDto>>() {
        });
    }

    public String get(String endpoint) throws IOException, InterruptedException {
        HttpResponse<String> resp = sendGETWithAutoRefresh(endpoint);
        return resp.body();
    }

    // novo método para salvar arquivo na árvore
    public void saveFile(String path, String content) throws IOException, InterruptedException {
        final String norm = normalizePath(path);
        final String url = baseUrl + "/api/tree/file";
        String json = mapper.writeValueAsString(Map.of("path", norm, "content", content == null ? "" : content));

        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json; charset=utf-8")
                .header("User-Agent", "MirrorPage-Client/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
        addAuth(b);

        HttpResponse<String> resp = sendWithAutoRefresh(b, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ensure2xx(resp, url);
    }

    public String readCsv(String relativePath) throws IOException, InterruptedException {
        String p = (relativePath == null || relativePath.isBlank()) ? "/" : relativePath.trim();
        String qs = URLEncoder.encode(p, StandardCharsets.UTF_8);
        String url = baseUrl + "/api/file/read?path=" + qs;

        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "text/plain; charset=utf-8")
                .header("User-Agent", "MirrorPage-Client/1.0");
        addAuth(b);

        HttpResponse<String> resp = sendWithAutoRefresh(
                b.GET(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        ensure2xx(resp, url);

        return resp.body(); // CSV inteiro
    }

    public ChangeBatchDto getTreeChanges(long since) throws IOException, InterruptedException {
        String url = baseUrl + "/api/tree/changes?since=" + since;
        HttpRequest.Builder b = baseRequest(url).GET();
        HttpResponse<String> resp = sendWithAutoRefresh(b, HttpResponse.BodyHandlers.ofString());
        ensure2xx(resp, url);
        return mapper.readValue(resp.body(), ChangeBatchDto.class);
    }

    public void postVoid(String endpoint) throws Exception {
        String url = baseUrl + (endpoint.startsWith("/") ? endpoint : "/" + endpoint);
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.noBody());
        addAuth(b);

        HttpResponse<Void> resp = sendWithAutoRefresh(b, HttpResponse.BodyHandlers.discarding());
        if (resp.statusCode() >= 300) {
            throw new ApiHttpException(resp.statusCode(), "POST falhou em " + url);
        }
    }

    // ====== Internals ======
    private HttpResponse<String> sendGet(String endpoint) throws IOException, InterruptedException {
        String url = buildUrl(endpoint);
        HttpRequest.Builder b = baseRequest(url).GET();
        HttpRequest req = b.build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        ensure2xx(resp, url);
        return resp;
    }

    // GET com auto-refresh
    private HttpResponse<String> sendGETWithAutoRefresh(String endpoint) throws IOException, InterruptedException {
        String url = buildUrl(endpoint);
        HttpRequest.Builder b = baseRequest(url).GET();
        HttpResponse<String> resp = sendWithAutoRefresh(b, HttpResponse.BodyHandlers.ofString());
        ensure2xx(resp, url);
        return resp;
    }

    private HttpRequest.Builder baseRequest(String url) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("User-Agent", "MirrorPage-Client/1.0");
        addAuth(b);
        return b;
    }

    private void addAuth(HttpRequest.Builder b) {
        String token = this.bearer;
        if (token != null && !token.isBlank()) {
            b.header("Authorization", "Bearer " + token);
        } else {
            System.out.println("[ApiClient] SEM Authorization (bearer vazio/null)");
        }
    }

    private <T> HttpResponse<T> sendWithAutoRefresh(HttpRequest.Builder builder,
            HttpResponse.BodyHandler<T> handler)
            throws IOException, InterruptedException {

        HttpResponse<T> resp = http.send(builder.build(), handler);
        int code = resp.statusCode();

        // só tenta refresh se for 401 OU 403
        if (code != 401 && code != 403) {
            return resp;
        }

        if (refreshTokenProvider == null || onTokenRefreshed == null) {
            return resp;
        }

        try {
            String newAccess = refreshAccessToken(refreshTokenProvider.get());
            if (newAccess != null && !newAccess.isBlank()) {
                this.bearer = newAccess;
                onTokenRefreshed.accept(newAccess);

                // 🔴 AQUI: usar setHeader em vez de header
                HttpRequest retry = builder
                        .setHeader("Authorization", "Bearer " + newAccess)
                        .build();

                return http.send(retry, handler);
            }
        } catch (Exception ignore) {
            // se der ruim no refresh, a resposta original (401/403) sobe
        }
        return resp;
    }

    // chama /api/auth/refresh
    private String refreshAccessToken(String refreshToken) throws IOException, InterruptedException {
        if (refreshToken == null || refreshToken.isBlank()) {
            return null;
        }

        String url = baseUrl + "/api/auth/refresh";
        String json = mapper.writeValueAsString(Map.of("refreshToken", refreshToken));

        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            Map<String, String> data = mapper.readValue(resp.body(), new TypeReference<>() {
            });
            return data.get("accessToken"); // token cru
        }
        return null;
    }

    private void ensure2xx(HttpResponse<?> resp, String url) {
        int code = resp.statusCode();
        if (code >= 200 && code < 300) {
            return;
        }
        String body = null;
        try {
            body = (resp.body() == null) ? null : resp.body().toString();
        } catch (Exception ignored) {
        }
        throw new ApiHttpException(code, "HTTP " + code + " em " + url + (body != null ? " → " + body : ""));
    }

    private static String buildUrlRoot(String base) {
        String u = (base == null) ? "" : base.trim();
        if (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        return u;
    }

    private String buildUrl(String endpoint) {
        String ep = Objects.requireNonNull(endpoint, "endpoint");
        if (!ep.startsWith("/")) {
            ep = "/" + ep;
        }
        return baseUrl + ep;
    }

    private static String normalizeBaseUrl(String baseUrl) {
        String u = buildUrlRoot(baseUrl);
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = "http://" + u; // default dev
        }
        return u;
    }

    private static String normalizePath(String p) {
        if (p == null || p.isBlank()) {
            return "/";
        }
        if ("/Produtos".equals(p)) {
            return "/";
        }
        if (p.startsWith("/Produtos/")) {
            p = p.substring("/Produtos".length());
        }
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        return p.replaceAll("/{2,}", "/");
    }

    // ====== Manipulação de Tema ======
    // [CHG] agora usa addAuth + sendWithAutoRefresh + ensure2xx
    public <T> T get(String path, Class<T> type) throws Exception {
        String url = baseUrl + path;
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json");
        addAuth(b);

        var resp = sendWithAutoRefresh(b, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ensure2xx(resp, url);
        return json.readValue(resp.body(), type);
    }

    // [CHG] idem para PUT
    public void put(String path, Object body) throws Exception {
        String url = baseUrl + path;
        String payload = json.writeValueAsString(body);

        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json; charset=utf-8");
        addAuth(b);

        HttpRequest req = b.PUT(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8)).build();
        var resp = sendWithAutoRefresh(HttpRequest.newBuilder(req.uri())
                .timeout(REQUEST_TIMEOUT)
                .headers(req.headers().map().entrySet().stream()
                        .flatMap(e -> e.getValue().stream().map(v -> Map.entry(e.getKey(), v)))
                        .flatMap(e -> java.util.stream.Stream.of(e.getKey(), e.getValue()))
                        .toArray(String[]::new))
                .PUT(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8)),
                HttpResponse.BodyHandlers.discarding());

        if (resp.statusCode() >= 300) {
            throw new RuntimeException("PUT " + path + " => " + resp.statusCode());
        }
    }

    // ====== Exception ======
    public static class ApiHttpException extends RuntimeException {

        private final int statusCode;

        public ApiHttpException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public boolean isUnauthorized() {
            return statusCode == 401;
        }

        public boolean isForbidden() {
            return statusCode == 403;
        }

        public boolean isNotFound() {
            return statusCode == 404;
        }

        public boolean isServerError() {
            return statusCode >= 500;
        }
    }

    /**
     * Tenta aplicar LOCK numa célula.
     *
     * @return true se o lock foi concedido, false se já está bloqueada por
     * outro usuário.
     */
    public ApiClient.LockResult tryLockCell(String docPath, int row, int col)
            throws IOException, InterruptedException {

        String norm = normalizePath(docPath);
        String url = baseUrl + "/api/sheet/lock";

        Map<String, Object> body = Map.of(
                "path", norm,
                "row", row,
                "col", col
        );

        String payload = json.writeValueAsString(body);

        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json");
        addAuth(b);

        HttpResponse<String> resp = sendWithAutoRefresh(
                b.POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8)),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        int status = resp.statusCode();
        String respBody = resp.body();

        // ✅ SUCESSO: lock concedido
        if (status == 200) {
            return new LockResult(true, null);
        }

        // ⚠ CONFLITO: célula já está bloqueada
        if (status == 409) {
            String owner = null;

            String trimmed = (respBody == null ? "" : respBody.trim());

            if (!trimmed.isEmpty() && trimmed.startsWith("{")) {
                // Parece JSON → tenta parsear como LockConflictResponse
                try {
                    LockConflictResponse conflict
                            = json.readValue(trimmed, LockConflictResponse.class);
                    owner = conflict.getOwner();
                } catch (Exception ex) {
                    // Se quebrar, apenas loga e segue sem owner
                    ex.printStackTrace();
                }
            } else {
                // Resposta simples tipo: "Cell already locked by another user"
                System.out.println("[DEBUG] /lock 409 corpo não-JSON: " + trimmed);
            }

            return new LockResult(false, owner);
        }

        // Outros erros (403, 500, etc.) → lança ApiHttpException
        ensure2xx(resp, url);
        return new LockResult(false, null);
    }

    /**
     * Apenas libera o lock da célula, sem salvar (caso ESC / cancelar edição).
     */
    public void unlockCell(String docPath, int row, int col) throws IOException, InterruptedException {
        String norm = normalizePath(docPath);
        if (!norm.startsWith("/")) {
            norm = "/" + norm;
        }
        String url = baseUrl + "/api/sheet/unlock";

        Map<String, Object> body = Map.of(
                "path", norm,
                "row", row,
                "col", col
        );
        String payload = json.writeValueAsString(body);

        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json");
        addAuth(b);

        HttpResponse<String> resp = sendWithAutoRefresh(
                b.POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8)),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        ensure2xx(resp, url);
    }

    public String getWebSocketUrl() {
        if (baseUrl.startsWith("https://")) {
            return "wss://" + baseUrl.substring(8) + "/ws";
        } else if (baseUrl.startsWith("http://")) {
            return "ws://" + baseUrl.substring(7) + "/ws";
        } else {
            return "ws://" + baseUrl + "/ws";
        }
    }

    public void saveCell(String docPath, int row, int col, String value)
            throws IOException, InterruptedException {

        String norm = normalizePath(docPath); // mantém padrão do seu lock/unlock
        if (!norm.startsWith("/")) {
            norm = "/" + norm;
        }
        String url = baseUrl + "/api/sheet/save-cell";

        Map<String, Object> body = Map.of(
                "path", norm,
                "row", row,
                "col", col,
                "value", value == null ? "" : value
        );

        String payload = json.writeValueAsString(body);

        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json");

        addAuth(b); // adiciona Authorization: Bearer TOKEN

        HttpResponse<String> resp = sendWithAutoRefresh(
                b.POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8)),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        ensure2xx(resp, url); // lança ApiHttpException se não for OK
    }

    public void insertRow(String path, int afterRow) throws IOException, InterruptedException {
        final String norm = normalizePath(path);

        String url = baseUrl + "/api/sheet/row/insert"
                + "?path=" + URLEncoder.encode(norm, StandardCharsets.UTF_8)
                + "&afterRow=" + afterRow;

        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "*/*") // pode ser application/json, mas aqui nem tem corpo mesmo
                .header("User-Agent", "MirrorPage-Client/1.0")
                .POST(HttpRequest.BodyPublishers.noBody()); // sem corpo

        addAuth(b);

        HttpResponse<Void> resp = sendWithAutoRefresh(b, HttpResponse.BodyHandlers.discarding());
        ensure2xx(resp, url);
    }

    public void deleteRow(String path, int row) throws IOException, InterruptedException {
        final String norm = normalizePath(path);
        final String url = baseUrl + "/api/sheet/row/delete";

        String json = mapper.writeValueAsString(Map.of(
                "path", norm,
                "row", row
        ));

        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json; charset=utf-8")
                .header("User-Agent", "MirrorPage-Client/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));

        addAuth(b);

        HttpResponse<String> resp
                = sendWithAutoRefresh(b, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        ensure2xx(resp, url);
    }

    public void moveRow(String path, int from, int to)
            throws IOException, InterruptedException {

        String url = baseUrl + "/api/sheet/moveRow";
        String json = mapper.writeValueAsString(
                Map.of("path", path, "from", from, "to", to)
        );

        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .header("User-Agent", "MirrorPage-Client/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));

        addAuth(b);

        HttpResponse<String> resp
                = sendWithAutoRefresh(b, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        ensure2xx(resp, url);
    }

    public String loadSheet(String path) throws IOException, InterruptedException {
        final String norm = normalizePath(path);
        final String url = baseUrl + "/api/sheet?path=" + URLEncoder.encode(norm, StandardCharsets.UTF_8);

        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "text/plain")
                .header("User-Agent", "MirrorPage-Client/1.0")
                .GET();

        addAuth(b);

        HttpResponse<String> resp
                = sendWithAutoRefresh(b, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        ensure2xx(resp, url);

        return resp.body();
    }
}
