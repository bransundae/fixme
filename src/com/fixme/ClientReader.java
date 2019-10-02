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

    private boolean init = true;

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

        if (init)
            System.out.println("Client Reader : Waiting for Client...");

        this.client = serverSocket.accept();

        if (init)
            System.out.println("Client Reader : Client Connected!");

        init = false;

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
            return message;

    }
}
