package com.app.mirrorpage.client.net;

import com.app.mirrorpage.client.dto.CellChangeEvent;
import com.app.mirrorpage.client.dto.FileLockEvent; // 1. Não esqueça de importar
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
    private final String username; 
    private final String subscriptionId;
    private final String lockSubscriptionId; // ID separado para locks

    // Callbacks de eventos
    private final Consumer<CellChangeEvent> onCellChange;
    private final Consumer<RowInsertedEvent> onRowInserted;
    private final Consumer<RowMoveEvent> onRowMoved;
    private final Consumer<RowDeletedEvent> onRowDeleted;
    private final Consumer<FileLockEvent> onLockEvent; // Callback de Lock
    private final Consumer<String> onConnectionError;

    /**
     * Construtor Completo
     */
    public SheetSocketClient(
            URI serverUri,
            String topic,
            String username,
            Consumer<CellChangeEvent> onCellChange,
            Consumer<RowInsertedEvent> onRowInserted,
            Consumer<RowMoveEvent> onRowMoved,
            Consumer<RowDeletedEvent> onRowDeleted,
            Consumer<FileLockEvent> onLockEvent, // Recebe Lock
            Consumer<String> onConnectionError
    ) {
        super(serverUri);
        this.topic = topic;
        this.username = username;
        this.onCellChange = onCellChange;
        this.onRowInserted = onRowInserted;
        this.onRowMoved = onRowMoved;
        this.onRowDeleted = onRowDeleted;
        this.onLockEvent = onLockEvent;
        this.onConnectionError = onConnectionError;
        
        this.subscriptionId = "sub-" + UUID.randomUUID();
        this.lockSubscriptionId = "sub-lock-" + UUID.randomUUID();
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("[WS] Conectado ao servidor: " + getURI());

        // 1. CONNECT
        String loginHeader = (username != null) ? "login-user:" + username + "\n" : "";
        String connectFrame = "CONNECT\n"
                + "accept-version:1.2\n"
                + "host:localhost\n"
                + loginHeader
                + "\n"
                + "\0";
        send(connectFrame);

        // 2. SUBSCRIBE PLANILHA
        String subData = "SUBSCRIBE\n"
                + "id:" + subscriptionId + "\n"
                + "destination:" + topic + "\n"
                + "\n"
                + "\0";
        send(subData);
        System.out.println("[WS] Ouvindo dados em: " + topic);

        // 3. SUBSCRIBE LOCKS (Crucial para "Lauda Livre")
        String subLocks = "SUBSCRIBE\n"
                + "id:" + lockSubscriptionId + "\n"
                + "destination:/topic/locks\n"
                + "\n"
                + "\0";
        send(subLocks);
        System.out.println("[WS] Ouvindo locks em: /topic/locks");
    }

    @Override
    public void onMessage(String message) {
        try {
            int idx = message.indexOf("\n\n");
            if (idx < 0) return;

            String body = message.substring(idx + 2);
            if (body.endsWith("\0")) {
                body = body.substring(0, body.length() - 1);
            }
            body = body.trim();
            if (body.isEmpty()) return;

            // Tenta processar como JSON
            JsonNode json = mapper.readTree(body);

            // A) Evento de Lock (Prioridade)
            if (json.has("locked") && json.has("path")) {
                System.out.println("[WS] Evento de Lock recebido: " + body); // Debug
                FileLockEvent ev = mapper.readValue(body, FileLockEvent.class);
                if (onLockEvent != null) onLockEvent.accept(ev);
                return;
            }

            // B) Eventos da Planilha
            if (json.has("col") && json.has("row") && json.has("value")) {
                CellChangeEvent ev = mapper.readValue(body, CellChangeEvent.class);
                if (onCellChange != null) onCellChange.accept(ev);
                return;
            }
            if (json.has("modelRow")) {
                RowDeletedEvent ev = mapper.readValue(body, RowDeletedEvent.class);
                if (onRowDeleted != null) onRowDeleted.accept(ev);
                return;
            }
            if (json.has("afterRow")) {
                RowInsertedEvent ev = mapper.readValue(body, RowInsertedEvent.class);
                if (onRowInserted != null) onRowInserted.accept(ev);
                return;
            }
            if (json.has("from")) {
                RowMoveEvent ev = mapper.readValue(body, RowMoveEvent.class);
                if (onRowMoved != null) onRowMoved.accept(ev);
                return;
            }

        } catch (Exception e) {
            // Ignora erros de parse em frames de controle (CONNECTED)
            if (!message.startsWith("CONNECTED") && !message.startsWith("MESSAGE")) {
                // System.out.println("[WS] Ignorando frame de controle ou erro de parse.");
            } else {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.printf("[WS] Fechado code=%d reason=%s remote=%s%n", code, reason, remote);
        if (code != 1000 && onConnectionError != null) {
            onConnectionError.accept("Conexão perdida (Código " + code + ")");
        }
    }

    @Override
    public void onError(Exception ex) {
        System.out.println("[WS] Erro: " + ex.getMessage());
    }

    public void disconnectStomp() {
        try { send("DISCONNECT\n\n\0"); } catch (Exception ignored) {} finally { close(); }
    }
}