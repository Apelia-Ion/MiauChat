package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private static final int PORT = 12345;
    private static final String HISTORY_FILE = "chat_history.ser";
    private static Set<ClientHandler> clientHandlers = ConcurrentHashMap.newKeySet();
    private static List<Message> chatHistory = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        loadChatHistory();

        System.out.println("MiauChat Server is running...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("A client has connected: " + clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandlers.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }

        Runtime.getRuntime().addShutdownHook(new Thread(ChatServer::saveChatHistory));
    }

    private static void saveChatHistory() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(HISTORY_FILE))) {
            oos.writeObject(chatHistory);
            System.out.println("Chat history saved.");
        } catch (IOException e) {
            System.err.println("Error saving chat history: " + e.getMessage());
        }
    }

    private static void loadChatHistory() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(HISTORY_FILE))) {
            chatHistory = (List<Message>) ois.readObject();
            System.out.println("Chat history loaded.");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("No previous chat history found or error loading it.");
        }
    }

    public static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler clientHandler : clientHandlers) {
            if (clientHandler != sender) {
                clientHandler.sendMessage(message);
            }
        }
    }

    public static void broadcastMessage(Message message, ClientHandler sender) {
        chatHistory.add(message);
        saveChatHistory();

        for (ClientHandler clientHandler : clientHandlers) {
            if (clientHandler != sender) {
                clientHandler.sendMessage(message.toString());
            }
        }
    }

    public static void sendChatHistory(ClientHandler clientHandler) {
        for (Message message : chatHistory) {
            clientHandler.sendMessage(message.toString());
        }
    }

    public static void broadcastUserList() {
        String userListMessage = "USER_LIST:" + String.join(",", getUserNames());
        for (ClientHandler clientHandler : clientHandlers) {
            clientHandler.sendMessage(userListMessage);
        }
    }

    private static List<String> getUserNames() {
        List<String> userNames = new ArrayList<>();
        for (ClientHandler clientHandler : clientHandlers) {
            userNames.add(clientHandler.getClientName());
        }
        return userNames;
    }

    public static void removeClient(ClientHandler clientHandler) {
        clientHandlers.remove(clientHandler);
        System.out.println(clientHandler.getClientName() + " has disconnected.");
        broadcastUserList();
    }
}
