package com.broker;

import com.broker.ClientReader;
import com.broker.ClientWriter;
import com.core.Message;
import com.core.Order;
import com.core.Portfolio;
import com.core.Stock;

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
    private static ExecutorService executorService = Executors.newCachedThreadPool();

    public static Portfolio portfolio = new Portfolio(
            new Stock("FIAT", 1.0, 10),
            new Stock("ASTOCK", 12.30, 10),
            new Stock("BSTOCK", 21.20, 5),
            new Stock("CSTOCK", 128.60, 3));

    private static Socket connect() throws IOException, ExecutionException, InterruptedException {

        HashMap<Future<Message>, ClientReader> futureMap = new HashMap<>();
        ArrayList<Future<Message>> deadFutureList = new ArrayList<>();

        //Create new Socket and send connection request to router on separate Thread
        socket = new Socket("localhost", 5000);

        ClientReader clientReader = new ClientReader(socket, portfolio);
        futureMap.put(executorService.submit(clientReader), clientReader);

        executorService.submit(new ClientWriter(socket, "35=A"));

        while (id == -1){
            Iterator<Map.Entry<Future<Message>, ClientReader>> it = futureMap.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry<Future<Message>, ClientReader> pair = it.next();
                if (pair.getKey().isDone()){
                    if (pair.getKey() != null){
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

            for (Future<Message> f : deadFutureList) {
                futureMap.put(executorService.submit(clientReader), clientReader);
                futureMap.remove(f);
            }
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

        ClientReader clientReader = new ClientReader(socket, portfolio);
        futureMap.put(executorService.submit(clientReader), clientReader);

        String input = "";

        System.out.println("This com.broker.Broker has been assigned ID : " + id + " for this session");
        System.out.println("This com.broker.Broker is now trading the following instruments...");
        System.out.println(portfolio.toString());

        String soh = "" + (char)1;

        input = "35=D" + soh + "115=" + id + soh + "56=100001" + soh + "55=ASTOCK" + soh + "38=30" +
                soh + "44=2.0" + soh + "54=2";

        while (true){
            /*InputReader inputReader = new InputReader(id, portfolio);

            if (inputFuture == null || inputFuture.isCancelled()) {
                inputFuture = inputService.submit(inputReader);
            }
            else if (inputFuture.isDone()){
                input = inputFuture.get().toFix();
                inputFuture = null;
            }*/

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

            for (Future<Message> f : deadFutureList) {
                clientReader = new ClientReader(socket, portfolio);
                futureMap.put(executorService.submit(clientReader), clientReader);
                futureMap.remove(f);
            }
            if (!deadFutureList.isEmpty())
                deadFutureList.clear();

            //Business Logic
            for (int i = 0; i < responseList.size(); i++){
                System.out.println("Response From Router : " + responseList.get(i).toFix());
                responseList.remove(i);
            }

            if (!input.isEmpty()){
                socket = new Socket("localhost", 5000);
                executorService.submit(new ClientWriter(socket, input));
                input = "";
            }
        }
    }
}
