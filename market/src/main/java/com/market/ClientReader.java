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
import java.util.concurrent.Callable;

public class ClientReader implements Callable {

    private Socket client;
    private Portfolio portfolio;
    private Order order;

    public ClientReader(Socket client, Portfolio portfolio) {
        this.client = client;
        this.portfolio = portfolio;

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

        //Blocking Socket call
        try {
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            input = in.readLine();
        } catch (SocketTimeoutException e){
            return null;
        } catch (IOException e){
            System.out.println("Read from Router Failed");
            return null;
        }

        if (input == null){
            return null;
        }

        this.order = new Order(input, this.client, this.portfolio);

        System.out.printf("New Message From Router : %S | Recipient : %S | Message %S\n", this.order.getSenderID(), this.order.getRecipientID(), order.toFix());
        return this.order;
    }
}
