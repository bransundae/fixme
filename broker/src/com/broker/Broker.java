package com.broker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Broker {

    public static void main(String args[]) throws IOException {
        Socket socket = new Socket("localhost", 5000);

        PrintWriter printWriter = new PrintWriter(socket.getOutputStream());
        printWriter.println("I request 10 apples");
        printWriter.flush();

        InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        String string = bufferedReader.readLine();
        System.out.println("Server: " + string);
    }

}
