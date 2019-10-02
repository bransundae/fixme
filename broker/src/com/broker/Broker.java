package com.broker;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Broker {

    public static void main(String args[]) throws IOException {

        Socket socket;
        BufferedReader in;
        PrintWriter out;
        String input = "";

        while (true) {
            socket = new Socket("localhost", 5000);
            Scanner user = new Scanner(System.in);

            if (user.hasNext()) {
                out = new PrintWriter(socket.getOutputStream(), true);
                input = user.next();
                out.println(input);
            }

            System.out.println("Requesting " + input + " shares from Market...");

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Market : " + in.readLine());
        }
    }
}
