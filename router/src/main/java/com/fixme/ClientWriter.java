package com.fixme;

import com.fixme.lib.Message;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.Callable;

public class ClientWriter implements Callable {

    private Socket client;
    private Message message;
    private PrintWriter out;

    public ClientWriter(Socket client, Message message){
        this.client = client;
        this.message = message;
    }

    public Socket getClient() {
        return client;
    }

    @Override
    public Object call() throws Exception {
        PrintWriter out = null;
        try {
            out = new PrintWriter(client.getOutputStream(), true);
            out.println(message.getMessage());
            System.out.println("Response Sent To Client : " + message.getMessage());
        } catch (IOException e){
            System.out.println("Write to Client Failed");
            return null;
        }
        return message;
    }
}
