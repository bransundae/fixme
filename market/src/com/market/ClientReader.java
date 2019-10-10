package com.market;

import com.fixme.lib.Message;

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

    private Message message;

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
        String message = "";
        BufferedReader in = null;
        PrintWriter out = null;

        //Blocking Socket call
        try {
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            message = in.readLine();
        } catch (SocketTimeoutException e){
            return null;
        } catch (IOException e){
            System.out.println("Read from Client Failed");
            return null;
        }

        if (message == null){
            return null;
        }

        out = new PrintWriter(this.client.getOutputStream(), true);

        if (!message.equalsIgnoreCase("c")) {
            this.message = new Message(message, client);
            System.out.println(this.message.toString());
        }
        System.out.printf("New Message From Client : %S | Recipient : %S | Message %S\n", this.message.getSenderID(), this.message.getRecipientID(), this.message.getMessage());
        return this.message;
    }
}
