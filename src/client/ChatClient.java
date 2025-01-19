package client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import utils.SoundUtils;


public class ChatClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private String clientName;

    public ChatClient() {
        initializeGUI();

        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            new Thread(this::listenForMessages).start();
        } catch (IOException e) {
            showError("Unable to connect to the server: " + e.getMessage());
        }
    }

    private void initializeGUI() {
        frame = new JFrame("MiauChat - Client");
        frame.setSize(600, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);

        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        sendButton = new JButton("Send");
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setPreferredSize(new Dimension(150, 0));
        frame.add(chatPanel, BorderLayout.CENTER);
        frame.add(new JScrollPane(userList), BorderLayout.EAST);

        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());

        frame.setVisible(true);
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            // Trimitem mesajul serverului
            out.println(message);

            // Afișăm "You: ..." doar pentru mesajele trimise ulterior conectării
            if (clientName != null) {
                chatArea.append("You: " + message + "\n");
            }

            messageField.setText(""); // Golim câmpul de text
        }
    }


    private void listenForMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                // Procesăm mesajul de conectare
                if (message.startsWith("Connected as:")) {
                    clientName = message.split(":")[1].trim(); // Salvăm numele utilizatorului
                    chatArea.append(message + "\n");
                } else if (message.startsWith("USER_LIST:")) {
                    updateUserList(message.substring(10).split(","));
                } else {
                    // Afișăm restul mesajelor
                    chatArea.append(message + "\n");

                    // Redăm sunetul "meow" pentru fiecare mesaj primit
                    if (!message.startsWith("You:")) {
                        SoundUtils.playSound("resources/meow.wav");
                    }
                }
            }
        } catch (IOException e) {
            showError("Disconnected from the server.");
        } finally {
            closeConnection();
        }
    }


    private void updateUserList(String[] users) {
        userListModel.clear();
        for (String user : users) {
            userListModel.addElement(user);
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
