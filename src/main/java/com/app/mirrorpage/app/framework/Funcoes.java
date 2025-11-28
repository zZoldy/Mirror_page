
package com.app.mirrorpage.app.framework;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.TableModel;

public class Funcoes {

    public static void install() {

    }

    // Funções TEMPO
    public static void acao_com_trava(Runnable acao, AtomicBoolean temporizador_disp) {
        if (!temporizador_disp.get()) {
            return;
        }

        temporizador_disp.set(false);
        acao.run();
    }

    public static void stop_action_frame(Component comp, Runnable acao_in, Runnable acao_out, AtomicBoolean action_frame) {
        acao_com_trava(() -> {
            comp.setEnabled(false);
            comp.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    Thread.sleep(500); // Delay opcional
                    // Se acao_in mexe na UI, use invokeLater
                    if (acao_in != null) {
                        SwingUtilities.invokeLater(acao_in);
                    }
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        if (acao_out != null) {
                            acao_out.run();
                        }
                    } finally {
                        comp.setEnabled(true);
                        action_frame.set(true);
                        comp.setCursor(Cursor.getDefaultCursor());
                        Log.registrarErro_noEx("Ação Stop Frame Acionada");
                    }
                }
            }.execute();
        }, action_frame);
    }

    public static void in_out_run(Component comp, Runnable acao_in, Runnable acao_out) {
        comp.setEnabled(false);
        comp.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Se acao_in mexe na UI, use invokeLater
                if (acao_in != null) {
                    acao_in.run();
                }
                Thread.sleep(500); // Delay opcional
                return null;
            }

            @Override
            protected void done() {
                try {
                    if (acao_out != null) {
                        acao_out.run();
                    }
                } finally {
                    comp.setEnabled(true);
                    comp.setCursor(Cursor.getDefaultCursor());
                    Log.registrarErro_noEx("Ação Stop Frame Acionada -  in_out_run()");
                }
            }
        }.execute();

    }

    public static void agendar_acao(String tempo, Runnable acao) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        try {
            Date for_tempo = sdf.parse(tempo);
            long milissegundos = for_tempo.getTime() - sdf.parse("00:00:00").getTime();

            java.util.Timer timer = new java.util.Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    acao.run();
                    timer.cancel();
                }
            }, milissegundos);
        } catch (ParseException e) {
            System.err.println("Erro ao converter Tempo \n" + e);
        }
    }

    public static boolean delay_acao(String tempo, Runnable acao) {
        boolean stts_acao = false;

        if (stts_acao == true) {
            return true;
        }

        return stts_acao;

    }

    public static void iniciar_disparo(int segundos, Runnable acao) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(acao, 0, segundos, TimeUnit.SECONDS);
    }

    public static String data_atual() {
        Date date_atual = new Date();
        SimpleDateFormat data_atual_formatada = new SimpleDateFormat("dd/MM/yyyy");
        return data_atual_formatada.format(date_atual);
    }

    public static String hora_atual() {
        return java.time.LocalTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    // Relógio
    public static void relogio(JLabel label) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("HH:mm:ss");
        scheduler.scheduleAtFixedRate(() -> {
            LocalTime agora = LocalTime.now();
            String horario_formatado = agora.format(formato);
            label.setText(horario_formatado);
        }, 0, 1, TimeUnit.SECONDS);
    }

    // Processa um arquivo para definir um modelo de tabela;
    public static int parseSafe(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    public static int converter_to_segundos(String tempo) {
        String[] partes = tempo.split(":");

        int horas = 0;
        int minutos = 0;
        int segundos = 0;

        if (partes.length == 3) {
            horas = Integer.parseInt(partes[0]);
            minutos = Integer.parseInt(partes[1]);
            segundos = Integer.parseInt(partes[2]);
        } else if (partes.length == 2) {
            minutos = Integer.parseInt(partes[0]);
            segundos = Integer.parseInt(partes[1]);
        } else if (partes.length == 1) {
            segundos = Integer.parseInt(partes[0]);
        }

        return horas * 3600 + minutos * 60 + segundos;
    }

    public static String soma_tempo(String tempo_1, String tempo_2) {
        int segundos_1 = converter_to_segundos(tempo_1);
        int segundos_2 = converter_to_segundos(tempo_2);
        int total_segundos = segundos_1 + segundos_2;

        int horas = total_segundos / 3600;
        int mins = (total_segundos % 3600) / 60;
        int segundos = total_segundos % 60;

        return String.format("%02d:%02d:%02d", horas, mins, segundos);
    }

    public static String soma_mins(String tempo_1, String tempo_2) {
        int segundos_1 = converter_to_segundos(tempo_1);
        int segundos_2 = converter_to_segundos(tempo_2);
        int total_segundos = segundos_1 + segundos_2;

        int horas = total_segundos / 3600;
        int mins = (total_segundos % 3600) / 60;
        int segundos = total_segundos % 60;

        return String.format("%02d:%02d", mins, segundos);
    }

    public static String format_ms_to_hms(String mm_ss) {
        if (mm_ss.equals("")) {
            return "00:00:00";
        }

        // Converte mm:ss para segundos
        String[] partes = mm_ss.split(":");
        int minutos = partes.length > 0 ? Integer.parseInt(partes[0]) : 0;
        int segundos = partes.length > 1 ? Integer.parseInt(partes[1]) : 0;
        int totalSegundos = minutos * 60 + segundos;

        // Converte totalSegundos para HH:mm:ss
        int horas = totalSegundos / 3600;
        int minutosRestantes = (totalSegundos % 3600) / 60;
        int segundosRestantes = totalSegundos % 60;

        String tempoFormatado = String.format("%02d:%02d:%02d", horas, minutosRestantes, segundosRestantes);
        return tempoFormatado;
    }

    public static String formatarDuracao(Duration d) {
        long horas = d.toHours();
        long minutos = d.toMinutesPart();
        long segundos = d.toSecondsPart();

        return String.format("%02d:%02d:%02d", horas, minutos, segundos);
    }

    public static void criarCSV(String caminhoArquivo, String[] colunas, List<String[]> linhas) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(caminhoArquivo))) {
            // Escreve as colunas (primeira linha)
            bw.write(String.join(",", colunas));
            bw.newLine();

            if (linhas != null) {
                // Escreve cada linha de dados
                for (String[] line : linhas) {
                    bw.write(String.join(",", line));
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao criar arquivo CSV");
            e.printStackTrace();
        }
    }

    public static void copy_file(File copia, File novo) {
        if (copia == null || novo == null || !copia.exists()) {
            System.out.println("Arquivo de origem não encontrado.");
            return;
        }

        try (BufferedReader leitor = new BufferedReader(new FileReader(copia)); BufferedWriter escritor = new BufferedWriter(new FileWriter(novo))) {

            String linha;
            while ((linha = leitor.readLine()) != null) {
                escritor.write(linha);
                escritor.newLine();
            }
            System.out.println("Arquivo copiado com sucesso. | Copy File");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Erro ao copiar o arquivo.");
        }
    }

    public static void copy_pasta(File pastaOrigem, File destinoPai) {
        File destino = new File(destinoPai, pastaOrigem.getName());

        if (!destino.exists()) {
            destino.mkdirs();
        }

        for (File file : pastaOrigem.listFiles()) {
            File destFile = new File(destino, file.getName());
            if (file.isDirectory()) {
                copy_pasta(file, destino); // recursivo
            } else {
                copy_file(file, destFile);
            }
        }
    }

    public static void save_file(JTable tabela, File file) {

        if (tabela.isEditing()) {
            tabela.getCellEditor().stopCellEditing();
        }

        try {
            String path_completo = file.getPath();

            if (!path_completo.endsWith(".csv")) {
                path_completo += ".csv";
            }

            TableModel modelo = tabela.getModel();
            FileWriter writer = new FileWriter(path_completo);

            // Escreve os Cabeçalhos
            for (int i = 0; i < modelo.getColumnCount(); i++) {
                writer.write(modelo.getColumnName(i) + (i == modelo.getColumnCount() - 1 ? "\n" : ","));
            }

            // Escreve os Dados
            for (int i = 0; i < modelo.getRowCount(); i++) {
                for (int j = 0; j < modelo.getColumnCount(); j++) {
                    Object valor = modelo.getValueAt(i, j);
                    writer.write((valor != null ? valor.toString() : "") + (j == modelo.getColumnCount() - 1 ? "\n" : ","));
                }
            }

            writer.close();
            System.out.println("Arquivo salvo com Sucesso");
            Log.registrarErro_noEx("Arquivo: " + file.getAbsolutePath() + " - Salvo com Sucesso");

        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Erro ao salvar: " + e.getMessage());
        }
    }

    public static Properties init_properties() {
        Properties properties = new Properties();
        File config = new File("config.properties");

        if (config.exists()) {
            try (FileInputStream fis = new FileInputStream(config)) {
                properties.load(fis);
                System.out.println("Configuração carregada com sucesso.");
            } catch (IOException e) {
                System.err.println("Erro ao ler o arquivo de configuração:");
                e.printStackTrace();
            }
        } else {
            System.out.println("Arquivo de configuração não existe ainda.");
            properties.setProperty("Tema", "Default");
            try (FileOutputStream fos = new FileOutputStream(config)) {
                properties.store(fos, "Arquivo de configuração gerado");
            } catch (IOException e) {
                System.err.println("❌ Erro ao criar o arquivo de configuração:");
                e.printStackTrace();
            }
        }

        return properties;

    }

    public static void salvarConfiguracao(Properties props, String chave, String valor) {
        props.setProperty(chave, valor);
        try (FileOutputStream out = new FileOutputStream("config.properties")) {
            props.store(out, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Caixa de Mensagem Interface
    public static void message_error(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Erro", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean message_confirm(Component parent, String mensagem, String titulo) {
        int resposta = JOptionPane.showConfirmDialog(
                parent,
                mensagem,
                titulo,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        return resposta == JOptionPane.YES_OPTION;
    }

    public static void message_return(String message) {
        JOptionPane.showInputDialog(null, message);
    }

    public static String message_in(Component parent, String mensagem, String titulo) {
        String[] opcoes = {"Selecione sua Posição", "CTJ1b", "CTJ2b", "DTV1b", "DTV2b"};
        JComboBox<String> comboBox = new JComboBox<>(opcoes);

        // Mostrando o JOptionPane com a combo
        int result = JOptionPane.showConfirmDialog(
                parent,
                comboBox,
                "Selecione uma opção",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        // Verificando se o usuário clicou em OK
        if (result == JOptionPane.OK_OPTION) {
            String selecionado = (String) comboBox.getSelectedItem();
            if (selecionado.equals("Selecione sua Posição")) {
                return "";
            }
            return selecionado;
        } else {
            JOptionPane.showMessageDialog(null, "Operação cancelada.");
            return null;
        }
    }

    //
    public static void open_file_desktop(Component parent, File file) {
        try {
            Thread.sleep(1000);
            // Tenta abrir o arquivo com o aplicativo padrão do sistema
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            } else {
                System.out.println("Abertura automática não suportada neste sistema.");
            }
        } catch (IOException | InterruptedException e) {
            message_error(parent, "Erro ao abrir arquivo: " + file.getName());
        }
    }

    public static String gerarRetranca(String retranca) {
        int ponto = retranca.lastIndexOf(".");
        String nome = (ponto > 0) ? retranca.substring(0, ponto) : retranca;
        String ext = (ponto > 0) ? retranca.substring(ponto) : "";

        String hora = new SimpleDateFormat("HHmmss").format(new Date());
        String data = new SimpleDateFormat("ddMMyyyy").format(new Date());

        return hora + "_" + nome + "_" + data + ext;
    }
    // LGOS
}
