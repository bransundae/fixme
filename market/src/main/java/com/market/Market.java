package com.market;

import com.core.io.ThreadReader;
import com.core.io.ThreadWriter;
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

    private static ConcurrentHashMap<Future<Message>, ThreadReader> futureMap = new ConcurrentHashMap<>();
    private static ArrayList<Message> messageQueue = new ArrayList<>();
    private static ArrayList<Future<Message>> deadFutureList = new ArrayList<>();
    private static HashMap<String, ArrayList<Order>> orderMap = new HashMap<>();
    private static ArrayList<String> deadOrderList = new ArrayList<>();

    private static ExecutorService executorService = Executors.newCachedThreadPool();

    public static Portfolio portfolio = new Portfolio(
            new Stock("FIAT", 1.0, 1000000),
            new Stock("ASTOCK", 12.30, 100),
            new Stock("BSTOCK", 21.20, 50),
            new Stock("CSTOCK", 128.60, 30));

    private static Socket connect() throws IOException, ExecutionException, InterruptedException {
        //Create new Socket and send connection request to router on separate Thread
        Socket socket = new Socket("localhost", 5001);

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
            if (!stock.getName().equalsIgnoreCase("FIAT"))
                stock.setPrice(round(stock.getPrice() + fluc[random], 2));
        }
    }

    private static void MarketReopen(int SMALimit){
        Timer timer = new Timer();
        System.out.println("Timer Init");
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                MarketSnapshot marketSnapshot = new MarketSnapshot(id, 500, "W", new ArrayList<String>());
                randomizeStock();
                for(Stock stock : portfolio.getPortfolio()){
                    stock.newSMAPeriod(SMAPeriod);
                    marketSnapshot.addStock(stock.toFix());
                }
                portfolio.print();
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
                    executorService.submit(new ThreadWriter(socket, messageQueue.get(0)));
                    ThreadReader threadReader = new ThreadReader(socket);
                    futureMap.put(executorService.submit(new ThreadReader(socket)), threadReader);
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
            Iterator<Map.Entry<Future<Message>, ThreadReader>> it = futureMap.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry<Future<Message>, ThreadReader> pair = it.next();
                if (pair.getKey().isDone()){
                    if (pair.getKey().get() != null){
                        //Message is not from server and therefore constitutes an order
                        if (pair.getKey().get().getSenderID() != 500) {
                            Message message = pair.getKey().get();
                            if (orderMap.get(message.getId()) == null){
                                ArrayList<Order> fragments = new ArrayList();
                                fragments.add((Order) message);
                                orderMap.put(message.getId(), fragments);
                            } else {
                                orderMap.get(message.getId()).add((Order) message);
                            }
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

            //Business Logic
            Iterator<Map.Entry<String, ArrayList<Order>>> orderMapIterator = orderMap.entrySet().iterator();
            while (orderMapIterator.hasNext()){
                Map.Entry<String, ArrayList<Order>> pair = orderMapIterator.next();
                //IF ALL FRAGMENTS HAVE BEEN RECEIVED FOR THIS MESSAGE ID
                if (pair.getValue().size() == pair.getValue().get(0).getFragments()){
                    for (int i = 0; i < pair.getValue().size(); i++){
                        Order order = pair.getValue().get(i);
                        if (portfolio.getStock(order.getStock()) != null){
                            //SESSION LEVEL REJECT
                            if (!order.isReady()){
                                messageQueue.add(new Message(id, order.getSenderID(), "3"));
                                System.out.println("Session Level Reject");
                            }
                            //SESSION LEVEL VALIDATES
                            else {
                                if (order.isBuy()) {
                                    //BUY ORDER WHERE BID IS LESS THAN  MARKET PRICE
                                    if (order.getBid() <= portfolio.getStock(order.getStock()).getPrice() - .05) {
                                        //TODO MARKET WILL SEARCH FOR BROKERS THAT WILL ACCEPT THE BID
                                        System.out.println("UNACCEPTABLE BID : " + order.toFix());
                                    }
                                    //BUY ORDER WHERE BID IS GREATER THAN OR EQUAL TO MARKET PRICE
                                    else {
                                        //MARKET HAS THE AMOUNT OF STOCK REQUESTED
                                        if (portfolio.getStock(order.getStock()).getHold() >= order.getQuantity()) {
                                            portfolio.getStock("FIAT").modHold((int)(order.getQuantity() * order.getBid()));
                                            portfolio.getStock(order.getStock()).modHold(-(order.getQuantity()));
                                            order.setFragments(1);
                                            order.returnToSender("8");
                                            System.out.println("NEW BUY ORDER RECEIPT : " + order.toFix());
                                        }
                                        else {
                                            order.setFragments(1);
                                            order.returnToSender("j");
                                            System.out.println("NEW BUY ORDER REJECT : " + order.toFix());
                                        }
                                        messageQueue.add(order);
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
                                            order.setFragments(1);
                                            order.returnToSender("8");
                                        } else {
                                            order.setFragments(1);
                                            order.returnToSender("j");
                                        }
                                        messageQueue.add(order);
                                    }
                                }
                            }
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