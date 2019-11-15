package com.broker;

import com.broker.ClientReader;
import com.broker.ClientWriter;
import com.core.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class Broker {

    private static int id = -1;
    private static Socket socket;
    private static ExecutorService executorService = Executors.newCachedThreadPool();
    private static ConcurrentHashMap<Future<ArrayList<Message>>, ClientReader> futureMap = new ConcurrentHashMap<>();
    private static ArrayList<Future<ArrayList<Message>>> deadFutureList = new ArrayList<>();
    private static ArrayList<Order> orderList = new ArrayList<>();
    private static ArrayList<Message> messageQueue = new ArrayList<>();

    public static Portfolio portfolio = new Portfolio(
            new Stock("FIAT", 1.0, 1000),
            new Stock("ASTOCK", 12.30, 10),
            new Stock("BSTOCK", 21.20, 5),
            new Stock("CSTOCK", 128.60, 3));

    private static Socket connect() throws IOException, ExecutionException, InterruptedException {
        //Create new Socket and send connection request to router on separate Thread
        Socket socket = new Socket("localhost", 5000);

        executorService.submit(new ClientWriter(socket, new Message("35=A")));

        ClientReader clientReader = new ClientReader(socket);
        futureMap.put(executorService.submit(clientReader), clientReader);

        while (id == -1){
            Iterator<Map.Entry<Future<ArrayList<Message>>, ClientReader>> it = futureMap.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry<Future<ArrayList<Message>>, ClientReader> pair = it.next();
                if (pair.getKey().isDone()){
                    if (pair.getKey() != null){
                        if (pair.getKey().get() != null) {
                            if (pair.getKey().get().get(0).getSenderID() == 500 && pair.getKey().get().get(0).getMessage() != null) {
                                try {
                                    id = pair.getKey().get().get(0).getRecipientID();
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
        }
        return socket;
    }

    private static void RequestMarketData(){
        Timer timer = new Timer();
        System.out.println("Timer Init");
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Message message = new Message(id, 500, "V", true);
                try {
                    socket = new Socket("localhost", 5000);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("REQUESTING A MARKET SNAPSHOT UPDATE");
                executorService.submit(new ClientWriter(socket, message));
                ClientReader clientReader = new ClientReader(socket);
                futureMap.put(executorService.submit(clientReader), clientReader);
            }
        }, 0, 20000);
    }

    private static void messageQueue(){
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (messageQueue.size() > 0) {
                    try {
                        socket = new Socket("localhost", 5000);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    executorService.submit(new ClientWriter(socket, messageQueue.get(0)));
                    ClientReader clientReader = new ClientReader(socket);
                    futureMap.put(executorService.submit(new ClientReader(socket)), clientReader);
                    messageQueue.remove(0);
                }
            }
        }, 0, 1000);
    }

    public static void main(String args[]) throws IOException, ExecutionException, InterruptedException {
        socket = connect();
        messageQueue();
        BusinessEngine businessEngine = new BusinessEngine(portfolio, id);

        System.out.println("This com.broker.Broker has been assigned ID : " + id + " for this session");
        System.out.println("This com.broker.Broker is now trading the following instruments...");
        System.out.println(portfolio.toString());

        RequestMarketData();

        while (true){
            Iterator<Map.Entry<Future<ArrayList<Message>>, ClientReader>> it = futureMap.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry<Future<ArrayList<Message>>, ClientReader> pair = it.next();
                if (pair.getKey().isDone()){
                    if (pair.getKey().get() != null){
                        //Message is an Order Reject
                        if (pair.getKey().get().get(0).getType() == "j"){
                            System.out.println("ORDER REJECTED : " + pair.getKey().get().get(0));
                        }
                        //Message is an Order Accept
                        else if (pair.getKey().get().get(0).getType() == "8"){
                            System.out.println("ORDER RECEIPT : " + pair.getKey().get().get(0));
                        }
                        //Message is Market DataSnapShot
                        else if (pair.getKey().get().get(0).getType() == "W"){
                            System.out.println("MARKET DATA SNAPSHOTS RECEIVED : " + pair.getKey().get().size());
                            for (int i = 0; i < pair.getKey().get().size(); i++){
                                businessEngine.updateMarketMap(new MarketSnapshot(pair.getKey().get().get(i).getMessage()));
                            }
                            ArrayList<Order> orders = businessEngine.SMAInstruments();
                            for (Order order : orders){
                                if (!orderList.contains(order))
                                    orderList.add(order);
                            }
                        }
                        //Message is a Market DataSnapshot Reject
                        else if (pair.getKey().get().get(0).getType() == "Y"){
                            //TODO: Handle Rejects
                        }
                    }
                    deadFutureList.add(pair.getKey());
                }
            }

            for (Future<ArrayList<Message>> f : deadFutureList) {
                ClientReader clientReader = new ClientReader(socket);
                futureMap.put(executorService.submit(clientReader), clientReader);
                futureMap.remove(f);
            }
            if (!deadFutureList.isEmpty())
                deadFutureList.clear();
            for (Order order : orderList){
                messageQueue.add(order);
            }
            orderList.clear();
        }
    }
}
