package com.app.mirrorpage.client.tree;

import com.app.mirrorpage.client.dto.TreeNodeDto;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

public class CustomTreeRenderer extends DefaultTreeCellRenderer {

    private static final Object LOADING = new Object() {
        @Override
        public String toString() {
            return "Carregando...";
        }
    };

    private Color corFundo;
    private Color corTexto;
    private Color corFundoSelecionado;
    private Color corTextoSelecionado;

    public CustomTreeRenderer(Color corFundo, Color corTexto, Color corFundoSelecionado, Color corTextoSelecionado) {
        this.corFundo = corFundo;
        this.corTexto = corTexto;
        this.corFundoSelecionado = corFundoSelecionado;
        this.corTextoSelecionado = corTextoSelecionado;
    }

    private final Icon dir = new ImageIcon(getClass().getResource("/icons/pasta.png"));
    private final Icon file = new ImageIcon(getClass().getResource("/icons/arquivo.png"));

    @Override
    public Component getTreeCellRendererComponent(JTree t, Object value, boolean sel, boolean exp, boolean leaf, int row, boolean focus) {
        JLabel label = (JLabel) super.getTreeCellRendererComponent(t, value, sel, exp, leaf, row, focus);
        Object uo = ((DefaultMutableTreeNode) value).getUserObject();
        if (uo == LOADING) {
            setText("Carregando...");
            setIcon(dir);
        } else if (uo instanceof TreeNodeDto dto) {
            setText(dto.name != null ? dto.name : dto.path);
            setIcon(dto.dir ? dir : file);
            setPreferredSize(new Dimension(150, 20));
            label.setOpaque(true); // necessário para a cor de fundo aparecer
            label.setBackground(corFundo); // Cor de fundo padrão
            label.setForeground(corTexto); // Texto padrão
            if (sel) {
                setBackground(corFundoSelecionado);   // Fundo de seleção
                setForeground(corTextoSelecionado);  // Texto de seleção
            }

        }

        return this;
    }

}
