/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.app.mirrorpage.client.net;

import com.app.mirrorpage.client.ui.tree.FsTree;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.UUID;
import java.util.function.Consumer;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class AppSocketClient extends WebSocketClient {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Consumer<FsTree.ChangeEvent> onTreeChange;
    private final String subscriptionId;

    public AppSocketClient(URI serverUri, Consumer<FsTree.ChangeEvent> onTreeChange) {
        super(serverUri);
        this.onTreeChange = onTreeChange;
        this.subscriptionId = "sub-tree-" + UUID.randomUUID();
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("[AppWS] Conectado: " + getURI());

        // Protocolo STOMP: CONNECT
        String connectFrame = "CONNECT\n" + "accept-version:1.2\n" + "host:localhost\n\n\0";
        send(connectFrame);

        // Protocolo STOMP: SUBSCRIBE no tópico da árvore
        String subscribeFrame = "SUBSCRIBE\n"
                + "id:" + subscriptionId + "\n"
                + "destination:/topic/tree-changes\n\n\0";
        send(subscribeFrame);
        System.out.println("[AppWS] Inscrito em /topic/tree-changes");
    }

    @Override
    public void onMessage(String message) {
        try {
            // Parser manual simples do frame STOMP para extrair o JSON
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

            // Converter JSON para Evento da Árvore
            JsonNode json = mapper.readTree(body);

            if (json.has("type") && json.has("path")) {
                FsTree.ChangeEvent evt = new FsTree.ChangeEvent();
                evt.type = json.get("type").asText();
                evt.path = json.get("path").asText();

                if (json.has("newPath") && !json.get("newPath").isNull()) {
                    evt.newPath = json.get("newPath").asText();
                }

                // Trata o boolean 'isDir' (ou 'dir' dependendo do DTO do servidor)
                if (json.has("isDir")) {
                    evt.dir = json.get("isDir").asBoolean();
                } else if (json.has("dir")) {
                    evt.dir = Boolean.parseBoolean(json.get("dir").asText());
                }

                if (onTreeChange != null) {
                    onTreeChange.accept(evt);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("[AppWS] Desconectado: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("[AppWS] Erro: " + ex.getMessage());
    }
}
