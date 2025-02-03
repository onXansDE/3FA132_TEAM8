package de.fentacore.utils;

public class Server {

    private static boolean isRunning = false;
    private static String serverUrl;

    /**
     * Startet den Server mit der angegebenen URL.
     */
    public static void startServer(String url) {
        if (!isRunning) {
            serverUrl = url;
            isRunning = true;
            System.out.println("Server gestartet unter: " + serverUrl);
        } else {
            System.out.println("Server läuft bereits unter: " + serverUrl);
        }
    }

    /**
     * Stoppt den aktuell laufenden Server.
     */
    public static void stopServer() {
        if (isRunning) {
            System.out.println("Server gestoppt: " + serverUrl);
            isRunning = false;
            serverUrl = null;
        } else {
            System.out.println("Kein Server läuft aktuell.");
        }
    }

    public static void main(String[] args) {
        startServer("http://localhost:420");
        stopServer();
    }
}