package com.feevale.projetoseguranca.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import com.feevale.projetoseguranca.ServerConfig;
import com.feevale.projetoseguranca.util.CryptoUtil;

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
        private SecretKey secretKey;
        private IvParameterSpec iv;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
            try {
                this.secretKey = CryptoUtil.generateSecretKey();
                this.iv = CryptoUtil.generateIv();
            } catch (NoSuchAlgorithmException e) {
                LOGGER.log(Level.SEVERE, "Error generating secret key: " + e.getMessage());
            }
        }

        public String getClientName() {
            return clientName;
        }

        @Override
        public void run() {
            try (
                    InputStream input = clientSocket.getInputStream();
                    OutputStream output = clientSocket.getOutputStream();) {
                // Send the secret key and iv to the client
                output.write(secretKey.getEncoded());
                output.write(iv.getIV());

                int bytesRead = input.read(buffer);
                String encryptedMessage = new String(buffer, 0, bytesRead).trim();
                clientName = CryptoUtil.decrypt(encryptedMessage, secretKey, iv).trim();
                LOGGER.log(Level.INFO, "Client " + clientName + " connected");

                // Send a welcome message to the client
                String welcomeMessage = "Welcome, " + clientName + "!";
                String encryptedWelcomeMessage = CryptoUtil.encrypt(welcomeMessage, secretKey, iv);
                output.write((encryptedWelcomeMessage + "\n").getBytes());

                // Loop to read messages sent by the client
                while (true) {
                    bytesRead = input.read(buffer);
                    if (bytesRead == -1) {
                        break;
                    }
                    encryptedMessage = new String(buffer, 0, bytesRead).trim();
                    String message = CryptoUtil.decrypt(encryptedMessage, secretKey, iv);
                    broadcastMessage(message, this);
                    sendMessage("You said: " + message);
                }

                // Client disconnected
                LOGGER.log(Level.INFO, "Client " + clientName + " disconnected");
                clients.remove(this);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error communicating with client " + clientName + ": " + e.getMessage());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error decrypting message from client " + clientName + ": " + e.getMessage());
            }
        }

        public void sendMessage(String message) {
            try {
                OutputStream output = clientSocket.getOutputStream();
                String encryptedMessage = CryptoUtil.encrypt(message, secretKey, iv);
                output.write((encryptedMessage + "\n").getBytes());
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error sending message to client " + clientName + ": " + e.getMessage());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error encrypting message for client " + clientName + ": " + e.getMessage());
            }
        }        
    }
}
