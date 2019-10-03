package com.broker;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Broker {

    private static int id;

    private static void connect() throws IOException {
        Socket socket = new Socket("localhost", 5000);

        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println(-1);

        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

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
        }
        else {
            id = response;
        }
    }


    public static void main(String args[]) throws IOException {
        connect();

        Socket socket;
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
            System.out.println("Market : " + in.readLine());
        }
    }
}
