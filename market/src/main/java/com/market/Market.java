package com.market;

import com.market.ClientWriter;
import com.core.*;

import java.io.IOException;
import java.lang.Math;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

import static com.core.MathUtil.round;

public class Market {

    private static int id = -1;
    private static Socket socket;
    private static int SMAPeriod = 1;

    private static ConcurrentHashMap<Future<ArrayList<Order>>, ClientReader> futureMap = new ConcurrentHashMap<>();
    private static ArrayList<Message> messageQueue = new ArrayList<>();
    private static ArrayList<Future<ArrayList<Order>>> deadFutureList = new ArrayList<>();
    private static ArrayList<Order> orderList = new ArrayList<>();

    private static ExecutorService executorService = Executors.newCachedThreadPool();

    public static Portfolio portfolio = new Portfolio(
            new Stock("FIAT", 1.0, 1000000),
            new Stock("ASTOCK", 12.30, 100),
            new Stock("BSTOCK", 21.20, 50),
            new Stock("CSTOCK", 128.60, 30));

    private static Socket connect() throws IOException, ExecutionException, InterruptedException {
        //Create new Socket and send connection request to router on separate Thread
        Socket socket = new Socket("localhost", 5001);

        executorService.submit(new ClientWriter(socket, new Message("35=A")));

        ClientReader clientReader = new ClientReader(socket);
        futureMap.put(executorService.submit(clientReader), clientReader);

        while (id == -1){
            Iterator<Map.Entry<Future<ArrayList<Order>>, ClientReader>> it = futureMap.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry<Future<ArrayList<Order>>, ClientReader> pair = it.next();
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

    private static void randomizeStock(){

        //Double fluc[] = {.1, .2, -.1, -.2, -.5, .5, .3, -.3};

        Double fluc[] = {.1, .2};

        int random = (int)(Math.random() * ((1 - 0) + 1)) + 0;

        for (Stock stock : portfolio.getPortfolio()){
            stock.setPrice(round(stock.getPrice() + fluc[random], 2));
        }
    }

    private static void MarketReopen(int SMALimit){
        Timer timer = new Timer();
        System.out.println("Timer Init");
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                MarketSnapshot marketSnapshot = new MarketSnapshot(id, 500, "W", new ArrayList<String>(), true);
                randomizeStock();
                for(Stock stock : portfolio.getPortfolio()){
                    stock.newSMAPeriod(SMAPeriod);
                    marketSnapshot.addStock(stock.toFix());
                }
                System.out.println("SENDING A MARKET SNAPSHOT UPDATE");
                messageQueue.add(marketSnapshot);
                SMAPeriod++;
                if (SMAPeriod > SMALimit){
                    SMAPeriod = 1;
                }
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
                        socket = new Socket("localhost", 5001);
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
        MarketReopen(5);
        messageQueue();

        System.out.println("This Market has been assigned ID : " + id + " for this session");

        while (true){
            Iterator<Map.Entry<Future<ArrayList<Order>>, ClientReader>> it = futureMap.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry<Future<ArrayList<Order>>, ClientReader> pair = it.next();
                if (pair.getKey().isDone()){
                    if (pair.getKey().get() != null){
                        //Message is not from server and therefore constitutes an order
                        if (pair.getKey().get().get(0).getSenderID() != 500)
                            for (Order order : pair.getKey().get())
                                orderList.add(order);
                    }
                    deadFutureList.add(pair.getKey());
                }
            }

            for (Future<ArrayList<Order>> f : deadFutureList) {
                ClientReader clientReader = new ClientReader(socket);
                futureMap.put(executorService.submit(clientReader), clientReader);
                futureMap.remove(f);
            }
            if (!deadFutureList.isEmpty())
                deadFutureList.clear();

            //Business Logic
            for (int i = 0; i < orderList.size(); i++){
                Order order = orderList.get(i);
                if (portfolio.getStock(order.getStock()) != null){
                    //SESSION LEVEL REJECT
                    if (!order.isReady()){
                        messageQueue.add(new Message(id, order.getSenderID(), "3", true));
                        System.out.println("Session Level Reject");
                    }
                    //SESSION LEVEL VALIDATES
                    else {
                        if (order.isBuy()) {
                            //BUY ORDER WHERE BID IS LESS THAN  MARKET PRICE
                            if (order.getBid() <= portfolio.getStock(order.getStock()).getPrice() - .03) {
                                //TODO MARKET WILL SEARCH FOR BROKERS THAT WILL ACCEPT THE BID
                                System.out.println("UNACCEPTABLE BID");
                            }
                            //BUY ORDER WHERE BID IS GREATER THAN OR EQUAL TO MARKET PRICE
                            else {
                                //MARKET HAS THE AMOUNT OF STOCK REQUESTED
                                if (portfolio.getStock(order.getStock()).getHold() >= order.getQuantity()) {
                                    portfolio.getStock("FIAT").modHold((int)(order.getQuantity() * order.getBid()));
                                    portfolio.getStock(order.getStock()).modHold(-(order.getQuantity()));
                                    order.setDone(true);
                                    order.returnToSender("8");
                                    System.out.println("NEW BUY ORDER RECEIPT : " + order.toFix());
                                }
                                else {
                                    order.setDone(true);
                                    order.returnToSender("j");
                                    System.out.println("NEW BUY ORDER REJECT : " + order.toFix());
                                }
                                messageQueue.add(order);
                                /*System.out.println("Order Processed");
                                System.out.println("This Market is now trading the following instruments...");
                                System.out.println(portfolio.toString());*/
                            }
                        } else {
                            //SELL ORDER WHERE ASK IS GREATER THAN MARKET PRICE
                            if (order.getBid() >= portfolio.getStock(order.getStock()).getPrice()) {
                                //TODO MARKET WILL SEARCH FOR BUYERS WILLING TO ACCEPT BID
                            }
                            //SELL ORDER WHERE ASK IS LESS THAN OR EQUAL MARKET PRICE
                            else {
                                if (portfolio.getStock("FIAT").getHold() >= (order.getQuantity() * order.getBid())) {
                                    portfolio.getStock(order.getStock()).modHold(order.getQuantity());
                                    portfolio.getStock("FIAT").modHold(-(int)(order.getQuantity() * order.getBid()));
                                    order.setDone(true);
                                    order.returnToSender("8");
                                } else {
                                    order.setDone(true);
                                    order.returnToSender("j");
                                }
                                messageQueue.add(order);
                               /* System.out.println("Order Processed");
                                System.out.println("This Market is now trading the following instruments...");
                                System.out.println(portfolio.toString());*/
                            }
                        }
                    }
                }
            }
            orderList.clear();
        }
    }
}