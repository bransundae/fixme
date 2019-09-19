package com.fixme;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Router {

    public static void main(String args[]) throws IOException {
        ServerSocket serverSocket = new ServerSocket(5000);
        Socket socket = serverSocket.accept();

        System.out.println("Broker Connected");

        InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        String string = bufferedReader.readLine();
        System.out.println("Broker: " + string);

        PrintWriter printWriter = new PrintWriter(socket.getOutputStream());
        printWriter.println("10 apples have been sent");
        printWriter.flush();
    }

}
