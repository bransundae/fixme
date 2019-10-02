package com.fixme;

import java.io.*;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Router {
    private static ServerSocket broker;
    private static ServerSocket market;
    private static void listenSocket() throws IOException {
        System.out.println("Server is Listening on Ports 5000 and 5001");
        broker = new ServerSocket(5000);
        market = new ServerSocket(5001);
    }

    public static void main(String args[]) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        Future <String> brokerResponse = null;
        Future <String> marketResponse = null;
        ClientReader brokerReader = null;
        ClientReader marketReader = null;
        listenSocket();

        while (true){
            if (brokerResponse == null || brokerResponse.isDone()) {
                if (brokerResponse != null) {
                    if (brokerResponse.get() != null) {
                        System.out.println("Broker : " + brokerResponse.get());

                        ClientWriter marketWriter = new ClientWriter(market, brokerResponse.get());
                        //Ignoring Promise returned by marketWriter Call
                        executorService.submit(marketWriter);

                        marketReader = new ClientReader(marketWriter.getClient());
                        marketResponse = executorService.submit(marketReader);
                    }
                }
                //brokerReader = new ClientReader(broker);
                //brokerResponse = executorService.submit(brokerReader);
            }

            /*if (marketResponse != null) {
                if (marketResponse.isDone()) {
                    ClientWriter brokerWriter = new ClientWriter(broker, marketResponse.get());
                    //Ignoring Promise returned by brokerWriter Call
                    executorService.submit(brokerReader);
                    marketResponse = null;
                }
            }*/
        }

    }

}
