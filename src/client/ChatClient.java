package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

public class ChatClient {
    private static final String SERVER_ADDRESS = "localhost"; // Address of the server
    private static final int SERVER_PORT = 12345; // Port of the server

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // GUI Components
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;

    public ChatClient() {
        initializeGUI();

        try {
            // Connect to the server
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Start a thread to listen for messages from the server
            new Thread(this::listenForMessages).start();
        } catch (IOException e) {
            showError("Unable to connect to the server: " + e.getMessage());
        }
    }

    private void initializeGUI() {
        // Initialize the GUI
        frame = new JFrame("MiauChat - Client");
        frame.setSize(400, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        sendButton = new JButton("Send Miau!");

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        frame.add(inputPanel, BorderLayout.SOUTH);

        // Add action listener for the send button
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        // Add enter key listener to send messages
        messageField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        frame.setVisible(true);
    }

    private void sendMessage() {
        String message = messageField.getText();
        if (!message.trim().isEmpty()) {
            out.println(message); // Send the message to the server
            messageField.setText(""); // Clear the input field
        }
    }

    private void listenForMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                chatArea.append(message + "\n"); // Display the message in the chat area
            }
        } catch (IOException e) {
            showError("Disconnected from the server.");
        } finally {
            closeConnection();
        }
    }

    private void showError(String errorMessage) {
        JOptionPane.showMessageDialog(frame, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void closeConnection() {
        try {
            if (socket != null) socket.close();
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatClient::new);
    }
}
