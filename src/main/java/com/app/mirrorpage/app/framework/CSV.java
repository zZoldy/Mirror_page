package com.app.mirrorpage.app.framework;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTable;
import javax.swing.table.TableModel;

public class CSV {
    
    public static void gravar(JTable table, Path destino, String separador) {
        TableModel model = table.getModel();
        
        try (BufferedWriter writer = Files.newBufferedWriter(destino, StandardCharsets.UTF_8)) {
            
            List<String> header = new ArrayList<>();
            
            for (int col = 0; col < model.getColumnCount(); col++) {
                String nomeCol = model.getColumnName(col);
                nomeCol = escapeCsv(nomeCol, separador);
                header.add(nomeCol);
            }
            
            writer.write(String.join(separador, header));
            writer.newLine();
            
            for (int row = 0; row < model.getRowCount(); row++) {
                List<String> campos = new ArrayList<>();
                
                for (int col = 0; col < model.getColumnCount(); col++) {
                    Object valorBruto = model.getValueAt(row, col);
                    String texto;
                    if (valorBruto == null) {
                        texto = "";
                    } else {
                        texto = valorBruto.toString();
                    }
                    campos.add(escapeCsv(texto, separador));
                }
                
                writer.write(String.join(separador, campos));
                writer.newLine();
            }
            
        } catch (IOException e) {
            Log.registrarErro("[CSV] Erro ao salvar: " + destino, e);
            System.out.println("Erro: " + e);
        }
        
    }
    
    static String escapeCsv(String texto, String separador) {
        if (texto == null) {
            return "";
        }
        
        boolean contemSeparador = texto.contains(separador);
        boolean contemQuebraLinha = texto.contains("\n") || texto.contains("\r");
        boolean contemAspas = texto.contains("\"");
        
        if (contemSeparador || contemQuebraLinha || contemAspas) {
            texto = texto.replace("\"", "\"\"");
            
            texto = "\"" + texto + "\"";
        }
        
        return texto;
    }
    
}
