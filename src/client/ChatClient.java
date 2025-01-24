package client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import utils.SoundUtils;
import utils.BackgroundPanel;

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
    private JButton hissButton; // New button for "Hiss"
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

        // Creează panoul de fundal
        BackgroundPanel backgroundPanel = new BackgroundPanel("bg.jpg");
        backgroundPanel.setLayout(new BorderLayout()); // Pentru a adăuga alte componente

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);

        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        JPanel inputPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        sendButton = new JButton("Send");
        hissButton = new JButton("Hiss");

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        inputPanel.add(hissButton, BorderLayout.WEST);

        backgroundPanel.add(chatScrollPane, BorderLayout.CENTER);
        backgroundPanel.add(inputPanel, BorderLayout.SOUTH);

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setPreferredSize(new Dimension(150, 0));

        frame.add(backgroundPanel, BorderLayout.CENTER);
        frame.add(new JScrollPane(userList), BorderLayout.EAST);

        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());
        hissButton.addActionListener(e -> sendHiss());

        frame.setVisible(true);
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            out.println(message);
            if (clientName != null) {
                chatArea.append("You: " + message + "\n");
            }
            messageField.setText("");
        }
    }

    private void sendHiss() {
        out.println("COMMAND:HISS"); // Send hiss command to server
    }

    private void listenForMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("Connected as:")) {
                    clientName = message.split(":")[1].trim();
                    chatArea.append(message + "\n");
                } else if (message.startsWith("USER_LIST:")) {
                    updateUserList(message.substring(10).split(","));
                }else if (message.startsWith("COMMAND:HISS:")) {
                    String sender = message.split(":")[2]; // Extrage numele expeditorului
                    SoundUtils.playSound("resources/hiss.wav");
                    chatArea.append("*HISS* from " + sender + "\n"); // Afișează cine a trimis
                } else {
                    chatArea.append(message + "\n");
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
