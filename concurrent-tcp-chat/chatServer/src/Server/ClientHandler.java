package Server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements Runnable {

    public static List<ClientHandler> clientHandlers = new ArrayList<>();

    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUsername;

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.clientUsername = bufferedReader.readLine();
            synchronized (clientHandlers) {
                clientHandlers.add(this);
            }
            System.out.println("Client " + clientUsername + " connected");
            broadcastMessage("SERVER: " + clientUsername + " has entered the chat");
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    @Override
    public void run() {
        String messageFromClient;

        while (socket.isConnected()) {
            try {
                messageFromClient = bufferedReader.readLine();
                if (messageFromClient != null) {
                    if (messageFromClient.startsWith("/")) {
                        handleCommand(messageFromClient);
                    } else {
                        broadcastMessage(clientUsername + ": " + messageFromClient);
                    }
                }
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }
        }
    }

    private void handleCommand(String messageFromClient) {
        String[] tokens = messageFromClient.split(" ", 3);
        String command = tokens[0];

        switch (command) {
            case "/list":
                sendList();
                break;
            case "/whisper":
                if (tokens.length >= 3) {
                    String targetUsername = tokens[1];
                    String message = tokens[2];
                    whisper(targetUsername, message);
                } else {
                    sendMessage("Usage: /whisper <username> <message>");
                }
                break;
            case "/name":
                if (tokens.length >= 2) {
                    String newName = tokens[1];
                    changeName(newName);
                } else {
                    sendMessage("Usage: /name <new name>");
                }
                break;
            case "/help":
                sendHelp();
                break;
            case "/quit":
                removeClientHandler();
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            default:
                sendMessage("Unknown command: " + command);
                break;
        }
    }

    private void sendList() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Connected clients: ");
        synchronized (clientHandlers) {
            for (ClientHandler clientHandler : clientHandlers) {
                stringBuilder.append(clientHandler.clientUsername).append(" ");
            }
        }
        sendMessage(stringBuilder.toString());
    }

    private void whisper(String targetUsername, String message) {
        synchronized (clientHandlers) {
            for (ClientHandler clientHandler : clientHandlers) {
                if (clientHandler.clientUsername.equals(targetUsername)) {
                    clientHandler.sendMessage("(whisper) " + clientUsername + ": " + message);
                    sendMessage("(whisper) sent to " + targetUsername);
                    return;
                }
            }
        }
        sendMessage("User not found: " + targetUsername);
    }

    private void changeName(String newName) {
        if (!newName.equals(clientUsername)) {
            String previousName;
            synchronized (clientHandlers) {
                previousName = clientUsername;
                clientUsername = newName;
            }
            sendMessage("Name changed to " + clientUsername);
            broadcastMessage(previousName + " has changed their name to " + newName);
        } else {
            sendMessage("Name is the same as the current name");
        }
    }

    private void sendHelp() {
        String helpMessage = "Available commands: \n" +
                "/list - list all connected clients\n" +
                "/whisper <username> <message> - send a private message to a user\n" +
                "/name <new name> - change your displayed name\n" +
                "/help - list all available commands\n" +
                "/quit - leave the chat";
        sendMessage(helpMessage);
    }

    private void sendMessage(String message) {
        try {
            bufferedWriter.write(message);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    public void broadcastMessage(String messageToSend) {
        synchronized (clientHandlers) {
            for (ClientHandler clientHandler : clientHandlers) {
                try {
                    if (!clientHandler.clientUsername.equals(clientUsername)) {
                        clientHandler.bufferedWriter.write(messageToSend);
                        clientHandler.bufferedWriter.newLine();
                        clientHandler.bufferedWriter.flush();
                    }
                } catch (IOException e) {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
            }
        }
    }

    public void removeClientHandler() {
        synchronized (clientHandlers) {
            clientHandlers.remove(this);
        }
        broadcastMessage("SERVER: " + clientUsername + " has left the chat!");
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        removeClientHandler();
        try {
            if (bufferedReader != null){
                bufferedReader.close();
            }
            if (bufferedWriter != null){
                bufferedWriter.close();
            }
            if (socket != null){
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
