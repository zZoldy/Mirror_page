/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.app.mirrorpage.app.model.file;

public enum CsvModelType {
    BOLETIM_CTL1("Boletim_ctl1.csv", new Boletim_ctl1()),
    BOLETIM_CTL2("Boletim_ctl2.csv", new Boletim_ctl2()),
    PRELIMINAR("Prelim.csv", new Prelim()),
    FINAL("Final.csv", new Final());

    private final String defaultFileName;
    private final CsvModel model;

    CsvModelType(String defaultFileName, CsvModel model) {
        this.defaultFileName = defaultFileName;
        this.model = model;
    }

    public String defaultFileName() {
        return defaultFileName;
    }

    public CsvModel model() {
        return model;
    }
}
