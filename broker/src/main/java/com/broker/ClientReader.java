package com.broker;

import com.core.Message;
import com.core.Order;
import com.core.Portfolio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.concurrent.Callable;

public class ClientReader implements Callable {

    private Socket client;

    private Portfolio portfolio;

    private Order message;

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

        this.message = new Order(input, portfolio);

        if (this.message.validateChecksum(message.getMessage())) {
            System.out.println("Checksum Validates");
            messages.add(this.message);
        }
        else {
            System.out.println("Checksum does not Validate, Faulty Receive");
        }
        while (!this.message.isDone()){
            //Blocking Socket call
            try {
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                input = in.readLine();
                this.message = new Order(input, portfolio);
                if (this.message.validateChecksum(message.toFix())) {
                    System.out.println("Checksum Validates");
                    messages.add(this.message);
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

        //System.out.printf("New Message From Client : %S | Recipient : %S | Message %S\n", this.message.getSenderID(), this.message.getRecipientID(), this.message.getMessage());
        return messages;
    }
}
