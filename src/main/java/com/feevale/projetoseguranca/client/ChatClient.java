package com.feevale.projetoseguranca.client;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.*;
import java.sql.Date;
import java.text.SimpleDateFormat;

import com.feevale.projetoseguranca.ServerConfig;
import com.feevale.projetoseguranca.util.CryptoUtil;

public class ChatClient {

    private static Socket socket;
    private static SecretKey secretKey;
    private static IvParameterSpec iv;
    private static final Logger LOGGER = Logger.getLogger(ChatClient.class.getName());


    public static void main(String[] args) {
        JFrame frame = new JFrame("Chat Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());

        // Create JTextArea for displaying messages
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        // Create JTextField and JButton for writing/sending messages
        JTextField messageField = new JTextField();
        JButton sendButton = new JButton("Enviar");
        sendButton.addActionListener(action -> {
            String message = messageField.getText();
            if (message.trim().length() > 0) {
                Date currentDate = new Date(System.currentTimeMillis());
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String formattedDate = dateFormat.format(currentDate);
                System.out.println(String.format("%s - Message: %s", formattedDate, message));
                sendMessageToServer(message, socket);
                messageField.setText(""); // Clear messageField for next message
            }
        });

        // Create JPanel for holding messageField and sendButton
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        bottomPanel.add(messageField);
        bottomPanel.add(sendButton);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        frame.add(bottomPanel, BorderLayout.SOUTH);

        frame.setVisible(true);

        // Connect to server
        try {
            socket = new Socket((String) ServerConfig.HOST.getValue(), (int) ServerConfig.PORT.getValue());
            System.out.println("Connected to server");

            // Receive the secret key and iv from the server
            InputStream input = socket.getInputStream();
            byte[] secretKeyBytes = new byte[32];
            byte[] ivBytes = new byte[16];
            input.read(secretKeyBytes);
            input.read(ivBytes);
            secretKey = new SecretKeySpec(secretKeyBytes, "AES");
            iv = new IvParameterSpec(ivBytes);

            // Get username from user
            String username = "";
            while (username.trim().equals("")) {
               username = JOptionPane.showInputDialog(frame, "Digite seu nome de usuário:");
            }
            sendMessageToServer(username, socket);

            // Listen for messages from server
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (isValidBase64(line)) {
                    try {
                        String decryptedMessage = CryptoUtil.decrypt(line, secretKey, iv);
                        Date currentDate = new Date(System.currentTimeMillis());
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String formattedDate = dateFormat.format(currentDate);
                        LOGGER.log(Level.INFO, "Received message from server encrypted: " + line);
                        textArea.append(String.format("[%s]: %s\n", formattedDate, decryptedMessage));
                    } catch (Exception e) {
                        System.err.println("Error decrypting message from server: " + e.getMessage());
                    }
                } else {
                    System.err.println("Received an invalid Base64 encoded message from server: " + line);
                }
            }
            
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // ignore exception
                }
            }
        }
    }

    private static void sendMessageToServer(String message, Socket socket) {
        try {
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            String encryptedMessage = CryptoUtil.encrypt(message, secretKey, iv);
            LOGGER.log(Level.INFO, "Sending message to server: " + encryptedMessage);
            writer.println(encryptedMessage);
        } catch (Exception e) {
            System.err.println("Error sending message to server: " + e.getMessage());
        }
    }

    public static boolean isValidBase64(String input) {
        try {
            Base64.getDecoder().decode(input);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}

