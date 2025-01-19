package server;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private String clientName;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            out = new PrintWriter(socket.getOutputStream(), true);

            // Solicităm username-ul
            out.println("Welcome to MiauChat! Please enter your username:");
            clientName = in.readLine();

            // Trimitem mesajul de conectare doar acestui client
            out.println("You successfully connected as: " + clientName);

            System.out.println(clientName + " has joined the chat.");
            ChatServer.sendChatHistory(this); // Trimitem istoricul conversațiilor
            ChatServer.broadcast(clientName + " has joined the chat!", this);
            ChatServer.broadcastUserList(); // Actualizăm lista de utilizatori

            String message;
            while ((message = in.readLine()) != null) {
                Message chatMessage = new Message(clientName, message);
                ChatServer.broadcastMessage(chatMessage);
            }
        } catch (IOException e) {
            System.err.println("Error with client: " + e.getMessage());
        } finally {
            ChatServer.removeClient(this);
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public String getClientName() {
        return clientName;
    }
}
