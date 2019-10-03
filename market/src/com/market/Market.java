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

        int response = -1;

        try {
            response = Integer.parseInt(in.readLine());
        } catch (IOException e){
            System.out.println("Invalid Response");
        } catch (NumberFormatException e){
            System.out.println("Invalid Response");
        } catch (NullPointerException e){
            System.out.println("Invalid Response");
        }

        if (response < 0){
            System.exit(-1);
        } else {
            id = response;
        }
    }

    public static void main(String args[]) throws IOException {

        connect();

        BufferedReader in;
        PrintWriter out;

        int apples = 1000;
        System.out.println("Router has allocated this server with ID : " + id);
        System.out.println("This Market is now open!");
        System.out.println(apples + " shares are availabe on this market...");

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
                response = "-1";
                System.out.println("Transaction Refused!");
            }

            socket = new Socket("localhost", 5001);

            System.out.println(apples + " shares are availabe...");
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println(id + "|" + senderID + "|" + response);
        }
    }
}