package com.feevale.projetoseguranca.client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.sql.Date;
import java.text.SimpleDateFormat;

import com.feevale.projetoseguranca.ServerConfig;

public class ChatClient {

    private static Socket socket; // Declare socket as a class-level field


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
                textArea.append(String.format("[%s]: Me: %s\n", formattedDate, message));
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

            // Get username from user
            String username = JOptionPane.showInputDialog(frame, "Digite seu nome de usuário:");
            sendMessageToServer(username, socket);

            // Listen for messages from server
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                Date currentDate = new Date(System.currentTimeMillis());
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String formattedDate = dateFormat.format(currentDate);
                System.out.println(String.format("%s - Message from server: %s", formattedDate, line));
                textArea.append(String.format("[%s]: %s\n", formattedDate, line));
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
            writer.println(message);
        } catch (IOException e) {
            System.err.println("Error sending message to server: " + e.getMessage());
        }
    }
}
