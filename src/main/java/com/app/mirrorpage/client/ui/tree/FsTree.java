package com.app.mirrorpage.client.ui.tree;

import com.app.mirrorpage.app.framework.Log;
import com.app.mirrorpage.app.framework.Session;
import com.app.mirrorpage.client.dto.TreeNodeDto;
import com.app.mirrorpage.client.net.ApiClient;
import com.app.mirrorpage.client.net.ApiClient.ApiHttpException;
import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.util.Enumeration;
import java.util.LinkedHashSet;

import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

public class FsTree extends JPanel {

    private static final Object LOADING = new Object() {
        @Override
        public String toString() {
            return "Carregando...";
        }
    };

    private final Session session;
    private final ApiClient api;
    private final JTree tree;

    private static final String ROOT_LABEL = "Produtos";
    private static final String ROOT_PATH = "/";

    // índice rápido path -> nó
    private final ConcurrentMap<String, DefaultMutableTreeNode> index = new ConcurrentHashMap<>();

    // callback para abrir arquivo
    public interface OpenListener {

        void onOpenFile(String path);
    }
    private final java.util.List<OpenListener> openListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    public void addOpenListener(OpenListener l) {
        openListeners.add(l);
    }

    public void removeOpenListener(OpenListener l) {
        openListeners.remove(l);
    }

    public FsTree(Session session, ApiClient api) {
        this.session = session;
        this.api = api;
        setLayout(new BorderLayout());

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(ROOT_LABEL);

        tree = new JTree(root);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setLargeModel(true);
        tree.setToggleClickCount(1);
        tree.setName("tree");

        instalarMouse();

        add(new JScrollPane(tree), BorderLayout.CENTER);

        carregarRaiz();
        configurarExpansao();
        ((JScrollPane) getComponent(0)).setBorder(null);

    }

    public JTree getTree() {
        return tree;
    }

    public void refresh() {
        carregarRaiz();
    }

    // -------- Lazy load raiz --------
    private void carregarRaiz() {
        new SwingWorker<DefaultMutableTreeNode, Void>() {
            @Override
            protected DefaultMutableTreeNode doInBackground() throws Exception {
                List<TreeNodeDto> nodes = api.getTree("/");
                DefaultMutableTreeNode root = new DefaultMutableTreeNode(ROOT_LABEL);
                index.clear();
                index.put(ROOT_PATH, root);
                for (TreeNodeDto dto : nodes) {
                    // garanta que dto.path tem prefixo /
                    dto.path = normalizePath(dto.path);
                    DefaultMutableTreeNode n = new DefaultMutableTreeNode(dto);
                    if (dto.dir) {
                        n.add(new DefaultMutableTreeNode(LOADING));
                    }
                    root.add(n);
                    index.put(dto.path, n);
                }
                return root;
            }

            @Override
            protected void done() {
                try {
                    tree.setModel(new DefaultTreeModel(get()));
                    if (tree.getRowCount() > 0) {
                        tree.expandRow(0);
                    }
                } catch (Exception ex) {
                    tratarErro("Erro ao carregar estrutura", ex);
                }
            }
        }.execute();
    }

    // helper local:
    private static String normalizePath(String p) {
        if (p == null || p.isBlank()) {
            return "/"; // raiz lógica
        }

        // Remover prefixo indevido "/Produtos" se vier da UI/usuário:
        if (p.equals("/Produtos")) {
            p = "/";
        } else if (p.startsWith("/Produtos/")) {
            p = p.substring("/Produtos".length()); // mantém a barra que já existe após "Produtos"
        }

        // Garantir que começa com "/"
        if (!p.startsWith("/")) {
            p = "/" + p;
        }

        // Colapsar barras repetidas
        p = p.replaceAll("/{2,}", "/");

        return p;
    }

    private void configurarExpansao() {
        tree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
                Object obj = node.getUserObject();
                if (!(obj instanceof TreeNodeDto dto) || !dto.dir) {
                    return;
                }

                boolean precisaCarregar = node.getChildCount() == 1
                        && ((DefaultMutableTreeNode) node.getChildAt(0)).getUserObject() == LOADING;
                if (!precisaCarregar) {
                    return;
                }

                new SwingWorker<List<TreeNodeDto>, Void>() {
                    @Override
                    protected List<TreeNodeDto> doInBackground() throws Exception {
                        return api.getTree(dto.path);
                    }

                    @Override
                    protected void done() {
                        try {
                            List<TreeNodeDto> filhos = get();
                            node.removeAllChildren();
                            for (TreeNodeDto f : filhos) {
                                f.path = normalizePath(f.path);
                                DefaultMutableTreeNode n = new DefaultMutableTreeNode(f);
                                if (f.dir) {
                                    n.add(new DefaultMutableTreeNode(LOADING));
                                }
                                node.add(n);
                                index.put(f.path, n);
                            }
                            ((DefaultTreeModel) tree.getModel()).nodeStructureChanged(node);
                        } catch (Exception ex) {
                            tratarErro("Erro ao expandir", ex);
                        }
                    }
                }.execute();
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent e) {
            }
        });
    }

    private void instalarMouse() {
        tree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    TreePath tp = tree.getPathForLocation(e.getX(), e.getY());
                    if (tp == null) {
                        return;
                    }
                    DefaultMutableTreeNode n = (DefaultMutableTreeNode) tp.getLastPathComponent();
                    Object uo = n.getUserObject();
                    if (uo instanceof TreeNodeDto dto && !dto.dir) {
                        if (isCsv(dto.name)) {
                            openListeners.forEach(l -> l.onOpenFile(dto.path));
                        } else {
                            Toolkit.getDefaultToolkit().beep();
                        }
                    }
                }
            }
        });
    }

    private boolean isCsv(String name) {
        return name != null && name.toLowerCase().endsWith(".csv");
    }

    private void tratarErro(String titulo, Exception ex) {
        Log.registrarErro(titulo + ": ", ex);
        if (ex instanceof ApiHttpException apiEx) {
            if (apiEx.isUnauthorized()) {
                JOptionPane.showMessageDialog(this, "Sessão expirada. Faça login novamente.", "Autenticação", JOptionPane.WARNING_MESSAGE);
                return;
            } else if (apiEx.isForbidden()) {
                JOptionPane.showMessageDialog(this, "Sem permissão.", "Permissão", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        JOptionPane.showMessageDialog(this, titulo + ": " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Recarrega apenas o diretório informado (sem recarregar a árvore inteira).
     * Ex.: reloadDir("/Produtos/DF2")
     */
    public void reloadDir(String dirPath) {
        final String path = normalizePath(dirPath);

        SwingUtilities.invokeLater(() -> {
            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            final DefaultMutableTreeNode target = index.get(path);
            if (target == null) {
                if (ROOT_PATH.equals(path)) {
                    refresh();
                }
                return;
            }

            Object uo = target.getUserObject();
            if (!(uo instanceof TreeNodeDto dto) || !dto.dir) {
                return;
            }

            new SwingWorker<List<TreeNodeDto>, Void>() {
                @Override
                protected List<TreeNodeDto> doInBackground() throws Exception {
                    return api.getTree(dto.path);
                }

                @Override
                protected void done() {
                    try {
                        List<TreeNodeDto> filhos = get();

                        // limpa índice dos filhos antigos
                        removeIndexRecursively(target);
                        // recoloca o próprio diretório no índice
                        index.put(dto.path, target);

                        // limpa filhos visuais
                        target.removeAllChildren();

                        // repopula
                        for (TreeNodeDto f : filhos) {
                            f.path = normalizePath(f.path);
                            DefaultMutableTreeNode n = new DefaultMutableTreeNode(f);
                            if (f.dir) {
                                n.add(new DefaultMutableTreeNode(LOADING));
                            }
                            target.add(n);
                            index.put(f.path, n);
                        }

                        model.nodeStructureChanged(target);

                    } catch (Exception ex) {
                        Log.registrarErro("Erro ao recarregar pasta: " + path, ex);
                    }
                }
            }.execute();
        });
    }

    // --------- Atualizações em tempo real (chamadas pelo monitor) ---------
    public static class ChangeEvent {

        public String type;    // CREATE | UPDATE | DELETE | RENAME
        public String path;
        public String newPath; // RENAME
        public boolean dir;
    }

    public void applyEvent(ChangeEvent evt) {
        applyEvents(java.util.Collections.singletonList(evt));
    }

    public void applyEvents(List<ChangeEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        // Garante que está no EDT
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> applyEvents(events));
            return;
        }

        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        Set<String> dirsParaReload = new LinkedHashSet<>();

        for (ChangeEvent ev : events) {
            try {
                String type = ev.type;
                if ("MODIFY".equalsIgnoreCase(type)) {
                    type = "UPDATE"; // unifica
                }
                // normaliza
                String path = normalizePath(ev.path);
                String newPath = (ev.newPath != null && !ev.newPath.isBlank()) ? normalizePath(ev.newPath) : null;
                String dir = (ev.dir) ? parentOf(path) : parentOf(path); // se vier dir específico no payload, pode usar aqui

                switch (type) {
                    case "CREATE" -> {
                        // antes de criar: verifica se já existe
                        boolean existed = (index.get(path) != null);
                        onCreate(model, path, ev.dir);
                        // verifica se entrou no índice; se não, recarrega a pasta
                        boolean nowExists = (index.get(path) != null);
                        if (!nowExists && !existed) {
                            dirsParaReload.add(parentOf(path));
                        }
                    }

                    case "UPDATE" -> {
                        // Se o nó existir, aplica visualmente, senão recarrega a pasta
                        boolean exists = (index.get(path) != null);
                        onUpdate(model, path);
                        if (!exists) {
                            dirsParaReload.add(parentOf(path));
                        }
                    }

                    case "DELETE" -> {
                        boolean existed = (index.get(path) != null);
                        onDelete(model, path);
                        // Se ainda está no índice, força reload
                        if (index.get(path) != null || !existed) {
                            dirsParaReload.add(parentOf(path));
                        }
                    }

                    case "RENAME" -> {
                        if (newPath == null) {
                            dirsParaReload.add(parentOf(path));
                            break;
                        }
                        boolean existedOld = (index.get(path) != null);
                        onRename(model, path, newPath, ev.dir);
                        boolean ok = (index.get(newPath) != null) && (index.get(path) == null);

                        if (!ok) {
                            dirsParaReload.add(parentOf(path));
                            String dstDir = parentOf(newPath);
                            if (!Objects.equals(dstDir, parentOf(path))) {
                                dirsParaReload.add(dstDir);
                            }
                        }
                    }

                    default -> {
                        // tipo desconhecido → garante ao menos um reload do diretório pai
                        dirsParaReload.add(parentOf(path));
                    }
                }

            } catch (Exception ex) {
                Log.registrarErro("Falha ao aplicar evento " + ev.type + " " + ev.path, ex);
                dirsParaReload.add(parentOf(ev.path));
            }
        }

        // Fallback: recarrega as pastas impactadas
        for (String d : dirsParaReload) {
            try {
                reloadDirSafe(d);
            } catch (Exception ex) {
                Log.registrarErro("Falha ao recarregar dir " + d, ex);
            }
        }
    }

    /**
     * Recarrega diretório de forma segura (já está no EDT, mas garante
     * fallback).
     */
    private void reloadDirSafe(String dir) {
        String d = (dir == null || dir.isBlank()) ? "/" : normalizePath(dir);
        reloadDir(d);
    }

    private void onCreate(DefaultTreeModel model, String path, boolean dir) {
        path = normalizePath(path);

        // 1) Se já existe no índice, não cria de novo
        DefaultMutableTreeNode existente = index.get(path);
        if (existente != null) {
            // opcional: só garante que o nó seja redesenhado
            model.nodeChanged(existente);
            return;
        }

        String parent = parentOf(path);
        DefaultMutableTreeNode parentNode = index.get(parent);
        if (parentNode == null) {
            // pai ainda não foi expandido, deixa quieto
            return;
        }

        TreeNodeDto dto = new TreeNodeDto();
        dto.path = path;
        dto.name = nameOf(path);
        dto.dir = dir;

        DefaultMutableTreeNode node = new DefaultMutableTreeNode(dto);
        if (dir) {
            node.add(new DefaultMutableTreeNode(LOADING));
        }

        int pos = insertionIndexByName(parentNode, dto.name);
        model.insertNodeInto(node, parentNode, pos);
        index.put(path, node);
        reloadDirSafe(path);
    }

    private void onUpdate(DefaultTreeModel model, String path) {
        DefaultMutableTreeNode n = index.get(path);
        if (n != null) {
            model.nodeChanged(n);
        }
    }

    private void onDelete(DefaultTreeModel model, String path) {
        DefaultMutableTreeNode n = index.remove(path);
        if (n == null) {
            return;
        }
        removeIndexRecursively(n);
        MutableTreeNode p = (MutableTreeNode) n.getParent();
        if (p != null) {
            model.removeNodeFromParent(n);
        }
    }

    private void onRename(DefaultTreeModel model, String oldPath, String newPath, boolean dir) {
        DefaultMutableTreeNode node = index.remove(oldPath);
        if (node == null) {
            return;
        }
        Object uo = node.getUserObject();
        if (uo instanceof TreeNodeDto dto) {
            dto.path = newPath;
            dto.name = nameOf(newPath);
            dto.dir = dir;
            String oldParent = parentOf(oldPath), newParent = parentOf(newPath);
            if (!Objects.equals(oldParent, newParent)) {
                DefaultMutableTreeNode newParentNode = index.get(newParent);
                if (newParentNode != null) {
                    MutableTreeNode oldParentNode = (MutableTreeNode) node.getParent();
                    if (oldParentNode != null) {
                        model.removeNodeFromParent(node);
                    }
                    int pos = insertionIndexByName(newParentNode, dto.name);
                    model.insertNodeInto(node, newParentNode, pos);
                }
            } else {
                model.nodeChanged(node);
            }
            index.put(newPath, node);
        }
    }

    private void removeIndexRecursively(DefaultMutableTreeNode node) {
        Enumeration<?> e = node.depthFirstEnumeration();
        while (e.hasMoreElements()) {
            Object o = e.nextElement();
            if (o instanceof DefaultMutableTreeNode n) {
                Object uo = n.getUserObject();
                if (uo instanceof TreeNodeDto dto) {
                    index.remove(dto.path);
                }
            }
        }
    }

    private static String parentOf(String path) {
        if (path == null || "/".equals(path)) {
            return "/";
        }
        int i = path.lastIndexOf('/');
        return i <= 0 ? "/" : path.substring(0, i);
    }

    private static String nameOf(String path) {
        if (path == null || path.isBlank()) {
            return "(?)";
        }
        int i = path.lastIndexOf('/');
        return (i >= 0 && i < path.length() - 1) ? path.substring(i + 1) : path;
    }

    private int insertionIndexByName(DefaultMutableTreeNode parent, String name) {
        int count = parent.getChildCount();
        for (int i = 0; i < count; i++) {
            DefaultMutableTreeNode c = (DefaultMutableTreeNode) parent.getChildAt(i);
            String cn = labelOf(c);
            if (cn.compareToIgnoreCase(name) > 0) {
                return i;
            }
        }
        return count;
    }

    private String labelOf(DefaultMutableTreeNode n) {
        Object uo = n.getUserObject();
        if (uo instanceof TreeNodeDto dto) {
            return dto.name != null ? dto.name : dto.path;
        }
        return String.valueOf(uo);
    }
}
