package com.broker;

import com.fixme.lib.Message;
import com.fixme.lib.Order;
import com.fixme.lib.Portfolio;
import com.fixme.lib.Stock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class Broker {

    private static int id = -1;
    private static Socket socket;
    private static ExecutorService readerService = Executors.newFixedThreadPool(2);
    private static ExecutorService writerService = Executors.newFixedThreadPool(2);

    public static Portfolio portfolio = new Portfolio(
            new Stock("FIAT", 1.0, 10),
            new Stock("ASTOCK", 12.30, 10),
            new Stock("BSTOCK", 21.20, 5),
            new Stock("CSTOCK", 128.60, 3));

    private static Socket connect() throws IOException, ExecutionException, InterruptedException {

        HashMap<Future<Message>, ClientReader> futureMap = new HashMap<>();
        ArrayList<Future<Message>> deadFutureList = new ArrayList<>();

        //Create a new Socket and send registration request to router
        socket = new Socket("localhost", 5000);

        writerService.submit(new ClientWriter(socket, "35=A"));

        while (id == -1) {
            ClientReader clientReader = new ClientReader(socket, portfolio);
            futureMap.put(readerService.submit(clientReader), clientReader);

            Iterator<Map.Entry<Future<Message>, ClientReader>> it = futureMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Future<Message>, ClientReader> pair = it.next();
                if (pair.getKey().isDone()) {
                    if (pair.getKey() != null) {
                        if (pair.getKey().get() != null) {
                            if (pair.getKey().get().getSenderID() == 500 && pair.getKey().get().getMessage() != null) {
                                try {
                                    id = pair.getKey().get().getRecipientID();
                                } catch (NumberFormatException e) {
                                    System.out.println("Router Attempted to assign an Invalid ID");
                                    System.exit(-1);
                                }
                            }
                        }
                    }
                    deadFutureList.add(pair.getKey());
                }
            }

            for (Future<Message> f : deadFutureList)
                futureMap.remove(f);
            if (!futureMap.isEmpty())
                deadFutureList.clear();
        }
        return socket;
    }

    public static void main(String args[]) throws IOException, ExecutionException, InterruptedException {
        Socket socket = connect();
        HashMap<Future<Message>, ClientReader> futureMap = new HashMap<>();
        ArrayList<Future<Message>> deadFutureList = new ArrayList<>();
        ArrayList<Order> responseList = new ArrayList<>();

        Future<Order> inputFuture = null;
        String input = "";

        ExecutorService inputService = Executors.newFixedThreadPool(2);

        System.out.println("This Broker has been assigned ID : " + id + " for this session");
        System.out.println("This Broker is now trading the following instruments...");
        System.out.println(portfolio.toString());

        while (true){
            InputReader inputReader = new InputReader(id, portfolio);

            if (inputFuture == null || inputFuture.isCancelled()) {
                inputFuture = inputService.submit(inputReader);
            }
            else if (inputFuture.isDone()){
                input = inputFuture.get().toFix();
                inputFuture = null;
            }

            ClientReader clientReader = new ClientReader(socket, portfolio);
            futureMap.put(readerService.submit(clientReader), clientReader);

            Iterator<Map.Entry<Future<Message>, ClientReader>> it = futureMap.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry<Future<Message>, ClientReader> pair = it.next();
                if (pair.getKey().isDone()){
                    if (pair.getKey().get() != null){
                        //Message is not from server and therefore constitutes an order
                        if (pair.getKey().get().getSenderID() != 500){
                            //TODO Refactor Message to Parse FIX
                            responseList.add(new Order(pair.getKey().get().getMessage(), pair.getKey().get().getSocket(), portfolio));
                        }
                    }
                    deadFutureList.add(pair.getKey());
                }
            }

            for (Future<Message> f : deadFutureList)
                futureMap.remove(f);
            if (!deadFutureList.isEmpty())
                deadFutureList.clear();

            //Business Logic
            for (int i = 0; i < responseList.size(); i++){
                System.out.println("Response From Router : " + responseList.get(i).toFix());
                responseList.remove(i);
            }

            if (!input.isEmpty()){
                socket = new Socket("localhost", 5000);
                writerService.submit(new ClientWriter(socket, input));
                input = "";
            }
        }
    }
}
