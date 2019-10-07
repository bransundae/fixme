package com.market;

import com.market.ClientReader;
import com.market.ClientWriter;
import com.market.Message;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Market {

    private static int id = -1;
    private static Socket socket;

    private static ExecutorService readerService = Executors.newFixedThreadPool(2);
    private static ExecutorService writerService = Executors.newFixedThreadPool(2);

    private static Socket connect() throws IOException, ExecutionException, InterruptedException {

        HashMap<Future<Message>, ClientReader> futureMap = new HashMap<>();
        ArrayList<Future<Message>> deadFutureList = new ArrayList<>();

        //Create new Socket and send connection request to router on separate Thread
        socket = new Socket("localhost", 5001);

        writerService.submit(new ClientWriter(socket, "c"));

        while (id == -1){
            ClientReader clientReader = new ClientReader(socket);
            futureMap.put(readerService.submit(clientReader), clientReader);

            Iterator<Map.Entry<Future<Message>, ClientReader>> it = futureMap.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry<Future<Message>, ClientReader> pair = it.next();
                if (pair.getKey().isDone()){
                    if (pair.getKey() != null){
                        if (pair.getKey().get().getSender() == 500 && pair.getKey().get().getMessage() != null){
                            try {
                                id = Integer.parseInt(pair.getKey().get().getMessage());
                            } catch (NumberFormatException e) {
                                System.out.println("Router Attempted to assign an Invalid ID");
                                System.exit(-1);
                            }
                        }
                    }
                    deadFutureList.add(pair.getKey());
                }
            }

            for (Future<Message> f : deadFutureList)
                futureMap.remove(f);
            if (!futureMap.isEmpty())
                deadFutureList.clone();
        }

        return socket;
    }

    public static void main(String args[]) throws IOException, ExecutionException, InterruptedException {

        Socket socket = connect();
        HashMap<Future<Message>, ClientReader> futureMap = new HashMap<>();
        ArrayList<Future<Message>> deadFutureList = new ArrayList<>();

        System.out.println("This Market has been assigned ID : " + id + " for this session");

        Portfolio portfolio = new Portfolio(
                new Stock("FIAT", 1.0, 1000000),
                new Stock("ASTOCK", 12.30, 100),
                new Stock("BSTOCK", 21.20, 50),
                new Stock("CSTOCK", 128.60, 30));

        ArrayList<Order> orderList = new ArrayList<>();

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        System.out.println("This Market has been assigned ID : " + id + " for this session");
        System.out.printf("This Market is now trading the following instruments...");
        System.out.println(portfolio.toString());

        while (true){
            ClientReader clientReader = new ClientReader(socket);
            futureMap.put(executorService.submit(clientReader), clientReader);

            Iterator<Map.Entry<Future<Message>, ClientReader>> it = futureMap.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry<Future<Message>, ClientReader> pair = it.next();
                if (pair.getKey().isDone()){
                    if (pair.getKey().get() != null){
                        if (pair.getKey().get().getSender() != 500 && pair.getKey().get().getMessage() != null){
                            orderList.add();
                        }
                    }

                    deadFutureList.add(pair.getKey());
                }
            }





        }































        /*while (true){
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
                response = "0";
                System.out.println("Transaction Refused!");
            }

            socket = new Socket("localhost", 5001);

            System.out.println(apples + " shares are availabe...");
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println(id + "|" + senderID + "|" + response);
        }*/
    }
}