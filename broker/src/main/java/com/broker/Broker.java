package com.broker;

import com.core.*;
import com.core.database.Database;
import com.core.io.ThreadReader;
import com.core.io.ThreadWriter;
import org.graalvm.compiler.core.common.type.ArithmeticOpTable;

import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class Broker {

    private static int id = -1;
    private static Socket socket;
    private static ExecutorService executorService = Executors.newCachedThreadPool();
    private static ConcurrentHashMap<Future<Message>, ThreadReader> futureMap = new ConcurrentHashMap<>();
    private static ArrayList<Future<Message>> deadFutureList = new ArrayList<>();
    private static HashMap<String, ArrayList<Message>> orderMap = new HashMap<>();
    private static ArrayList<String> deadOrderList = new ArrayList<>();
    private static ArrayList<Message> messageQueue = new ArrayList<>();

    private static BusinessEngine businessEngine;

    public static Portfolio portfolio = new Portfolio(
            new Stock("FIAT", 1.0, 1000),
            new Stock("ASTOCK", 12.30, 10),
            new Stock("BSTOCK", 21.20, 5),
            new Stock("CSTOCK", 128.60, 3));

    private static Socket connect() throws ExecutionException, InterruptedException {
        //Create new Socket and send connection request to router on separate Thread
        Socket socket = null;
        try {
            socket = new Socket("localhost", 5000);
        } catch (IOException e) {
            System.out.println("ERROR REATTEMPT: Router Not Found");
            return null;
        }

        executorService.submit(new ThreadWriter(socket, new Message("35=A")));

        ThreadReader threadReader = new ThreadReader(socket);
        futureMap.put(executorService.submit(threadReader), threadReader);

        while (id == -1){
            Iterator<Map.Entry<Future<Message>, ThreadReader>> it = futureMap.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry<Future<Message>, ThreadReader> pair = it.next();
                if (pair.getKey().isDone()){
                    if (pair.getKey() != null){
                        if (pair.getKey().get() != null) {
                            if (pair.getKey().get().getSenderID() == 500 && pair.getKey().get().getMessage() != null) {
                                try {
                                    id = pair.getKey().get().getRecipientID();
                                } catch (NumberFormatException e) {
                                    System.out.println("ERROR FATAL: Router Attempted to assign an Invalid ID");
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
                    System.out.println("ERROR FATAL: Router Not Found");
                    System.exit(-1);
                }
                System.out.println("This Broker is currently trading the following instruments...");
                portfolio.print();
                System.out.println("REQUESTING A MARKET SNAPSHOT UPDATE");
                executorService.submit(new ThreadWriter(socket, message));
                ThreadReader threadReader = new ThreadReader(socket);
                futureMap.put(executorService.submit(threadReader), threadReader);
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
                        System.out.println("ERROR FATAL: Router Not Found");
                        System.exit(-1);
                    }
                    executorService.submit(new ThreadWriter(socket, messageQueue.get(0)));
                    ThreadReader threadReader = new ThreadReader(socket);
                    futureMap.put(executorService.submit(new ThreadReader(socket)), threadReader);
                    messageQueue.remove(0);
                }
            }
        }, 0, 1000);
    }

    public static void main(String args[]) throws ExecutionException, InterruptedException {
        while (socket == null){
            socket = connect();
        }

        messageQueue();
        businessEngine = new BusinessEngine(portfolio, id);

        System.out.println("This com.broker.Broker has been assigned ID : " + id + " for this session");
        System.out.println(portfolio.toString());

        RequestMarketData();
        Trade();

        while (true){
            Iterator<Map.Entry<Future<Message>, ThreadReader>> it = futureMap.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry<Future<Message>, ThreadReader> pair = it.next();
                if (pair.getKey().isDone()){
                    if (pair.getKey().get() != null) {
                        if (pair.getKey().get().getSenderID() != 500){
                            if (orderMap.get(pair.getKey().get().getId()) == null) {
                                ArrayList<Message> fragments = new ArrayList();
                                fragments.add(pair.getKey().get());
                                orderMap.put((pair.getKey().get()).getId(), fragments);
                            } else {
                                orderMap.get(pair.getKey().get().getId()).add(pair.getKey().get());
                            }
                        } else {
                            if (pair.getKey().get().getType().equalsIgnoreCase("0"))
                                messageQueue.add(new Message(id, 500, "0"));
                            else if (pair.getKey().get().getType().equalsIgnoreCase("A"))
                                socket = connect();
                        }
                    }
                    deadFutureList.add(pair.getKey());
                }
            }

            for (Future<Message> f : deadFutureList) {
                ThreadReader threadReader = new ThreadReader(socket);
                futureMap.put(executorService.submit(threadReader), threadReader);
                futureMap.remove(f);
            }
            if (!deadFutureList.isEmpty())
                deadFutureList.clear();


            Iterator<Map.Entry<String, ArrayList<Message>>> orderMapIterator = orderMap.entrySet().iterator();
            while (orderMapIterator.hasNext()) {
                Map.Entry<String, ArrayList<Message>> pair = orderMapIterator.next();
                //IF ALL FRAGMENTS HAVE BEEN RECEIVED FOR THIS MESSAGE ID
                if (pair.getValue().size() == pair.getValue().get(0).getFragments()) {
                    for (int i = 0; i < pair.getValue().size(); i++){
                        Message message = pair.getValue().get(i);
                        //Message is an Order Reject
                        if (message.getType() == "j"){
                            Order order = (Order)message;
                            System.out.println("ORDER REJECT : " + order.toFix());
                        }
                        //Message is an Order Accept
                        else if (message.getType() == "8"){
                            Order order = (Order)message;
                            if (order.isBuy()) {
                                portfolio.getStock("FIAT").modHold(-(int) (order.getQuantity() * order.getBid()));
                                portfolio.getStock(order.getStock()).modHold(order.getQuantity());
                            }
                            else {
                                portfolio.getStock("FIAT").modHold((int) (order.getQuantity() * order.getBid()));
                                portfolio.getStock(order.getStock()).modHold(-(order.getQuantity()));
                            }
                            System.out.println("ORDER RECEIPT : " + order.toFix());
                        }
                        //Message is Market DataSnapShot
                        else if (message.getType() == "W"){
                            MarketSnapshot marketSnapshot = (MarketSnapshot)message;
                            System.out.println("MARKET DATA SNAPSHOTS RECEIVED : " + marketSnapshot.toFix());
                            businessEngine.updateMarketMap(marketSnapshot);
                        }
                        //Message is a Market DataSnapshot Reject
                        else if (message.getType() == "Y"){
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
