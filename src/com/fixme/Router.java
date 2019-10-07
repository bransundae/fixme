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
        Map<Future<Message>, ClientReader> futureMap = new HashMap<>();

        ArrayList<Future<Message>> deadFutureList = new ArrayList<>();
        ArrayList<Message> jobList = new ArrayList<>();

        ExecutorService readerService = Executors.newFixedThreadPool(2);
        ExecutorService writerService = Executors.newFixedThreadPool(2);

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
                        //If the sender is previously registered to the router then simply overwrite it's stored socket with the socket it used to send the current message
                        else if (clientMap.get(pair.getKey().get().getSender()) != null){
                            clientMap.replace(pair.getKey().get().getSender(), pair.getKey().get().getSocket());
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
                else
                    writerService.submit(new ClientWriter(clientMap.get(jobList.get(i).getSender()), "500|" + jobList.get(i).getSender() + "|-1"));
                jobList.remove(jobList.get(i));
            }
        }
    }
}
