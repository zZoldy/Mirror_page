/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.app.mirrorpage.client.dto;

public record RowMoveEvent(
        String path,   // "/BDBR/Prelim.csv"
        int from,      // índice origem (lado cliente / servidor)
        int to         // índice destino
) {}
