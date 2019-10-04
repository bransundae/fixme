package com.fixme;

import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Router {
    private static ServerSocket broker;
    private static ServerSocket market;

    private static ClientReader marketReader;
    private static ClientReader brokerReader;

    public static int clientID = 100000;


    private static void listenSocket() throws IOException {
        System.out.println("Server is Listening on Ports 5000 and 5001");

        broker = new ServerSocket(5000);
        market = new ServerSocket(5001);

        brokerReader = new ClientReader(broker);
        marketReader = new ClientReader(market);
    }

    public static void main(String args[]) throws Exception {
        Map<Integer, Socket> clientMap = new HashMap<>();
        ArrayList<Future<Message>> futureList = new ArrayList<>();
        ArrayList<Future<Message>> deadFutureList = new ArrayList<>();
        ArrayList<Message> jobList = new ArrayList<>();
        Map<Future<Message>, ClientReader> futureMap = new HashMap<>();


        ExecutorService readerService = Executors.newFixedThreadPool(2);
        ExecutorService writerService = Executors.newFixedThreadPool(2);
        /*Future <String> brokerResponse = null;
        Future <String> marketResponse = null;
        String brokerMessage = null;
        String marketMessage = null;
        String routerMessage = null;*/
        listenSocket();

        while (true){
            ClientReader brokerReader = new ClientReader(broker);
            ClientReader marketReader = new ClientReader(market);

            futureMap.put(readerService.submit(brokerReader), brokerReader);
            futureMap.put(readerService.submit(marketReader), marketReader);

            //Iterate through the futureMap and if a Callable has returned a completed Future then add the message to the job queue and remove the future - ClientReader pair
            //If the sender of the message is not registered then store the socket from the ClientReader first
            Iterator<Map.Entry<Future<Message>, ClientReader>> it = futureMap.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry<Future<Message>, ClientReader> pair = (Map.Entry<Future<Message>, ClientReader>)it.next();
                if (pair.getKey().isDone()){
                    if (pair.getKey().get() != null) {
                        if (pair.getKey().get().getSender() == 500) {
                            if (clientMap.get(pair.getKey().get().getRecipient()) == null) {
                                clientMap.put(pair.getKey().get().getRecipient(), pair.getKey().get().getSocket());
                            }
                        }
                        jobList.add(pair.getKey().get());
                    }
                    deadFutureList.add(pair.getKey());
                }
            }

            //Clean the completed futures from the futureMap
            for (Future<Message> f : deadFutureList){
                futureMap.remove(f);
            }
            if (!deadFutureList.isEmpty())
                deadFutureList.clear();

            for (int i = 0; i < jobList.size(); i++){
                if (clientMap.get(jobList.get(i).getRecipient()) != null)
                    writerService.submit(new ClientWriter(clientMap.get(jobList.get(i).getRecipient()), jobList.get(i).toString()));
                jobList.remove(jobList.get(i));
            }
        }



       /*while (true){
            if (brokerResponse == null){
                brokerResponse = executorService.submit(brokerReader);
            }
            else if (brokerResponse.isDone() && brokerResponse.get() != null){
                try {
                    int res = Integer.parseInt(brokerResponse.get());
                    if (res == -1){
                        if (clientMap.get(++brokerID) == null) {
                            clientMap.put(brokerID, brokerReader.getClient());
                            routerMessage = "" + brokerID;
                        }
                    }
                } catch (NumberFormatException e) {
                    brokerMessage = brokerResponse.get();
                    int senderID = Integer.parseInt(brokerMessage.split("\\|")[0]);
                    clientMap.replace(senderID, brokerReader.getClient());
                }
                brokerResponse = null;
            }

            if (marketResponse == null){
                marketResponse = executorService.submit(marketReader);
            }
            else if (marketResponse.isDone() && marketResponse.get() != null){
                try {
                    int res = Integer.parseInt(marketResponse.get());
                    if (res == -1) {
                        if (clientMap.get(++marketID) == null) {
                            clientMap.put(marketID, marketReader.getClient());
                            routerMessage = "" + marketID;
                        }
                    }
                } catch (NumberFormatException e) {
                    marketMessage = marketResponse.get();
                    int senderID = Integer.parseInt(marketMessage.split("\\|")[0]);
                    clientMap.replace(senderID, marketReader.getClient());
                }
                marketResponse = null;
            }

            if (routerMessage != null){
                executorService.submit(new ClientWriter(clientMap.get(Integer.parseInt(routerMessage)), routerMessage));
                routerMessage = null;
            }

            if (brokerMessage != null){
                int senderID;
                int recipientID;
                int order;

                String split[] = brokerMessage.split("\\|");

                senderID = Integer.parseInt(split[0]);
                recipientID = Integer.parseInt(split[1]);
                order = Integer.parseInt(split[2]);

                System.out.printf("Broker : SenderID: %S | RecipientID: %S | Order: %S\n", senderID, recipientID, order);

                executorService.submit(new ClientWriter(clientMap.get(recipientID), brokerMessage));
                brokerMessage = null;
            }

            if (marketMessage != null){
                int senderID;
                int recipientID;
                int status;

                String split[] = marketMessage.split("\\|");

                senderID = Integer.parseInt(split[0]);
                recipientID = Integer.parseInt(split[1]);
                status = Integer.parseInt(split[2]);

                System.out.printf("Market : SenderID: %S | RecipientID: %S | Status %S\n", senderID, recipientID, status);

                executorService.submit(new ClientWriter(clientMap.get(recipientID), marketMessage));
                marketMessage = null;
            }
        }*/
    }
}
