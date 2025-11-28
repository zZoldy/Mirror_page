package com.app.mirrorpage.app.tema;

import com.app.mirrorpage.app.model.Tabela;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class ErroCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int col) {

        Component c = super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, col);

        if (Tabela.celulasComErro.contains(new Point(row, col))) {
            c.setBackground(new Color(255, 200, 200));
        } else {
            // normal
            if (isSelected) {
                c.setBackground(table.getSelectionBackground());
            } else {
                c.setBackground(Color.WHITE);
            }
        }

        return c;
    }
}
