package com.feevale.projetoseguranca.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.feevale.projetoseguranca.ServerConfig;

public class Server {
    // Server configs
    private static final int PORT = (int) ServerConfig.PORT.getValue();
    private static final int MAX_CLIENTS = (int) ServerConfig.MAX_CLIENTS.getValue();
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());

    private List<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) {

        Server server = new Server();
        server.start();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            LOGGER.log(Level.INFO, "Server started on port: " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                LOGGER.log(Level.INFO, "Current clients: " + clients.size() + " of " + MAX_CLIENTS + ".");
                if (clients.size() >= MAX_CLIENTS) {
                    LOGGER.log(Level.WARNING, "Maximum number of clients reached");
                    try (OutputStream output = clientSocket.getOutputStream()) {
                        output.write("Server is full, please try again later\n".getBytes());
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, "Error sending message to client: " + e.getMessage());
                    } finally {
                        clientSocket.close();
                    }
                    continue;
                }

                // Wait for a client to connect
                LOGGER.log(Level.INFO, "New client connected: " + clientSocket);

                // Create a new thread to handle communication with the client
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                clientHandler.start();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error starting server: " + e.getMessage());
        }
    }

    private void broadcastMessage(String message, ClientHandler sender) {
        LOGGER.log(Level.INFO, "Message received from client " + sender.getClientName() + ": " + message);
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(sender.getClientName() + ": " + message);
            }
        }

    }

    private class ClientHandler extends Thread {
        private Socket clientSocket;
        private String clientName;
        private byte[] buffer = new byte[1024];

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        public String getClientName() {
            return clientName;
        }

        @Override
        public void run() {
            try (
                    InputStream input = clientSocket.getInputStream();
                    OutputStream output = clientSocket.getOutputStream();) {
                int bytesRead = input.read(buffer);
                clientName = new String(buffer, 0, bytesRead).trim();
                LOGGER.log(Level.INFO, "Client " + clientName + " connected");

                // Send a welcome message to the client
                output.write(("Welcome, " + clientName + "!\n").getBytes());

                // Loop to read messages sent by the client
                while (true) {
                    bytesRead = input.read(buffer);
                    if (bytesRead == -1) {
                        break;
                    }
                    String message = new String(buffer, 0, bytesRead).trim();
                    broadcastMessage(message, this);
                    sendMessage("You said: " + message);
                }

                // Client disconnected
                LOGGER.log(Level.INFO, "Client " + clientName + " disconnected");
                clients.remove(this);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error communicating with client " + clientName + ": " + e.getMessage());
            }
        }

        public void sendMessage(String message) {
            try {
                OutputStream output = clientSocket.getOutputStream();
                output.write((message + "\n").getBytes());
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error sending message to client " + clientName + ": " + e.getMessage());
            }
        }
    }
}
