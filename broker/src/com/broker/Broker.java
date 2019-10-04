package com.broker;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Broker {

    private static int id = -1;
    private static Socket socket;

    private static void connect() throws IOException {
        socket = new Socket("localhost", 5000);

        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println("c");

        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

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
        System.out.println("This Broker has been assigned ID : " + id + " for this session");
        BufferedReader in;
        PrintWriter out;
        String input = "";

        while (true) {
            socket = new Socket("localhost", 5000);
            Scanner user = new Scanner(System.in);
            System.out.println("Enter Market ID...");
            input += user.next();
            System.out.println("Enter your Order...");
            input += "|"+user.next();
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println(id + "|" + input);

            System.out.println("Requesting " + input + " shares from Market...");

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            int status = Integer.parseInt(in.readLine().split("\\|")[2]);

            if (status == 1)
                System.out.println("Market : Executed");
            else
                System.out.println("Market : Rejected");
        }
    }
}
