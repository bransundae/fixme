package com.broker;

import com.core.Message;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.Callable;

public class ClientWriter implements Callable {

    private Socket client;
    private Message message;

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
        this.message.generateChecksum();
        try {
            out = new PrintWriter(client.getOutputStream(), true);
            out.println(message.toFix());
            System.out.println("MESSAGE :" + message.toFix());
        } catch (IOException e){
            System.out.println("Write to Client Failed");
            return null;
        }
        return message;
    }
}
