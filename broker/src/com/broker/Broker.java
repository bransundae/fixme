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

        while (true) {
            socket = new Socket("localhost", 5000);
            Scanner user = new Scanner(System.in);

            if (user.hasNext()) {
                out = new PrintWriter(socket.getOutputStream(), true);
                out.println(user.next());
            }

            System.out.println("Awaiting Result...");

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println(in.readLine());

            System.out.println("Result Complete");
        }
    }
}
