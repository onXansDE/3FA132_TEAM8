package de.fentacore;

import de.fentacore.config.DatabaseConfig;
import de.fentacore.utils.CSVImporter;
import de.fentacore.utils.Server;

import java.time.LocalDate;

public class App {
    public String greet(String name) {
        return "Hello, " + name + "!";
    }

    public static void main(String[] args) {

        if (!DatabaseConfig.checkTablesExist()) {

            DatabaseConfig.createTables();
        }

//        CSVImporter importer = new CSVImporter();
//        importer.importAll("data");
        Server.startServer("localhost:420");

    }
}
