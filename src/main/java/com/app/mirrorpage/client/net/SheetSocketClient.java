package com.app.mirrorpage.client.net;

import com.app.mirrorpage.client.dto.CellChangeEvent;
import com.app.mirrorpage.client.dto.RowDeletedEvent;
import com.app.mirrorpage.client.dto.RowInsertedEvent;
import com.app.mirrorpage.client.dto.RowMoveEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.UUID;
import java.util.function.Consumer;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class SheetSocketClient extends WebSocketClient {

    private final ObjectMapper mapper = new ObjectMapper();

    // Configurações
    private final String topic;
    private final String username; // [NOVO] Guarda o usuário logado
    private final String subscriptionId;

    // Callbacks de eventos
    private final Consumer<CellChangeEvent> onCellChange;
    private final Consumer<RowInsertedEvent> onRowInserted;
    private final Consumer<RowMoveEvent> onRowMoved;
    private final Consumer<RowDeletedEvent> onRowDeleted;
    private final Consumer<String> onConnectionError;

    /**
     * Construtor Principal
     */
    public SheetSocketClient(
            URI serverUri,
            String topic,
            String username, // [NOVO] Recebe o usuário aqui
            Consumer<CellChangeEvent> onCellChange,
            Consumer<RowInsertedEvent> onRowInserted,
            Consumer<RowMoveEvent> onRowMoved,
            Consumer<RowDeletedEvent> onRowDeleted,
            Consumer<String> onConnectionError
    ) {
        super(serverUri);
        this.topic = topic;
        this.username = username;
        this.onCellChange = onCellChange;
        this.onRowInserted = onRowInserted;
        this.onRowMoved = onRowMoved;
        this.onRowDeleted = onRowDeleted;
        this.onConnectionError = onConnectionError;
        this.subscriptionId = "sub-" + UUID.randomUUID();
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("[WS] Conectado ao servidor: " + getURI());

        // 1. CONNECT (Mantém igual)
        String loginHeader = (username != null) ? "login-user:" + username + "\n" : "";
        String connectFrame = "CONNECT\n"
                + "accept-version:1.2\n"
                + "host:localhost\n"
                + loginHeader
                + "\n"
                + "\0";
        send(connectFrame);

        // 2. SUBSCRIBE NO TÓPICO DA PLANILHA (Mantém igual)
        String subData = "SUBSCRIBE\n"
                + "id:" + subscriptionId + "\n"
                + "destination:" + topic + "\n"
                + "\n"
                + "\0";
        send(subData);
        System.out.println("[WS] Ouvindo dados em: " + topic);
    }

    @Override
    public void onMessage(String message) {
        try {
            // Lógica padrão de Parsing do STOMP
            int idx = message.indexOf("\n\n");
            if (idx < 0) {
                return;
            }

            String body = message.substring(idx + 2);
            if (body.endsWith("\0")) {
                body = body.substring(0, body.length() - 1);
            }
            body = body.trim();
            if (body.isEmpty()) {
                return;
            }

            System.out.println("[WS] Mensagem recebida: " + body);

            // Tenta processar como JSON
            JsonNode json = mapper.readTree(body);

            // 1. Mudança de Célula (Edição)
            if (json.has("col") && json.has("row") && json.has("value")) {
                CellChangeEvent ev = mapper.readValue(body, CellChangeEvent.class);
                if (onCellChange != null) {
                    onCellChange.accept(ev);
                }
                return;
            }

            // 2. Linha Deletada
            if (json.has("modelRow")) {
                RowDeletedEvent ev = mapper.readValue(body, RowDeletedEvent.class);
                if (onRowDeleted != null) {
                    onRowDeleted.accept(ev);
                }
                return;
            }

            // 3. Linha Inserida
            if (json.has("afterRow")) {
                RowInsertedEvent ev = mapper.readValue(body, RowInsertedEvent.class);
                if (onRowInserted != null) {
                    onRowInserted.accept(ev);
                }
                return;
            }

            // 4. Linha Movida
            if (json.has("from")) {
                RowMoveEvent ev = mapper.readValue(body, RowMoveEvent.class);
                if (onRowMoved != null) {
                    onRowMoved.accept(ev);
                }
                return;
            }

            // Se for mensagem de sistema (CONNECTED, RECEIPT) ignoramos o log de erro
            if (!message.startsWith("MESSAGE")) {
                // System.out.println("[WS] Frame de controle recebido.");
                return;
            }

            System.out.println("[WS] Evento JSON desconhecido: " + body);

        } catch (Exception e) {
            // Ignora erros de parse se for frame de controle (CONNECTED)
            if (!message.startsWith("CONNECTED")) {
                e.printStackTrace();
                System.out.println("[WS] Erro ao processar mensagem: " + message);
            }
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.printf("[WS] Fechado code=%d reason=%s remote=%s%n", code, reason, remote);

        // Se a conexão fechar e NÃO for um logout intencional (código 1000 geralmente é normal)
        // O servidor geralmente fecha com códigos de erro ou 1006 (Abnormal) quando o Interceptor barra.
        if (code != 1000) {
            if (onConnectionError != null) {
                // Avisa a tela que deu ruim
                onConnectionError.accept("Conexão recusada ou perdida. (Código " + code + ")");
            }
        }
    }

    @Override
    public void onError(Exception ex) {
        System.out.println("[WS] Erro: " + ex.getMessage());
        // Também pode acionar aqui se quiser
    }

    public void disconnectStomp() {
        try {
            send("DISCONNECT\n\n\0");
        } catch (Exception ignored) {
        } finally {
            close();
        }
    }
}
