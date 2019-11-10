package com.router;

import com.core.MarketSnapshot;
import com.core.Message;
import com.core.Portfolio;

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

    private static Map<Integer, Socket> clientMap = new HashMap<>();
    private static Map<Integer, MarketSnapshot> marketSnapshotMap = new HashMap<>();
    private static Map<Future<Message>, ClientReader> futureMap = new HashMap<>();

    private static ArrayList<Future<Message>> deadFutureList = new ArrayList<>();
    private static ArrayList<Message> jobList = new ArrayList<>();

    private static ExecutorService executorService = Executors.newCachedThreadPool();

    public static int clientID = 100000;


    private static void listenSocket() throws IOException {
        System.out.println("Server is Listening on Ports 5000 and 5001");

        broker = new ServerSocket(5000);
        market = new ServerSocket(5001);

        brokerReader = new ClientReader(broker);
        marketReader = new ClientReader(market);
    }

    private static void heartBeat() {

        Iterator<Map.Entry<Integer, Socket>> it = clientMap.entrySet().iterator();

        while (it.hasNext()){
            Map.Entry<Integer, Socket> pair = it.next();
            Message message = new Message(500, pair.getKey(), "0", pair.getValue());
            message.setMessage(message.toFix());
            executorService.submit(new ClientWriter(pair.getValue(), message));
        }
    }


    public static void main(String args[]) throws Exception {

        listenSocket();

        futureMap.put(executorService.submit(brokerReader), brokerReader);
        futureMap.put(executorService.submit(marketReader), marketReader);

        while (true){
            //Iterate through the futureMap and if a Callable has returned a completed Future then add the message to the job queue and remove the future - ClientReader pair
            //If the sender of the message is not registered then store the socket from the ClientReader first
            Iterator<Map.Entry<Future<Message>, ClientReader>> it = futureMap.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry<Future<Message>, ClientReader> pair = it.next();
                if (pair.getKey().isDone()){
                    if (pair.getKey().get() != null) {
                        ArrayList<Message> toSend = new ArrayList<>();
                        //If the sender is previously registered to the router then simply overwrite it's stored socket with the socket it used to send the current message
                        if (clientMap.get(pair.getKey().get().getSenderID()) != null){
                            clientMap.replace(pair.getKey().get().getSenderID(), pair.getKey().get().getSocket());

                            //Market Data Snapshot
                            if (pair.getKey().get().getType().equalsIgnoreCase("W")){
                                if (marketSnapshotMap.get(pair.getKey().get().getSenderID()) == null){
                                    marketSnapshotMap.put(pair.getKey().get().getSenderID(), new MarketSnapshot(pair.getKey().get().getMessage()));
                                }
                                else if (marketSnapshotMap.get(pair.getKey().get().getSenderID()) != null){
                                    marketSnapshotMap.replace(pair.getKey().get().getSenderID(), new MarketSnapshot(pair.getKey().get().getMessage()));
                                }
                            }
                            //Market Data Request
                            else if (pair.getKey().get().getType().equalsIgnoreCase("V")){
                                Iterator<Map.Entry<Integer, MarketSnapshot>> iterator = marketSnapshotMap.entrySet().iterator();
                                while (iterator.hasNext()){
                                    Map.Entry<Integer, MarketSnapshot> itPair = iterator.next();
                                    toSend.add((Message) new MarketSnapshot(500, pair.getKey().get().getSenderID(), "W", null, itPair.getValue().getStockSnapshots()));
                                }
                            }
                            else {
                                toSend.add(pair.getKey().get());
                            }
                        }
                        //Registration
                        else {
                            if (pair.getKey().get().getType().equalsIgnoreCase("A")) {
                                if (clientMap.get(pair.getKey().get().getSenderID()) == null) {
                                    clientMap.put(pair.getKey().get().getSenderID(), pair.getKey().get().getSocket());
                                    toSend.add(new Message(500, pair.getKey().get().getSenderID(), "0", pair.getKey().get().getSocket()));
                                }
                            }
                        }
                        for(Message message : toSend) {
                            jobList.add(message);
                        }
                    }
                    deadFutureList.add(pair.getKey());
                }
            }

            //Clean the completed futures from the futureMap
            for (Future<Message> f : deadFutureList){
                if (futureMap.get(f).getPort() == 5000)
                    futureMap.put(executorService.submit(brokerReader), brokerReader);
                else if (futureMap.get(f).getPort() == 5001)
                    futureMap.put(executorService.submit(marketReader), marketReader);
                futureMap.remove(f);
            }

            if (!deadFutureList.isEmpty())
                deadFutureList.clear();

            for (int i = 0; i < jobList.size(); i++){
                executorService.submit(new ClientWriter(clientMap.get(jobList.get(i).getRecipientID()), jobList.get(i)));
                jobList.remove(jobList.get(i));
            }
        }
    }
}
