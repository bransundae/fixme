package com.market;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Market {

    public static void main(String args[]) throws IOException {

        Socket socket;
        BufferedReader in;
        PrintWriter out;

        int apples = 1000;
        System.out.println("Market is open!");
        System.out.println(apples + " shares are availabe on this market...");

        while (true){
            String input = "";
            String response = "";
            int num = 0;
            socket = new Socket("localhost", 5001);

            //Block until Market recieves input
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            try {
                input = in.readLine();
            } catch (IOException e){
                System.out.println("Invalid Input");
            }

            try {
                num = Integer.parseInt(input);
            } catch (NumberFormatException e){
                System.out.println("Invalid Input");
            }

            System.out.println("Broker : Requesting " + num + " shares...");

            if (num <= apples && num > 0){
                response = "Order Accepted";
                apples -= num;
                System.out.println("Transaction Approved!");
            }
            else {
                response = "Order Denied";
                System.out.println("Transaction Refused!");
            }
            System.out.println(apples + " shares are availabe...");
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println(response);
        }
    }

}
