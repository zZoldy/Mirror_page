package com.app.mirrorpage.client.net;

import com.app.mirrorpage.client.dto.CellChangeEvent;
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
    private final String topic;
    private final Consumer<CellChangeEvent> onCellChange;
    private final Consumer<RowInsertedEvent> onRowInserted;
    private final Consumer<RowMoveEvent> onRowMoved;
    private final String subscriptionId;

    public SheetSocketClient(
            URI serverUri,
            String topic,
            Consumer<CellChangeEvent> onCellChange,
            Consumer<RowInsertedEvent> onRowInserted,
            Consumer<RowMoveEvent> onRowMoved
    ) {
        super(serverUri);
        this.topic = topic;
        this.onCellChange = onCellChange;
        this.onRowInserted = onRowInserted;
        this.onRowMoved = onRowMoved;
        this.subscriptionId = "sub-" + UUID.randomUUID();
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("[WS] Conectado ao servidor: " + getURI());

        String connectFrame = "CONNECT\n"
                + "accept-version:1.2\n"
                + "host:localhost\n"
                + "\n"
                + "\0";
        send(connectFrame);

        String subscribeFrame = "SUBSCRIBE\n"
                + "id:" + subscriptionId + "\n"
                + "destination:" + topic + "\n"
                + "\n"
                + "\0";
        send(subscribeFrame);

        System.out.println("[WS] SUBSCRIBE em " + topic);
    }

    @Override
    public void onMessage(String message) {
        try {
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

            // 👇 ADICIONA ISSO
            System.out.println("[WS] Mensagem recebida: " + body);

            JsonNode json = mapper.readTree(body);
            System.out.println("JSON: " + json.toString());
            // -------------------------------
            // IDENTIFICA O TIPO DO EVENTO
            // -------------------------------
            if (json.has("col")) {
                // É CellChangeEvent
                CellChangeEvent ev = mapper.readValue(body, CellChangeEvent.class);
                if (onCellChange != null) {
                    onCellChange.accept(ev);
                }
                return;
            }

            if (json.has("afterRow")) {
                // É RowInsertedEvent
                RowInsertedEvent ev = mapper.readValue(body, RowInsertedEvent.class);
                if (onRowInserted != null) {
                    onRowInserted.accept(ev);
                }
                return;
            }

            // 3) Evento de LINHA MOVIDA (tem "fromRow" e "toRow")
            if (json.has("from")) {
                RowMoveEvent ev = mapper.readValue(body, RowMoveEvent.class);
                if (onRowMoved != null) {
                    onRowMoved.accept(ev);
                }
                return;
            }

            System.out.println("[WS] Evento desconhecido: " + body);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[WS] Erro ao processar mensagem STOMP: " + message);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.printf("[WS] Fechado code=%d reason=%s remote=%s%n", code, reason, remote);
    }

    @Override
    public void onError(Exception ex) {
        System.out.println("[WS] Erro: " + ex.getMessage());
        ex.printStackTrace();
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
