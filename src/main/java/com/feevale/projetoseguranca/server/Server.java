package com.feevale.projetoseguranca.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.feevale.projetoseguranca.ServerConfig;

public class Server {
    private static final int PORT = (int) ServerConfig.PORT.getValue();
    private static final int MAX_CLIENTS = (int) ServerConfig.MAX_CLIENTS.getValue();

    private List<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) {

        Server server = new Server();
        server.start();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (true) {
                if (clients.size() >= MAX_CLIENTS) {
                    System.out.println("Maximum number of clients reached");
                    continue;
                }

                // Wait for a client to connect
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);

                // Create a new thread to handle communication with the client
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                clientHandler.start();
            }
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        }
    }

    private void broadcastMessage(String message, ClientHandler sender) {
        System.out.println("Message received from client " + sender.getClientName() + ": " + message);
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
                System.out.println("Client " + clientName + " connected");

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
                System.out.println("Client " + clientName + " disconnected");
                clients.remove(this);
            } catch (IOException e) {
                System.err.println("Error communicating with client " + clientName + ": " + e.getMessage());
            }
        }

        public void sendMessage(String message) {
            try {
                OutputStream output = clientSocket.getOutputStream();
                output.write((message + "\n").getBytes());
            } catch (IOException e) {
                System.err.println("Error sending message to client " + clientName + ": " + e.getMessage());
            }
        }
    }
}
