package com.app.mirrorpage.client.dto;

public record RowInsertedEvent(
        String path,
        int afterRow,
        String user
        ) {

}
