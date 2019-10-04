package com.fixme;

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
        return client;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    @Override
    public Object call() throws Exception {
        String message = "";
        BufferedReader in = null;
        PrintWriter out = null;

        //Blocking Socket call
        try {
            this.client = serverSocket.accept();
            System.out.println("New Connection From Client");
            //this.client.setSoTimeout(2000);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            message = in.readLine();
        } catch (SocketTimeoutException e){
            //System.out.println("Connection Timed out");
            return null;
        } catch (IOException e){
            System.out.println("Read from Client Failed");
            return null;
        }

        if (message == null){
            return null;
        }

        out = new PrintWriter(this.client.getOutputStream(), true);

        if (!message.equalsIgnoreCase("c")) {
            String split[] = message.split("\\|");
            this.message = new Message(Integer.parseInt(split[0]), Integer.parseInt(split[1]), split[2], client);
        }
        //If client ID does not exist then assign client an ID and store socket in HashMap
        else {
            this.message = new Message(500, Router.clientID, "" + Router.clientID, client);
            Router.clientID++;
        }
        System.out.printf("New Message From Client : %S | Recipient : %S | Message %S\n", this.message.getSender(), this.message.getRecipient(), this.message.getMessage());
        return this.message;
    }
}
