package de.fentacore;

import de.fentacore.utils.CSVImporter;

import java.time.LocalDate;

public class App {
    public String greet(String name) {
        return "Hello, " + name + "!";
    }

    public static void main(String[] args) {
        CSVImporter importer = new CSVImporter();
        importer.importAll("data");
    }
}
