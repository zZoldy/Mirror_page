/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.app.mirrorpage.client.dto;

public class FileLockEvent {
    public String path;
    public String owner;
    public boolean locked;
    public boolean contentChanged; // [NOVO]
}
