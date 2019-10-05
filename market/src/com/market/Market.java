package com.market;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Market {

    private static int id;
    private static Socket socket;

    private static void connect() throws IOException {
        socket = new Socket("localhost", 5001);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println("c");

        String sender = "";
        String recipient = "";
        String message = "";

        String split[] = null;

        try {
            split = in.readLine().split("\\|");
        } catch (IOException e){
            System.out.println("Invalid Response");
        }

        if (split != null) {
            sender = split[0];
            recipient = split[1];
            message = split[2];
        }

        try {
            id = Integer.parseInt(message);
        } catch (NumberFormatException e){
            System.out.println("Invalid Response");
        } catch (NullPointerException e){
            System.out.println("Invalid Response");
        }

        if (id < 0){
            System.exit(-1);
        }
    }

    public static void main(String args[]) throws IOException {
        connect();

        Portfolio portfolio = new Portfolio(new Stock("FIAT", 1.0, 1000000),
                new Stock("ASTOCK", 12.30, 100),
                new Stock("BSTOCK", 21.20, 50),
                new Stock("CSTOCK", 128.60, 30));

        BufferedReader in;
        PrintWriter out;

        System.out.println("This Market has been assigned ID : " + id + " for this session");
        System.out.printf("This Market is now trading the following instruments...");
        System.out.println(portfolio.toString());

        while (true){
            String input = "";
            String response = "";
            int num = 0;

            //Block until Market recieves input
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            int senderID = -1;
            int recipientID = -1;
            int order = -1;

            try {
                input = in.readLine();
                System.out.println("Got input: " + input);
            } catch (IOException e){
                System.out.println("Invalid Input");
            }

            String split[] = input.split("\\|");

            try {
                senderID = Integer.parseInt(split[0]);
                recipientID = Integer.parseInt(split[1]);
                order = Integer.parseInt(split[2]);
            } catch (NumberFormatException e){
                System.out.println("Invalid Input");
            }

            System.out.println("Broker : Requesting " + order + " shares...");

            if (order <= apples && order > 0){
                response = "1";
                apples -= order;
                System.out.println("Transaction Approved!");
            }
            else {
                response = "0";
                System.out.println("Transaction Refused!");
            }

            socket = new Socket("localhost", 5001);

            System.out.println(apples + " shares are availabe...");
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println(id + "|" + senderID + "|" + response);
        }
    }
}