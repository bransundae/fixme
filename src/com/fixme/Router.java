package com.fixme;

import java.io.*;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Router {
    private static ServerSocket broker;
    private static ServerSocket market;

    private static ClientReader marketReader;
    private static ClientReader brokerReader;


    private static void listenSocket() throws IOException {
        System.out.println("Server is Listening on Ports 5000 and 5001");

        broker = new ServerSocket(5000);
        market = new ServerSocket(5001);

        brokerReader = new ClientReader(broker);
        marketReader = new ClientReader(market);
    }

    public static void main(String args[]) throws Exception {

        Map<Integer, ClientReader> idPortMap = new HashMap<>();
        Message message;
        int id = 100000;


        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Future <String> brokerResponse = null;
        Future <String> marketResponse = null;
        String brokerMessage = null;
        String marketMessage = null;
        String routerMessage = null;
        listenSocket();

        int responseCount = 0;

        while (true){
            if (brokerResponse == null){
                brokerResponse = executorService.submit(brokerReader);
            }
            else if (brokerResponse.isDone() && brokerResponse.get() != null){
                try {
                    int res = Integer.parseInt(brokerMessage);
                    if (res == -1){
                        idPortMap.put(id++, brokerReader);
                        routerMessage = "" + id;
                    }
                } catch (NumberFormatException e) {
                    brokerMessage = brokerResponse.get();
                    responseCount++;
                }
                brokerResponse = null;
            }

            if (marketResponse == null){
                marketResponse = executorService.submit(marketReader);
            }
            else if (marketResponse.isDone() && marketResponse.get() != null){
                try {
                    int res = Integer.parseInt(marketMessage);
                    if (res == -1) {
                        idPortMap.put(id++, marketReader);
                        routerMessage = "" + id;
                    }
                } catch (NumberFormatException e) {
                    marketMessage = marketResponse.get();
                    responseCount++;
                }
                marketResponse = null;
            }

            if (routerMessage != null){
                executorService.submit(new ClientWriter(idPortMap.get(Integer.parseInt(routerMessage)).getClient(), routerMessage));
                routerMessage = null;
            }

            if (marketMessage != null && responseCount <= 2){
                if (marketMessage.equalsIgnoreCase("Order Denied"))
                    System.out.println("Transaction : Failed!");
                else
                    System.out.println("Transaction : Success!");
                responseCount = 0;
            }

            if (brokerMessage != null){
                executorService.submit(new ClientWriter(marketReader.getClient(), brokerMessage));
                brokerMessage = null;
            }

            if (marketMessage != null){
                executorService.submit(new ClientWriter(brokerReader.getClient(), marketMessage));
                marketMessage = null;
            }
        }
    }
}
