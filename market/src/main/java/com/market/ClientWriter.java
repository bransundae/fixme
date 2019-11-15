package com.market;

import com.core.Message;
import com.core.Order;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;

public class ClientWriter implements Callable {

    private Socket client;
    private Message message;
    private Order order;

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

        this.message.setChecksum(this.message.generateChecksum(this.message.toFix()));
        try {
            out = new PrintWriter(client.getOutputStream(), true);
            out.println(message.toFix());
            //System.out.println("Response Sent To Client : " + message.toFix());
        } catch (IOException e) {
                System.out.println("Write to Client Failed");
                return null;
        }
        return message;

    }
}
