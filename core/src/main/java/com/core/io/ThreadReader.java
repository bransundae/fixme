package com.core.io;

import com.core.MarketSnapshot;
import com.core.Message;
import com.core.Order;
import com.core.Portfolio;

import javax.sound.sampled.Port;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.Callable;

public class ThreadReader implements Callable {

    private Socket client;

    public ThreadReader(Socket client) {
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
        } catch (ConnectException e){
            System.out.println("ERROR FATAL: Router Not Found");
            System.exit(-1);
        } catch (IOException e){
            System.out.println("Read from Client Failed");
            return null;
        }

        if (input == null){
            return null;
        }

        Object message = new Message(input);

        if (((Message)message).getType().equalsIgnoreCase("D")
                || ((Message)message).getType().equalsIgnoreCase("8")
                || ((Message)message).getType().equalsIgnoreCase("j")){
            message = new Order(input);
            if (((Order)message).validateChecksum()) {
                System.out.println("Checksum Validates : " + ((Order) message).toFix());
                return message;
            }
            else {
                System.out.println("Checksum does not Validate, Faulty Receive");
                return null;
            }

        } else if (((Message)message).getType().equalsIgnoreCase("W")
                || ((Message)message).getType().equalsIgnoreCase("Y")
                || ((Message)message).getType().equalsIgnoreCase("V")){

            message = new MarketSnapshot(input);
            if (((MarketSnapshot)message).validateChecksum()) {
                System.out.println("Checksum Validates : " + ((MarketSnapshot) message).toFix());
                return message;
            }
            else {
                System.out.println("Checksum does not Validate, Faulty Receive");
                return null;
            }

        } else {
            if (((Message)message).validateChecksum()) {
                System.out.println("Checksum Validates : " + ((Message) message).toFix());
                return message;
            }
            else {
                System.out.println("Checksum does not Validate, Faulty Receive");
                return null;
            }

        }
    }
}
