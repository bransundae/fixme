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

        int apples = 200;

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

            if (num <= apples || num > 0){
                response = "Order Accepted";
                apples -= num;
            }
            else {
                response = "Order Denied";
            }

            out = new PrintWriter(socket.getOutputStream(), true);
            out.println(response);

        }
    }

}
