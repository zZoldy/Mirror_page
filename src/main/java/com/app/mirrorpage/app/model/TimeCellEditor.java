package com.app.mirrorpage.app.model;

import com.app.mirrorpage.app.framework.Funcoes;
import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.ParseException;
import javax.swing.DefaultCellEditor;
import javax.swing.JFormattedTextField;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.MaskFormatter;

public class TimeCellEditor extends DefaultCellEditor {

    private final JFormattedTextField textField;

    public TimeCellEditor(String mask) {
        super(new JFormattedTextField(createFormatter(mask)));
        textField = (JFormattedTextField) getComponent();

        textField.setHorizontalAlignment(JTextField.CENTER);
        textField.setFocusLostBehavior(JFormattedTextField.PERSIST);

        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                SwingUtilities.invokeLater(() -> textField.setCaretPosition(0));
            }

            @Override
            public void focusLost(FocusEvent e) {
                textField.setText(sanitizeTime(textField.getText()));
            }
        });
    }

    private static MaskFormatter createFormatter(String mask) {
        try {
            MaskFormatter f = new MaskFormatter(mask);
            f.setPlaceholderCharacter('0');   // sempre “00:00”
            f.setOverwriteMode(true);
            f.setAllowsInvalid(false);        // evita letras etc.
            return f;
        } catch (ParseException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /*────────────────────  Sanitização  ────────────────────*/
    private String sanitizeTime(String txt) {
        if (txt == null) {
            return "00:00";
        }

        String[] p = txt.split(":");

        if (p.length == 2) {
            int m = p.length > 0 ? Funcoes.parseSafe(p[0]) : 0;
            int s = p.length > 1 ? Funcoes.parseSafe(p[1]) : 0;
            if (m > 59) {
                m = 59;
            }

            if (s > 59) {
                s = 59;
            }

            return String.format("%02d:%02d", m, s);
        }

        if (p.length == 3) {
            int h = p.length > 0 ? Funcoes.parseSafe(p[0]) : 0;
            int m = p.length > 1 ? Funcoes.parseSafe(p[1]) : 0;
            int s = p.length > 2 ? Funcoes.parseSafe(p[2]) : 0;

            if (h > 23) {
                h = 23;
            }

            if (m > 59) {
                m = 59;
            }

            if (s > 59) {
                s = 59;
            }

            return String.format("%02d:%02d:%02d", h, m, s);
        }

        return "00:00";
    }

    /*────────────────────  JTable hooks  ────────────────────*/
    @Override
    public Component getTableCellEditorComponent(JTable t, Object value,
            boolean isSel, int row, int col) {
        String v = (value != null && !value.toString().trim().isEmpty())
                ? value.toString().trim() : "00:00";
        textField.setText(sanitizeTime(v));
        SwingUtilities.invokeLater(() -> textField.setCaretPosition(0));
        return textField;
    }

    @Override
    public Object getCellEditorValue() {
        return sanitizeTime(textField.getText());
    }
}
