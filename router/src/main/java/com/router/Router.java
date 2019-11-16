package com.router;

import com.core.MarketSnapshot;
import com.core.Message;
import com.core.Order;
import com.core.io.ThreadWriter;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Router {
    private static ServerSocket broker;
    private static ServerSocket market;

    private static ServerReader marketReader;
    private static ServerReader brokerReader;

    private static Map<Integer, Socket> clientMap = new HashMap<>();
    private static ArrayList<Integer> deadClientList = new ArrayList<>();
    private static Map<Integer, MarketSnapshot> marketSnapshotMap = new HashMap<>();
    private static Map<Future<Message>, ServerReader> futureMap = new HashMap<>();

    private static ArrayList<Future<Message>> deadFutureList = new ArrayList<>();
    private static ArrayList<Message> messageQueue = new ArrayList<>();
    private static ArrayList<Message> messageBatch = new ArrayList<>();

    private static ExecutorService executorService = Executors.newCachedThreadPool();

    public static int clientID = 100000;


    private static void listenSocket() throws IOException {
        System.out.println("Server is Listening on Ports 5000 and 5001");

        broker = new ServerSocket(5000);
        market = new ServerSocket(5001);

        brokerReader = new ServerReader(broker);
        marketReader = new ServerReader(market);
    }

    private static void pruneDeadClients(){
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                for(Integer i : deadClientList){
                    System.out.println("REMOVING DEAD CLIENT ID: " + i);
                    clientMap.remove(i);
                }
                deadClientList.clear();
                Iterator<Map.Entry<Integer, Socket>> it = clientMap.entrySet().iterator();
                while (it.hasNext()){
                    Map.Entry<Integer, Socket> pair = it.next();
                    deadClientList.add(pair.getKey());
                    Message message = new Message(500, pair.getKey(), "0");
                    executorService.submit(new ThreadWriter(pair.getValue(), message));
                }
            }
        }, 0, 10000);
    }

    private static void messageQueue(){
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (messageQueue.size() > 0) {
                    executorService.submit(new ThreadWriter(clientMap.get(messageQueue.get(0).getRecipientID()), messageQueue.get(0)));
                    messageQueue.remove(0);
                }
            }
        }, 0, 500);
    }

    private static void messageBatch(){
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                for (Message message : messageBatch)
                    executorService.submit(new ThreadWriter(clientMap.get(message.getRecipientID()), message));
                messageBatch.clear();
            }
        }, 0, 500);
    }

    public static void main(String args[]) throws Exception {

        listenSocket();

        futureMap.put(executorService.submit(brokerReader), brokerReader);
        futureMap.put(executorService.submit(marketReader), marketReader);

        pruneDeadClients();
        messageQueue();
        messageBatch();

        while (true){
            //Iterate through the futureMap and if a Callable has returned a completed Future then add the message to the job queue and remove the future - ServerReader pair
            //If the sender of the message is not registered then store the socket from the ServerReader first
            Iterator<Map.Entry<Future<Message>, ServerReader>> it = futureMap.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry<Future<Message>, ServerReader> pair = it.next();
                if (pair.getKey().isDone()){
                    if (pair.getKey().get() != null) {
                        ArrayList<Message> toSend = new ArrayList<>();
                        //If the sender is previously registered to the router then simply overwrite it's stored socket with the socket it used to send the current message
                        if (clientMap.get(pair.getKey().get().getSenderID()) != null){
                            clientMap.replace(pair.getKey().get().getSenderID(), pair.getValue().getClient());

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
                                while (iterator.hasNext()) {
                                    Map.Entry<Integer, MarketSnapshot> itPair = iterator.next();
                                    MarketSnapshot marketSnapshot = new MarketSnapshot(itPair.getKey(), pair.getKey().get().getSenderID(), "W", itPair.getValue().getStockSnapshots());
                                    toSend.add((Message) marketSnapshot);
                                }
                                for (int i = 0; i < toSend.size(); i++){
                                    toSend.get(i).setId(System.currentTimeMillis() + "" + 500);
                                    toSend.get(i).setFragments(marketSnapshotMap.size());
                                }
                            }
                            //Business Orders
                            else if (pair.getKey().get().getType().equalsIgnoreCase("D") || pair.getKey().get().getType().equalsIgnoreCase("8")
                            || pair.getKey().get().getType().equalsIgnoreCase("j") || pair.getKey().get().getType().equalsIgnoreCase("3")){
                                toSend.add(new Order(pair.getKey().get().getMessage()));
                            }
                            //Pulse
                            else if (pair.getKey().get().getType().equalsIgnoreCase("0")){
                                System.out.println("FLAGGING CLIENT AS SAFE: " + pair.getKey().get().getSenderID());
                                for(int i = 0; i < deadClientList.size(); i++) {
                                    if (deadClientList.get(i) == pair.getKey().get().getSenderID()) {
                                        deadClientList.remove(deadClientList.get(i));
                                        break;
                                    }
                                }
                            }
                        }
                        //Registration
                        else {
                            if (pair.getKey().get().getType().equalsIgnoreCase("A")) {
                                if (clientMap.get(pair.getKey().get().getSenderID()) == null) {
                                    clientMap.put(pair.getKey().get().getSenderID(), pair.getValue().getClient());
                                    toSend.add(new Message(500, pair.getKey().get().getSenderID(), "0"));
                                }
                            }
                        }
                        for(Message message : toSend) {
                            if (message.getFragments() > 1)
                                messageBatch.add(message);
                            else
                                messageQueue.add(message);
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
        }
    }
}
