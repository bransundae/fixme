package com.market;

import com.core.Message;
import com.core.Order;
import com.core.Portfolio;

import javax.sound.sampled.Port;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.concurrent.Callable;

public class ClientReader implements Callable {

    private Socket client;
    private Order order;

    public ClientReader(Socket client) {
        this.client = client;

        try {
            this.client.setSoTimeout(2000);
        } catch (SocketException e){
            System.out.println("Cannot set Timeout on this Socket");
        }
    }

    public Socket getClient() {
        return client;
    }

    @Override
    public Object call() throws Exception {
        String input = "";
        BufferedReader in = null;
        ArrayList<Message> messages = new ArrayList<>();

        //Blocking Socket call
        try {
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            input = in.readLine();
        } catch (SocketTimeoutException e){
            return null;
        } catch (IOException e){
            System.out.println("Read from Client Failed");
            return null;
        }

        if (input == null){
            return null;
        }

        this.order = new Order(input);

        if (this.order.validateChecksum(order.getMessage())) {
            System.out.println("Checksum Validates");
            messages.add(this.order);
        }
        else {
            System.out.println("Checksum does not Validate, Faulty Receive");
        }
        while (!this.order.isDone()){
            //Blocking Socket call
            try {
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                input = in.readLine();
                this.order = new Order(input);
                if (this.order.validateChecksum(order.toFix())) {
                    System.out.println("Checksum Validates");
                    messages.add(this.order);
                }
                else {
                    System.out.println("Checksum does not Validate, Faulty Receive");
                }
            } catch (SocketTimeoutException e){
                System.out.println("Read from Client Failed");
                return messages;
            } catch (IOException e){
                System.out.println("Read from Client Failed");
                return messages;
            }
        }

        for (Message message : messages){
            System.out.println("Message: " + message.getMessage());
        }
        return messages;
    }
}
