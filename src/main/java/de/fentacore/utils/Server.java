package de.fentacore.utils;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import de.fentacore.endpoints.HelloWorld;
import de.fentacore.endpoints.DbSetup;


import java.net.URI;

import com.sun.net.httpserver.HttpServer;

public class Server {
    private static HttpServer server;

    public static void main(final String[] args) {
        startServer();

    }

    // Methode zum Starten des Servers
    public static void startServer() {
        final String pack = "dev.bsinfo.rest.ressource";
        String url = "http://localhost:8080/rest";

        System.out.println("Starting server...");
        System.out.println(url);


        final ResourceConfig rc = new ResourceConfig()
                .register(HelloWorld.class)
                .register(DbSetup.class);

     
        server = JdkHttpServerFactory.createHttpServer(URI.create(url), rc);

        System.out.println("Ready for Requests....");
    }

    // Methode zum Stoppen des Servers
    public static void stopServer() {

        if (server != null) {
            System.out.println("Stopping server...");
            server.stop(0);  // 0 bedeutet sofortiges Stoppen
            System.out.println("Server stopped.");
        } else {
            System.out.println("Server is not running.");
        }

    }

}
