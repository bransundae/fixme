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

    public ClientReader(ServerSocket serverSocket){
        this.serverSocket = serverSocket;
    }

    public ClientReader(Socket client) {
        this.client = client;
    }

    public Socket getClient() {
        return client;
    }

    @Override
    public Object call() throws Exception {
        System.out.println("Client Reader : Waiting for Client...");

        if (client == null)
            this.client = serverSocket.accept();

        System.out.println("Client Reader : Client Connected!");

        String message = "";
        BufferedReader in = null;
        PrintWriter out = null;

        try {
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        } catch (IOException e){
            System.out.println("Read from Client Failed");
            return null;
        }

        try {
            message = in.readLine();
        } catch (IOException e) {
            System.out.println("Input Read Failed");
        }

        return message;
    }
}
