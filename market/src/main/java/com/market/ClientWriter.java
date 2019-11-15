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

    public ClientWriter(Socket client, Order order){
        this.client = client;
        this.order = order;
    }

    public Socket getClient() {
        return client;
    }

    @Override
    public Object call() throws Exception {
        PrintWriter out = null;
        if (this.message != null) {
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

        else if (this.order != null){
            this.order.setChecksum(this.order.generateChecksum(this.order.toFix()));
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                out.println(order.toFix());
                System.out.println("Response Sent To Client : " + order.toFix());
            } catch (IOException e) {
                System.out.println("Write to Client Failed");
                return null;
            }
            return order;
        }

        return null;
    }
}
