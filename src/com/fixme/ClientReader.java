package com.fixme;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;

public class ClientReader implements Callable {

    private ServerSocket serverSocket;

    private Socket client;

    private Message message;

    public ClientReader(ServerSocket serverSocket){
        this.serverSocket = serverSocket;
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
        System.out.println("New Client Reader Started...");
        try {
            this.client = serverSocket.accept();
        } catch (Exception e){
            return null;
        }

            String message = "";
            BufferedReader in = null;
            PrintWriter out = null;

            try {
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            } catch (IOException e) {
                System.out.println("Read from Client Failed");
                return null;
            }

            try {
                message = in.readLine();
            } catch (IOException e) {
                System.out.println("Input Read Failed");
            }

            if (!message.equalsIgnoreCase("c")) {
                String split[] = message.split("\\|");
                this.message = new Message(Integer.parseInt(split[0]), Integer.parseInt(split[1]), split[2], client);
            }
            //If client ID does not exist then assign client an ID and store socket in HashMap
            else {
                this.message = new Message(500, Router.clientID, "Registered to Router with ID : " + Router.clientID, client);
                Router.clientID++;
                System.out.println(this.message.toString());
            }

            return this.message;
    }
}
