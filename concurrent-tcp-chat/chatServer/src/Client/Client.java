package Client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;

    public static void main(String[] args) throws IOException{
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter your username for the group chat: ");
        String username = scanner.nextLine();
        Socket socket = new Socket("localhost", 9090);
        Client client = new Client(socket, username);
        client.listenForMessage();
        client.sendMessage();
    }

    public Client (Socket socket, String username){
        try{
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.username = username;
            bufferedWriter.write(username);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        }catch (IOException e){
            closeResources(socket, bufferedReader, bufferedWriter);
        }
    }

    public void sendMessage(){
        try{
            Scanner scanner = new Scanner(System.in);
            while (socket.isConnected()) {
                String messageToSend = scanner.nextLine();
                bufferedWriter.write(messageToSend);
                bufferedWriter.newLine();
                bufferedWriter.flush();
            }
        } catch (IOException e){
            closeResources(socket, bufferedReader, bufferedWriter);
        }
    }

    public void listenForMessage(){
        new Thread(new Runnable() {
            @Override
            public void run(){
                String msgFromChat;
                while(socket.isConnected()){
                    try{
                        msgFromChat = bufferedReader.readLine();
                        System.out.println(msgFromChat);
                    } catch (IOException e) {
                        closeResources(socket, bufferedReader, bufferedWriter);
                    }
                }
            }
        }).start();
    }

    public void closeResources(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter){
        try {
            if (socket != null){
                socket.close();
            }
            if (bufferedReader != null){
                bufferedReader.close();
            }
            if (bufferedWriter != null){
                bufferedWriter.close();
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
