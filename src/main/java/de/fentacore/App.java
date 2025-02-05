package de.fentacore;

import de.fentacore.config.DatabaseConfig;
import de.fentacore.utils.CSVImporter;
import de.fentacore.utils.Server;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;


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
        Server.startServer();

    }
}
