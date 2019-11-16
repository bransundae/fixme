package com.router;

import com.core.MarketSnapshot;
import com.core.Message;
import com.core.Order;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.Callable;

public class ServerReader implements Callable {

    private ServerSocket serverSocket;

    private Socket client;

    public ServerReader(ServerSocket serverSocket){
        this.serverSocket = serverSocket;

        try {
            this.serverSocket.setSoTimeout(2000);
        } catch (SocketException e){
            System.out.println("Cannot set Timeout on this Socket");
        }
    }

    public ServerReader(Socket client) {
        this.client = client;
    }

    public Socket getClient() {
        return this.client;
    }

    public int getPort(){
        return this.serverSocket.getLocalPort();
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    @Override
    public Object call() throws Exception {
        String input = "";
        BufferedReader in = null;
        PrintWriter out = null;

        //Blocking Socket call
        try {
            this.client = serverSocket.accept();
            System.out.println("New Connection From Client");
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            input = in.readLine();
        } catch (SocketTimeoutException e){
            return null;
        } catch (IOException e){
            System.out.println("Read from Router Failed");
            return null;
        }

        if (input == null){
            return null;
        }

        Object message = new Message(input);

        if (((Message)message).getType().equalsIgnoreCase("D")
                || ((Message)message).getType().equalsIgnoreCase("j")
                || ((Message)message).getType().equalsIgnoreCase("j")){
            message = new Order(input);
            if (((Order)message).validateChecksum()) {
                System.out.println("Checksum Validates : " + ((Order) message).toFix());
                return message;
            }
            else {
                System.out.println("Checksum does not Validate, Faulty Receive");
                return null;
            }

        } else if (((Message)message).getType().equalsIgnoreCase("W")
                || ((Message)message).getType().equalsIgnoreCase("Y")
                || ((Message)message).getType().equalsIgnoreCase("V")){

                message = new MarketSnapshot(input);
            if (((MarketSnapshot)message).validateChecksum()) {
                System.out.println("Checksum Validates : " + ((MarketSnapshot) message).toFix());
                return message;
            }
            else {
                System.out.println("Checksum does not Validate, Faulty Receive");
                return null;
            }

        } else {

            if (((Message)message).validateChecksum()) {
                System.out.println("Checksum Validates : " + ((Message) message).toFix());
                //If client ID does not exist then assign client an ID and store socket in HashMap
                if (((Message)message).getType().equalsIgnoreCase("A")) {
                    Router.clientID++;
                    ((Message)message).setSenderID(Router.clientID);
                }
                return message;
            }
            else {
                System.out.println("Checksum does not Validate, Faulty Receive");
                return null;
            }

        }
    }
}
