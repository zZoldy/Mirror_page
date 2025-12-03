package com.app.mirrorpage.app.tema;

import com.app.mirrorpage.app.model.Tabela;
import static com.app.mirrorpage.app.tema.TemaNome.DARK;
import static com.app.mirrorpage.app.tema.TemaNome.DEFAULT;
import static com.app.mirrorpage.app.tema.TemaNome.STAR_LIGHT;
import com.app.mirrorpage.client.tree.CustomTreeRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.border.MatteBorder;

public class ThemeApplier {

    public static String tema;

    public static Font font_jTree = new Font("/fonts/Titulo-Bold.tff", Font.BOLD, 12);
    public static Font font_desktop = new Font("/fonts/Titulo-Bold.tff", Font.BOLD, 14);
    public static Font font_table = new Font("/fonts/Texto-Bold.tff", Font.BOLD, 16);
    public static Font font_horario = new Font("/fonts/Titulo-Bold.tff", Font.BOLD, 20);

    public static Color COLOR_WHITE = new Color(255, 255, 255);

    public static Color COLOR_DEFAULT = new Color(180, 190, 200);
    public static Color COLOR_DEFAULT_BACKGROUND = new Color(210, 220, 230);

    public static Color COLOR_CRONOMETRO_START = new Color(0, 150, 0);
    public static Color COLOR_CRONOMETRO_PAUSE = new Color(230, 140, 0);
    public static Color COLOR_CRONOMETRO_STOP = new Color(180, 0, 0);

    public static Color COLOR_DEFAULT_HEADER_TABLE = new Color(255, 255, 255);
    public static Color COLOR_DEFAULT_LINE = new Color(210, 220, 230);
    public static Color COLOR_DEFAULT_LINE_SELECTED = new Color(210, 220, 230);
    public static Color COLOR_DEFAULT_LINE_BACK_SELECTED = new Color(210, 220, 230);
    public static Color COLOR_DEFAULT_LINE_TEXT_SELECTED = new Color(210, 220, 230);

    public static Color COLOR_DARK_LINE = new Color(210, 220, 230);
    public static Color COLOR_DARK_LINE_SELECTED = new Color(210, 220, 230);
    public static Color COLOR_DARK_LINE_BACK_SELECTED = new Color(210, 220, 230);
    public static Color COLOR_DARK_LINE_TEXT_SELECTED = new Color(210, 220, 230);

    public static Color COLOR_ERROR = new Color(255, 200, 200);

    public static Color COLOR_DARK = new Color(30, 30, 30);

    public static void apply(Container root, TemaNome tema) {
        switch (tema) {
            case DEFAULT ->
                Default(root);
            case DARK ->
                Dark(root);
            case STAR_LIGHT ->
                Dark(root);
            default ->
                Default(root);
        }

        root.revalidate();
        root.repaint();
    }

    public static void apply_table(JTable table, TemaNome tema) {
        switch (tema) {
            case DEFAULT ->
                jTable_default(table);
            case DARK ->
                jTabel_dark(table);
            case STAR_LIGHT ->
                jTable_star_light(table);
            default ->
                Default(table);
        }

        System.out.println("TEMA APPLY: " + tema);
        table.revalidate();
        table.repaint();
    }

    public static void Default(Container root) {
        aplicarDefaultRecursivo(root, COLOR_DEFAULT, COLOR_DARK, COLOR_DEFAULT_BACKGROUND, COLOR_WHITE, COLOR_WHITE, font_jTree);
        root.revalidate();
        root.repaint();
        tema = "default";
    }

    public static void applyCronometro(JLabel start, JLabel stop, JLabel cronometro, TemaNome nome, boolean run) {
        switch (nome) {
            case DEFAULT ->
                cron_default(start, stop, cronometro, run);

            case DARK ->
                cron_dark(start, stop, cronometro, run);

            case STAR_LIGHT ->
                cron_dark(start, stop, cronometro, run);

            default ->
                cron_default(start, stop, cronometro, run);
        }
    }

    static void cron_default(JLabel start, JLabel stop, JLabel cronometro, boolean run) {
        start.setBackground(COLOR_DEFAULT);
        if (start.getText().equals("START")) {
            start.setForeground(COLOR_CRONOMETRO_START);
            if (run) {
                cronometro.setForeground(COLOR_CRONOMETRO_START);
            } else {
                cronometro.setForeground(COLOR_CRONOMETRO_PAUSE);
            }
        } else if (start.getText().equals("PAUSE")) {
            start.setForeground(COLOR_CRONOMETRO_PAUSE);
            if (run) {
                cronometro.setForeground(COLOR_CRONOMETRO_START);
            } else {
                cronometro.setForeground(COLOR_CRONOMETRO_PAUSE);
            }

        }

        stop.setBackground(COLOR_DEFAULT);
        stop.setForeground(COLOR_CRONOMETRO_STOP);
    }

    static void cron_dark(JLabel start, JLabel stop, JLabel cronometro, boolean run) {
        start.setBackground(COLOR_DARK);

        if (start.getText().equals("START")) {
            start.setForeground(COLOR_CRONOMETRO_START);
            if (run) {
                cronometro.setForeground(COLOR_CRONOMETRO_START);
            } else {
                cronometro.setForeground(COLOR_CRONOMETRO_PAUSE);
            }

        } else if (start.getText().equals("PAUSE")) {
            start.setForeground(COLOR_CRONOMETRO_PAUSE);
            if (run) {
                cronometro.setForeground(COLOR_CRONOMETRO_START);
            } else {
                cronometro.setForeground(COLOR_CRONOMETRO_PAUSE);
            }
        }

        stop.setBackground(COLOR_DARK);
        stop.setForeground(COLOR_CRONOMETRO_STOP);
    }

    private static void aplicarDefaultRecursivo(Component c, Color back, Color fore, Color background, Color file, Color close_file, Font font_ree) {
        // debug opcional
        //System.out.println("Comp: " + c.getClass().getSimpleName() + " | name=" + c.getName());

        // regra geral
        if (c instanceof JComponent jc) {
            jc.setOpaque(true);
        }

        // regras por nome
        aplicarRegrasPorNome(c, back, fore, background, font_ree);
        // desce nos filhos
        if (c instanceof Container cont) {
            for (Component filho : cont.getComponents()) {
                aplicarDefaultRecursivo(filho, back, fore, background, file, close_file, font_ree);
            }
        }
    }

    // back = default / fore = dark
    private static void aplicarRegrasPorNome(Component c, Color back, Color fore, Color background, Font font_tree) {
        String name = c.getName();
        if (name == null) {
            return;
        }

        switch (name) {
            case "pn_background":
                c.setBackground(back);
                c.setForeground(fore);

                break;
            case "pn_lateral":
                c.setBackground(back);
                c.setForeground(fore);

                break;
            case "pn_tree":
                c.setBackground(back);
                c.setForeground(fore);

                break;
            case "fsTree":
                c.setBackground(back);
                c.setForeground(fore);

                break;
            case "pn_marca":
                c.setBackground(back);
                c.setForeground(fore);

                break;
            case "lbl_marca":
                c.setBackground(back);
                c.setForeground(fore);

                break;
            case "control_pn_lateral":
                c.setBackground(back);
                c.setForeground(fore);
                c.setFont(font_desktop);
                if (c instanceof JLabel label) {
                    label.setBorder(new MatteBorder(0, 0, 0, 2, fore)); // só na direita
                }

                break;

            case "pn_desktop":
                c.setBackground(back);

                break;
            case "tree":
                if (c instanceof JTree tree) {
                    tree.setFont(font_tree);
                    tree.setBackground(back);
                    tree.setForeground(fore);
                    tree.setCellRenderer(new CustomTreeRenderer(back, fore, new Color(0, 0, 0, 0), Color.ORANGE));
                    tree.repaint();
                    tree.revalidate();

                }
                break;
            case "pn_logo":
                c.setBackground(background);
                c.setForeground(fore);

                break;
            case "pn_superior_desktop":
                c.setBackground(back);
                c.setForeground(fore);

                break;
            case "lbl_show_user":
                c.setBackground(back);
                c.setForeground(fore);
                if (c instanceof JLabel label) {
                    label.setBorder(new MatteBorder(2, 0, 2, 2, fore));
                }
                c.setFont(font_desktop);
                break;
            case "lbl_arquivo_aberto":
                c.setBackground(back);
                c.setForeground(fore);
                if (c instanceof JLabel label) {
                    label.setBorder(new MatteBorder(2, 2, 2, 0, fore));
                }
                c.setFont(font_desktop);

                break;
            case "lbl_fechar_arquivo":
                c.setBackground(back);
                c.setForeground(Color.RED);
                if (c instanceof JLabel label) {
                    label.setBorder(new MatteBorder(2, 0, 2, 2, fore));
                }
                c.setFont(font_desktop);

                break;
            case "pn_inferior":
                c.setBackground(back);
                c.setForeground(fore);
                c.setFont(font_desktop);
                break;
            case "pn_alto":
                c.setBackground(back);
                c.setForeground(fore);
                c.setFont(font_desktop);
                break;

            case "lbl_entrada_jornal":
                c.setBackground(back);
                c.setForeground(fore);
                c.setFont(font_desktop);
                break;
            case "out_entrada_jornal":
                c.setBackground(back);
                c.setForeground(fore);
                c.setFont(font_desktop);

                break;
            case "lbl_producao":
                c.setBackground(back);
                c.setForeground(fore);
                c.setFont(font_desktop);

                break;
            case "out_tempo_producao":
                c.setBackground(back);
                c.setForeground(fore);
                c.setFont(font_desktop);

                break;
            case "lbl_saida_jornal":
                c.setBackground(back);
                c.setForeground(fore);
                c.setFont(font_desktop);

                break;
            case "out_saida_jornal":
                c.setBackground(back);
                c.setForeground(fore);
                c.setFont(font_desktop);

                break;

            case "pn_baixo":
                c.setBackground(back);
                c.setForeground(fore);
                c.setFont(font_desktop);

                break;
            case "lbl_encerramento":
                c.setBackground(back);
                c.setForeground(fore);
                c.setFont(font_desktop);

                break;
            case "out_encerramento":
                c.setBackground(back);
                c.setForeground(fore);
                c.setFont(font_desktop);
                break;
            case "out_status_jornal":
                c.setBackground(back);
                c.setForeground(fore);
                c.setFont(font_desktop);

                break;
            case "horario_atual":
                c.setBackground(back);
                c.setForeground(fore);
                c.setFont(font_horario);
                break;

            case "lbl_cronometro":
                c.setBackground(back);
                c.setForeground(fore);
                c.setFont(font_horario);
                break;
            case "lbl_cronometro_start":
                c.setBackground(back);
                if (c instanceof JLabel label) {
                    if (label.getText().equals("START")) {
                        c.setForeground(COLOR_CRONOMETRO_START);
                    } else if (label.getText().equals("PAUSE")) {
                        c.setForeground(COLOR_CRONOMETRO_PAUSE);
                    }
                }
                c.setFont(font_desktop);
                break;
            case "lbl_cronometro_stop":
                c.setBackground(back);
                c.setForeground(COLOR_CRONOMETRO_STOP);
                c.setFont(font_desktop);
                break;

            case "splitPane_lauda":
                c.setBackground(back);
                c.setForeground(fore);
                c.setFont(font_horario);
                break;

            case "pn_superior_lauda":
                c.setBackground(back);
                c.setForeground(fore);
                c.setFont(font_horario);
                break;

            case "pn_lauda":
                c.setBackground(back);
                c.setForeground(fore);
                c.setFont(font_horario);
                break;

            case "txt_lauda":
                c.setBackground(back);
                c.setForeground(fore);
                c.setFont(font_horario);
                break;

            case "lauda_titulo":
                c.setBackground(back);
                c.setForeground(fore);
                c.setFont(font_horario);
                break;

            case "lauda_info":
                c.setBackground(back);
                c.setForeground(fore);
                c.setFont(font_horario);
                break;

            case "lauda_usuario":
                c.setBackground(back);
                c.setForeground(fore);
                c.setFont(font_horario);
                break;

            // etc… aqui você trata pn_tree, pn_superior_desktop, pn_inferior, pn_baixo, horario_atual, etc.
        }
    }

    public static void Dark(Container root) {
        aplicarDefaultRecursivo(root, COLOR_DARK, COLOR_WHITE, COLOR_DARK, COLOR_WHITE, COLOR_WHITE, font_jTree);
        root.revalidate();
        root.repaint();
        tema = "dark";
    }

    public static void jTable_default(JTable table) {
        Container parent = table.getParent();
        if (parent instanceof JViewport viewport) {
            // Agora você tem acesso ao viewport
            viewport.setBackground(COLOR_DEFAULT); // Exemplo de uso
        }

        Tabela.renderer_header_table(table, font_desktop, COLOR_WHITE, COLOR_DARK);
        Tabela.renderer_line_table(table, font_table, new Color(200, 200, 200), new Color(0, 0, 0), COLOR_WHITE, new Color(0, 0, 0), new Color(184, 207, 229), new Color(0, 0, 0), new Color(184, 207, 229), new Color(0, 0, 0));
    }

    public static void jTabel_dark(JTable table) {
        Container parent = table.getParent();
        if (parent instanceof JViewport viewport) {
            // Agora você tem acesso ao viewport
            viewport.setBackground(COLOR_DARK); // Exemplo de uso
        }

        Tabela.renderer_header_table(table, font_desktop, COLOR_WHITE, COLOR_DARK);
        Tabela.renderer_line_table(table, font_table, new Color(80, 80, 80), COLOR_WHITE, new Color(30, 30, 30), COLOR_WHITE, new Color(184, 207, 229), new Color(0, 0, 0), new Color(184, 207, 229), new Color(0, 0, 0));

    }

    public static void jTable_star_light(JTable table) {
        Container parent = table.getParent();
        if (parent instanceof JViewport viewport) {
            // Agora você tem acesso ao viewport
            viewport.setBackground(COLOR_DARK); // Exemplo de uso
        }

        Tabela.renderer_header_table(table, font_desktop, Color.ORANGE, COLOR_DARK);
        Tabela.renderer_line_table(table, font_table, new Color(0, 0, 0), Color.ORANGE, new Color(0, 0, 0), Color.ORANGE, new Color(0, 0, 0, 0), Color.GREEN, new Color(0, 0, 0, 0), Color.GREEN);
        tema = "star_light";
    }

}
