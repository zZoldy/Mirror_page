package com.app.mirrorpage.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChangeDto {

    public String type;     // CREATE | UPDATE | DELETE | RENAME
    public String path;
    public String newPath;  // opcional em RENAME
    public boolean dir;

    // opcional: se quiser enxergar o cursor individual do evento
    public long cursor;     // se não quiser usar, pode deixar assim mesmo
}
