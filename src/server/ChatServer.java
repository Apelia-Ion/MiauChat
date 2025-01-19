package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private static final int PORT = 12345; // The port where the server will listen for client connections
    private static Set<ClientHandler> clientHandlers = ConcurrentHashMap.newKeySet(); // A thread-safe set to store active client handlers

    public static void main(String[] args) {
        System.out.println("MiauChat Server is running...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                // Accept incoming client connections
                Socket clientSocket = serverSocket.accept();
                System.out.println("A client has connected: " + clientSocket.getInetAddress());

                // Create a handler for the connected client and start a new thread
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandlers.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    // Broadcast a message to all connected clients except the sender
    public static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler clientHandler : clientHandlers) {
            if (clientHandler != sender) {
                clientHandler.sendMessage(message);
            }
        }
    }

    // Remove a client from the active clients set when it disconnects
    public static void removeClient(ClientHandler clientHandler) {
        clientHandlers.remove(clientHandler);
        System.out.println("A client has disconnected.");
    }
}

// This class handles the interaction with a single client
class ClientHandler implements Runnable {
    private Socket socket; // The client's socket
    private PrintWriter out; // Output stream to send messages to the client
    private String clientName; // The name of the connected client

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                // Input stream to read messages from the client
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        ) {
            out = new PrintWriter(socket.getOutputStream(), true); // Initialize the output stream

            // Send a welcome message to the client
            out.println("Welcome to MiauChat! Please enter your username:");

            // Read the username from the client
            clientName = in.readLine();
            sendMessage("You joined the chat successfully as: " + clientName);
            System.out.println(clientName + " has joined the chat.");
            ChatServer.broadcast(clientName + " has joined the chat!", this);

            // Listen for messages from the client
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println(clientName + ": " + message);
                ChatServer.broadcast(clientName + ": " + message, this);
            }
        } catch (IOException e) {
            System.err.println("Error with client: " + e.getMessage());
        } finally {
            // Remove the client from the active set and close the socket
            ChatServer.removeClient(this);
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing the connection: " + e.getMessage());
            }
        }
    }

    // Send a message to this client
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
}
