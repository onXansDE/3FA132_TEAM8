package de.fentacore;

import de.fentacore.config.DatabaseConfig;
import de.fentacore.utils.Server;

public class App {
    public String greet(String name) {
        return "Hello, " + name + "!";
    }

    public static void main(String[] args) {

        if (!DatabaseConfig.checkTablesExist()) {

            DatabaseConfig.createTables();
        }

        Server.startServer("http://localhost:8080/rest");

    }
}
