package com.fixme;

import com.fixme.lib.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.Callable;

public class ClientReader implements Callable {

    private ServerSocket serverSocket;

    private Socket client;

    private Message message;

    public ClientReader(ServerSocket serverSocket){
        this.serverSocket = serverSocket;

        try {
            this.serverSocket.setSoTimeout(2000);
        } catch (SocketException e){
            System.out.println("Cannot set Timeout on this Socket");
        }
    }

    public ClientReader(Socket client) {
        this.client = client;
    }

    public Socket getClient() {
        return this.client;
    }

    public int getPort(){
        return this.serverSocket.getLocalPort();
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    @Override
    public Object call() throws Exception {
        String input = "";
        BufferedReader in = null;
        PrintWriter out = null;

        //Blocking Socket call
        try {
            this.client = serverSocket.accept();
            System.out.println("New Connection From Client");
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            input = in.readLine();
            System.out.println(input);
        } catch (SocketTimeoutException e){
            return null;
        } catch (IOException e){
            System.out.println("Read from Router Failed");
            return null;
        }

        if (input == null){
            return null;
        }

        out = new PrintWriter(this.client.getOutputStream(), true);

        this.message = new Message(input, this.client);

        //If client ID does not exist then assign client an ID and store socket in HashMap
        if (this.message.getType().equalsIgnoreCase("A")) {
            Router.clientID++;
            message.setSenderID(Router.clientID);
        }

        System.out.printf("New Message From Client : %S | Recipient : %S | Message %S\n", message.getSenderID(), message.getRecipientID(), this.message.getMessage());
        return this.message;
    }
}
