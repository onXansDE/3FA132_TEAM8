package de.fentacore.utils;

import de.fentacore.endpoints.Customers;
import de.fentacore.endpoints.Readings;
import de.fentacore.rest.CsvImportEndpoint;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import de.fentacore.endpoints.DbSetup;


import java.net.URI;

import com.sun.net.httpserver.HttpServer;

public class Server {
    private static HttpServer server;

    public static void main(final String[] args) {
        startServer("http://localhost:8080/rest");
    }

    public static void startServer(String url) {
        System.out.println("Starting server...");
        System.out.println(url);

        final ResourceConfig rc = new ResourceConfig()
                .register(DbSetup.class)
                .register(Customers.class)
                .register(Readings.class)
                .register(CsvImportEndpoint.class);

        server = JdkHttpServerFactory.createHttpServer(URI.create(url), rc);

        System.out.println("Ready for Requests....");
    }

    public static void stopServer() {
        if (server != null) {
            try {
                System.out.println("Stopping server...");
                server.stop(0);
                System.out.println("Server stopped.");
            } catch (Exception e) {
                System.out.println("Error stopping server: " + e.getMessage());
            } finally {
                server = null;
            }
        } else {
            System.out.println("Server is not running.");
        }
    }

}
