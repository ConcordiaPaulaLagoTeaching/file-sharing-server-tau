package ca.concordia.server;

import ca.concordia.filesystem.FileSystemManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/*
 * TODO: Review and comment code properly
 */

/**
 * FileServer accepts client connections and dispatches file system commands.
 * Each client is handled in a separate thread.
 */
public class FileServer {

    private final FileSystemManager fsManager;
    private final int port;

    public FileServer(int port, String fileSystemName, int totalSize) {
        this.fsManager = new FileSystemManager(fileSystemName, totalSize);
        this.port = port;
    }

    
    //Starts the server and listens for incoming client connections.
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started. Listening on port " + port + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Connected to client: " + clientSocket);

                // Spawn a new thread for each client
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (Exception e) {
            System.err.println("Could not start server on port " + port);
            e.printStackTrace();
        }
    }

    
    //Handles a single client connection in a dedicated thread.
    private void handleClient(Socket clientSocket) {
        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Received from client: " + line);
                String[] parts = line.trim().split(" ", 3); // max 3 parts: command, filename, content
                String command = parts[0].toUpperCase();

                try {
                    switch (command) {
                        case "CREATE":
                            if (parts.length < 2) {
                                writer.println("ERROR: Missing filename");
                            } else if (parts[1].length() > 11) {
                                writer.println("ERROR: Filename too long (max 11 chars)");
                            } else {
                                fsManager.createFile(parts[1]);
                                writer.println("SUCCESS: File '" + parts[1] + "' created.");
                            }
                            break;

                        case "WRITE":
                            if (parts.length < 3) {
                                writer.println("ERROR: Missing filename or content");
                            } else {
                                fsManager.writeFile(parts[1], parts[2].getBytes());
                                writer.println("SUCCESS: File '" + parts[1] + "' written.");
                            }
                            break;

                        case "READ":
                            if (parts.length < 2) {
                                writer.println("ERROR: Missing filename");
                            } else {
                                byte[] data = fsManager.readFile(parts[1]);
                                writer.println("SUCCESS: " + new String(data));
                            }
                            break;

                        case "DELETE":
                            if (parts.length < 2) {
                                writer.println("ERROR: Missing filename");
                            } else {
                                fsManager.deleteFile(parts[1]);
                                writer.println("SUCCESS: File '" + parts[1] + "' deleted.");
                            }
                            break;

                        case "LIST":
                            String[] files = fsManager.listFiles();
                            writer.println("SUCCESS: " + String.join(", ", files));
                            break;

                        case "QUIT":
                            writer.println("SUCCESS: Disconnecting.");
                            return;

                        default:
                            writer.println("ERROR: Unknown command.");
                            break;
                    }
                } catch (Exception e) {
                    writer.println("ERROR: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Client handler error:");
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (Exception ignore) {}
        }
    }
}