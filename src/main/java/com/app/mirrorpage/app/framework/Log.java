
package com.app.mirrorpage.app.framework;

import com.app.mirrorpage.ui.jInternal_console;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
public class Log {

    public static void registrarErro(String contexto, Exception ex) {
        String mensagem = "[" + java.time.LocalDateTime.now() + "] "
                + "Erro em: " + contexto + " -> "
                + ex.getClass().getSimpleName() + ": "
                + ex.getMessage();

        try (java.io.FileWriter fw = new java.io.FileWriter("Logs.txt", true)) {
            fw.write(mensagem + System.lineSeparator());
        } catch (IOException ioe) {
            System.err.println("Falha ao escrever no log: " + ioe.getMessage());
        }

        System.err.println(mensagem); // Exibe no console

        // (Opcional) Escreve em arquivo de log
        try (java.io.FileWriter fw = new java.io.FileWriter("Logs.txt", true)) {
            fw.write(mensagem + System.lineSeparator());
        } catch (IOException ioe) {
            System.err.println("Falha ao escrever no log: " + ioe.getMessage());
        }
        jInternal_console.getInstance().append(jInternal_console.Level.INFO, contexto);
        jInternal_console.getInstance().append(jInternal_console.Level.ERROR, "Deu ruim!", ex);
        // (Opcional) Exibe em popup
        // JOptionPane.showMessageDialog(null, mensagem, "Erro", JOptionPane.ERROR_MESSAGE);
    }

    public static void registrarErro_noEx(String contexto) {
        String mensagem = "[" + java.time.LocalDateTime.now() + "] "
                + "Atenção em: " + contexto;

        System.err.println(mensagem); // Exibe no console

        // (Opcional) Escreve em arquivo de log
        try (java.io.FileWriter fw = new java.io.FileWriter("Logs.txt", true)) {
            fw.write(mensagem + System.lineSeparator());
        } catch (IOException ioe) {
            System.err.println("Falha ao escrever no log: " + ioe.getMessage());
        }

        jInternal_console.getInstance().append(jInternal_console.Level.INFO, contexto);

        // (Opcional) Exibe em popup
        // JOptionPane.showMessageDialog(null, mensagem, "Erro", JOptionPane.ERROR_MESSAGE);
    }

    public static void memory_log(String mensagem, String memoria) {
        String timetamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String linha = timetamp + " - " + mensagem + " - " + memoria;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("memory_log.txt", true))) {
            writer.write(linha);
            writer.newLine();
        } catch (IOException e) {
            Funcoes.message_error(null, "Erro ao gravar log de memória: " + e.getMessage());
        }
    }
}
