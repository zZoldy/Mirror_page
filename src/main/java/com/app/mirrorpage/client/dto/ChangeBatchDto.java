package com.app.mirrorpage.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChangeBatchDto {
    public java.util.List<ChangeDto> events;
    public long cursor;
}