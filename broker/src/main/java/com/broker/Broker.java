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
    private static HashMap<String, ArrayList<Order>> orderMap = new HashMap<>();
    private static ArrayList<String> deadOrderList = new ArrayList<>();
    private static ArrayList<Message> messageQueue = new ArrayList<>();

    private static BusinessEngine businessEngine;

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
                Message message = new Message(id, 500, "V");
                try {
                    socket = new Socket("localhost", 5000);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("This Broker is currently trading the following instruments...");
                portfolio.print();
                System.out.println("REQUESTING A MARKET SNAPSHOT UPDATE");
                executorService.submit(new ClientWriter(socket, message));
                ClientReader clientReader = new ClientReader(socket);
                futureMap.put(executorService.submit(clientReader), clientReader);
            }
        }, 0, 10000);
    }

    private static void Trade(){
        Timer timer = new Timer();
        System.out.println("Timer Init");
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                ArrayList<Order> orders = businessEngine.SMAInstruments();
                for (Order order : orders){
                    if (!messageQueue.contains(order))
                        messageQueue.add(order);
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
        businessEngine = new BusinessEngine(portfolio, id);

        System.out.println("This com.broker.Broker has been assigned ID : " + id + " for this session");
        System.out.println(portfolio.toString());

        RequestMarketData();
        Trade();

        while (true){
            Iterator<Map.Entry<Future<ArrayList<Message>>, ClientReader>> it = futureMap.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry<Future<ArrayList<Message>>, ClientReader> pair = it.next();
                if (pair.getKey().isDone()){
                    if (pair.getKey().get() != null){
                        if (orderMap.get(pair.getKey().get().get(0).getId()) == null){
                            ArrayList<Order> fragments = new ArrayList();
                            fragments.add((Order)pair.getKey().get().get(0));
                            orderMap.put(pair.getKey().get().get(0).getId(), fragments);
                        } else {
                            orderMap.get(pair.getKey().get().get(0).getId()).add((Order)pair.getKey().get().get(0));
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


            Iterator<Map.Entry<String, ArrayList<Order>>> orderMapIterator = orderMap.entrySet().iterator();
            while (orderMapIterator.hasNext()) {
                Map.Entry<String, ArrayList<Order>> pair = orderMapIterator.next();
                //IF ALL FRAGMENTS HAVE BEEN RECEIVED FOR THIS MESSAGE ID
                if (pair.getValue().size() == pair.getValue().get(0).getFragments()) {
                    for (int i = 0; i < pair.getValue().size(); i++){
                        Order order = pair.getValue().get(i);

                        //Message is an Order Reject
                        if (order.getType() == "j"){
                            System.out.println("ORDER REJECT : " + order.toFix());
                        }
                        //Message is an Order Accept
                        else if (order.getType() == "8"){
                            System.out.println("ORDER RECEIPT : " + order.toFix());
                        }
                        //Message is Market DataSnapShot
                        else if (order.getType() == "W"){
                            System.out.println("MARKET DATA SNAPSHOTS RECEIVED : " + order.toFix());
                            businessEngine.updateMarketMap(new MarketSnapshot(order.getMessage()));
                        }
                        //Message is a Market DataSnapshot Reject
                        else if (order.getType() == "Y"){
                            //TODO: Handle Rejects
                        }
                    }
                    deadOrderList.add(pair.getKey());
                }
            }

            for (String id : deadOrderList){
                orderMap.remove(id);
            }
            if (!deadOrderList.isEmpty())
                deadOrderList.clear();
        }
    }
}
