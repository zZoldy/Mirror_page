package com.app.mirrorpage.client.tree;

import com.app.mirrorpage.client.net.ApiClient;
import com.app.mirrorpage.client.ui.tree.FsTree;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;

public class TreeChangeMonitor {

    private final ApiClient api;
    private final FsTree tree;
    private final ScheduledExecutorService exec
            = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "mp-tree-monitor");
                t.setDaemon(true);
                return t;
            });
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile long cursor = 0L;
    private final long intervalSeconds;

    public TreeChangeMonitor(ApiClient api, FsTree tree, long intervalSeconds) {
        this.api = api;
        this.tree = tree;
        this.intervalSeconds = Math.max(1, intervalSeconds);
    }

    public void start() {
        if (running.compareAndSet(false, true)) {

            // 1) Handshake: pega só o cursor atual e ignora o histórico
            try {
                var batch = api.getTreeChanges(0L);
                this.cursor = batch.cursor;
                System.out.println("[TreeMonitor] Inicializado. cursor=" + cursor
                        + " (ignorados " + (batch.events != null ? batch.events.size() : 0) + " eventos antigos)");
            } catch (Exception e) {
                System.err.println("[TreeMonitor] Falha ao inicializar cursor: " + e.getMessage());
            }

            // 2) Só a partir daqui começa a aplicar eventos novos
            exec.scheduleWithFixedDelay(this::tick, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        }
    }

    public void stop() {
        running.set(false);
        exec.shutdownNow();
    }

    private void tick() {
        if (!running.get()) {
            return;
        }
        try {
            var batch = api.getTreeChanges(cursor);
            if (batch == null || batch.events.isEmpty()) {
                return;
            }

            // Log simples para debug
            System.out.println("[TreeMonitor] " + batch.events.size() + " eventos recebidos.");

            List<FsTree.ChangeEvent> mapped = batch.events.stream()
                    .map(d -> {
                        FsTree.ChangeEvent ce = new FsTree.ChangeEvent();
                        ce.type = d.type;
                        ce.path = d.path;
                        ce.newPath = d.newPath;
                        ce.dir = d.dir;  // campo booleano do servidor
                        return ce;
                    })
                    .toList();

            SwingUtilities.invokeLater(() -> tree.applyEvents(mapped));

            cursor = Math.max(cursor, batch.cursor);

        } catch (Exception e) {
            System.err.println("[TreeMonitor] Falha ao buscar alterações: " + e.getMessage());
        }
    }
}
