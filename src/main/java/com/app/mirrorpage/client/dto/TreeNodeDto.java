package com.app.mirrorpage.client.dto;

public class TreeNodeDto {

    public String name;
    public String path;   // começa com "/" (ex.: "/DF1")
    public boolean dir;   // true = diretório
    public long size;     // 0 para diretórios
    public long mtime;    // epoch millis

    @Override
    public String toString() {
        return name;
    }
}
