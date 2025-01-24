package server;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String clientName;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Solicităm username-ul
            out.println("Welcome to MiauChat! Please enter your username:");
            clientName = in.readLine();

            // Confirmare conectare pentru client
            out.println("Connected as: " + clientName);

            System.out.println(clientName + " has joined the chat.");
            ChatServer.broadcast(clientName + " has joined the chat!", this);
            ChatServer.broadcastUserList();
            ChatServer.sendChatHistory(this);

            String message;
            while ((message = in.readLine()) != null) {
                if (message.equals("COMMAND:HISS")) {
                    ChatServer.broadcastCommand("COMMAND:HISS:" + clientName);// Send hiss to all
                } else {
                    Message chatMessage = new Message(clientName, message);
                    ChatServer.broadcastMessage(chatMessage, this);
                }
            }
        } catch (IOException e) {
            System.err.println("Error with client: " + e.getMessage());
        } finally {
            ChatServer.removeClient(this);
        }
    }

    public void sendMessage(String message) {
        synchronized (this) {
            if (out != null) {
                out.println(message);
            }
        }
    }

    public String getClientName() {
        return clientName;
    }
}
