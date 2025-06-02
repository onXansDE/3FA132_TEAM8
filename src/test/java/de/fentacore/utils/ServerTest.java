package de.fentacore.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ServerTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    public void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        
        // Always try to stop the server after each test
        try {
            Server.stopServer();
        } catch (Exception e) {
            // Ignore exceptions during cleanup
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testStartServer_Success() throws Exception {
        String testUrl = "http://localhost:8081/rest";
        
        // Start server
        Server.startServer(testUrl);
        
        // Verify console output
        String output = outContent.toString();
        assertTrue(output.contains("Starting server..."));
        assertTrue(output.contains(testUrl));
        assertTrue(output.contains("Ready for Requests...."));
        
        // Verify server is actually running by making a request
        Thread.sleep(1000); // Give server time to start
        
        URL url = new URL(testUrl + "/dbsetup");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        // Should get a response (even if it's an error, it means server is running)
        int responseCode = connection.getResponseCode();
        assertTrue(responseCode > 0, "Server should be responding");
        
        connection.disconnect();
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testStartServer_DifferentPort() throws Exception {
        String testUrl = "http://localhost:8082/rest";
        
        // Start server on different port
        Server.startServer(testUrl);
        
        // Verify console output contains the correct URL
        String output = outContent.toString();
        assertTrue(output.contains(testUrl));
        
        // Verify server is running on the specified port
        Thread.sleep(1000);
        
        URL url = new URL(testUrl + "/dbsetup");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        
        int responseCode = connection.getResponseCode();
        assertTrue(responseCode > 0, "Server should be responding on port 8082");
        
        connection.disconnect();
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testStopServer_WhenRunning() throws Exception {
        String testUrl = "http://localhost:8083/rest";
        
        // Start server first
        Server.startServer(testUrl);
        Thread.sleep(1000);
        
        // Clear output to focus on stop message
        outContent.reset();
        
        // Stop server
        Server.stopServer();
        
        // Verify console output
        String output = outContent.toString();
        assertTrue(output.contains("Stopping server..."));
        assertTrue(output.contains("Server stopped."));
        
        // Verify server is actually stopped by trying to connect
        Thread.sleep(1000);
        
        try {
            URL url = new URL(testUrl + "/dbsetup");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);
            
            // This should fail since server is stopped
            connection.getResponseCode();
            fail("Connection should have failed since server is stopped");
        } catch (Exception e) {
            // Expected - server should be stopped
            assertTrue(true, "Server is properly stopped");
        }
    }

    @Test
    public void testStopServer_WhenNotRunning() {
        // Try to stop server when it's not running
        // This should not throw an exception, just print a message
        assertDoesNotThrow(() -> {
            Server.stopServer();
        });
        
        // Verify console output
        String output = outContent.toString();
        assertTrue(output.contains("Server is not running."));
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testMultipleStopCalls() throws Exception {
        String testUrl = "http://localhost:8084/rest";
        
        // Start server
        Server.startServer(testUrl);
        Thread.sleep(1000);
        
        // Stop server first time
        Server.stopServer();
        
        // Clear output and stop again
        outContent.reset();
        
        // Second stop should not throw exception, just indicate server is not running
        assertDoesNotThrow(() -> {
            Server.stopServer();
        });
        
        // Second stop should indicate server is not running
        String output = outContent.toString();
        assertTrue(output.contains("Server is not running."));
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testStartServer_WithDifferentPaths() throws Exception {
        String testUrl = "http://localhost:8085/api/v1";
        
        // Start server with different path
        Server.startServer(testUrl);
        
        // Verify console output
        String output = outContent.toString();
        assertTrue(output.contains("Starting server..."));
        assertTrue(output.contains(testUrl));
        
        Thread.sleep(1000);
        
        // Verify server responds on the correct path
        URL url = new URL(testUrl + "/dbsetup");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        
        int responseCode = connection.getResponseCode();
        assertTrue(responseCode > 0, "Server should be responding on custom path");
        
        connection.disconnect();
    }

    @Test
    public void testStartServer_InvalidURL() {
        // Test with invalid URL format
        assertThrows(Exception.class, () -> {
            Server.startServer("invalid-url");
        });
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testStartServer_PortAlreadyInUse() throws Exception {
        String testUrl = "http://localhost:8086/rest";
        
        // Start first server
        Server.startServer(testUrl);
        Thread.sleep(1000);
        
        // Try to start another server on the same port
        assertThrows(Exception.class, () -> {
            Server.startServer(testUrl);
        });
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    public void testServerLifecycle() throws Exception {
        String testUrl = "http://localhost:8087/rest";
        
        // Test complete lifecycle: start -> verify -> stop -> verify
        
        // 1. Start server
        Server.startServer(testUrl);
        Thread.sleep(1000);
        
        // 2. Verify it's running
        URL url = new URL(testUrl + "/dbsetup");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        int responseCode = connection.getResponseCode();
        assertTrue(responseCode > 0, "Server should be running");
        connection.disconnect();
        
        // 3. Stop server
        Server.stopServer();
        Thread.sleep(1000);
        
        // 4. Verify it's stopped
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(2000);
            connection.getResponseCode();
            fail("Server should be stopped");
        } catch (Exception e) {
            // Expected - server should be stopped
            assertTrue(true, "Server lifecycle test passed");
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testMainMethod() throws Exception {
        // Test the main method (which starts server on default URL)
        // We'll run this in a separate thread to avoid blocking
        Thread mainThread = new Thread(() -> {
            try {
                // We can't easily test main() directly since it would block,
                // but we can test that startServer works with the default URL
                Server.startServer("http://localhost:8088/rest");
            } catch (Exception e) {
                // Handle any exceptions
            }
        });
        
        mainThread.start();
        Thread.sleep(2000);
        
        // Verify server started
        String output = outContent.toString();
        assertTrue(output.contains("Starting server..."));
        assertTrue(output.contains("Ready for Requests...."));
        
        // Clean up
        Server.stopServer();
        mainThread.interrupt();
    }

    @Test
    public void testConsoleOutput_Format() throws Exception {
        String testUrl = "http://localhost:8089/rest";
        
        // Start server and capture output
        Server.startServer(testUrl);
        
        String output = outContent.toString();
        
        // Verify output format and content
        assertTrue(output.contains("Starting server..."), "Should contain startup message");
        assertTrue(output.contains(testUrl), "Should contain the URL");
        assertTrue(output.contains("Ready for Requests...."), "Should contain ready message");
        
        // Verify output order (startup message should come before URL)
        int startupIndex = output.indexOf("Starting server...");
        int urlIndex = output.indexOf(testUrl);
        int readyIndex = output.indexOf("Ready for Requests....");
        
        assertTrue(startupIndex < urlIndex, "Startup message should come before URL");
        assertTrue(urlIndex < readyIndex, "URL should come before ready message");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testServerEndpoints_Registration() throws Exception {
        String testUrl = "http://localhost:8090/rest";
        
        // Start server
        Server.startServer(testUrl);
        Thread.sleep(1000);
        
        // Test that registered endpoints are accessible
        // Test DbSetup endpoint
        URL dbSetupUrl = new URL(testUrl + "/dbsetup");
        HttpURLConnection dbSetupConnection = (HttpURLConnection) dbSetupUrl.openConnection();
        dbSetupConnection.setRequestMethod("GET");
        dbSetupConnection.setConnectTimeout(5000);
        
        int dbSetupResponse = dbSetupConnection.getResponseCode();
        assertTrue(dbSetupResponse > 0, "DbSetup endpoint should be accessible");
        dbSetupConnection.disconnect();
        
        // Test Customers endpoint
        URL customersUrl = new URL(testUrl + "/customers");
        HttpURLConnection customersConnection = (HttpURLConnection) customersUrl.openConnection();
        customersConnection.setRequestMethod("GET");
        customersConnection.setConnectTimeout(5000);
        
        int customersResponse = customersConnection.getResponseCode();
        assertTrue(customersResponse > 0, "Customers endpoint should be accessible");
        customersConnection.disconnect();
    }
} 