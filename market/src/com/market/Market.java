package com.market;

import com.fixme.lib.Message;
import com.fixme.lib.Order;
import com.fixme.lib.Portfolio;
import com.fixme.lib.Stock;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Market {

    private static int id = -1;
    private static Socket socket;

    private static ExecutorService readerService = Executors.newCachedThreadPool();
    private static ExecutorService writerService = Executors.newCachedThreadPool();

    public static Portfolio portfolio = new Portfolio(
            new Stock("FIAT", 1.0, 1000000),
            new Stock("ASTOCK", 12.30, 100),
            new Stock("BSTOCK", 21.20, 50),
            new Stock("CSTOCK", 128.60, 30));

    private static Socket connect() throws IOException, ExecutionException, InterruptedException {

        HashMap<Future<Message>, ClientReader> futureMap = new HashMap<>();
        ArrayList<Future<Message>> deadFutureList = new ArrayList<>();

        //Create new Socket and send connection request to router on separate Thread
        socket = new Socket("localhost", 5001);

        writerService.submit(new ClientWriter(socket, "35=A"));

        while (id == -1){
            ClientReader clientReader = new ClientReader(socket, portfolio);
            futureMap.put(readerService.submit(clientReader), clientReader);

            Iterator<Map.Entry<Future<Message>, ClientReader>> it = futureMap.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry<Future<Message>, ClientReader> pair = it.next();
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

            for (Future<Message> f : deadFutureList)
                futureMap.remove(f);
            if (!futureMap.isEmpty())
                deadFutureList.clear();
        }

        return socket;
    }

    public static void main(String args[]) throws IOException, ExecutionException, InterruptedException {

        Socket socket = connect();
        HashMap<Future<Order>, ClientReader> futureMap = new HashMap<>();
        ArrayList<Future<Order>> deadFutureList = new ArrayList<>();
        ArrayList<Order> orderList = new ArrayList<>();

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        System.out.println("This Market has been assigned ID : " + id + " for this session");
        System.out.println("This Market is now trading the following instruments...");
        System.out.println(portfolio.toString());

        while (true){
            ClientReader clientReader = new ClientReader(socket, portfolio);
            futureMap.put(executorService.submit(clientReader), clientReader);

            Iterator<Map.Entry<Future<Order>, ClientReader>> it = futureMap.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry<Future<Order>, ClientReader> pair = it.next();
                if (pair.getKey().isDone()){
                    if (pair.getKey().get() != null){
                        //Message is not from server and therefore constitutes an order
                        if (pair.getKey().get().getSenderID() != 500){
                            //TODO Refactor Message to Parse FIX
                            orderList.add(pair.getKey().get());
                        }
                    }
                    deadFutureList.add(pair.getKey());
                }
            }

            for (Future<Order> f : deadFutureList)
                futureMap.remove(f);
            if (!deadFutureList.isEmpty())
                deadFutureList.clear();

            //Business Logic
            for (int i = 0; i < orderList.size(); i++){
                Order order = orderList.get(i);
                if (portfolio.getStock(order.getStock().getName()) != null){
                    if (!order.isReady()){
                        //TODO SESSION LEVEL REJECT
                        System.out.println("Order Rejected");
                    }
                    else {
                        if (order.isBuy()) {
                            if (order.getBid() <= portfolio.getStock(order.getStock().getName()).getPrice()) {
                                //If buyer is offering less than market price then market will search for sellers willing to accept bid
                                //TODO MARKET WILL SEARCH FOR BROKERS THAT WILL ACCEPT THE BID

                            } else {
                                //TODO MARKET WILL SELL STOCKS
                            }
                        } else {
                            if (order.getBid() >= portfolio.getStock(order.getStock().getName()).getPrice()) {
                                //If seller is asking for more than market price the market will search for buyers willing to accept the bid}
                            } else {
                                //If seller is asking for less than market price then the market will buy the stock and resell, keeping the profit
                                //TODO MARKET PURCHASE STOCKS
                                System.out.println("Business logic triggered");
                                order.setRecipientID(order.getSenderID());
                                order.setSenderID(id);
                                order.setStatus("2");
                                System.out.println("Business Logic: " + order.toFix());
                                socket = new Socket("localhost", 5001);
                                writerService.submit(new ClientWriter(socket, order.toFix()));
                                orderList.remove(order);
                            }
                        }
                    }
                }
            }
        }































        /*while (true){
            String input = "";
            String response = "";
            int num = 0;

            //Block until Market recieves input
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            int senderID = -1;
            int recipientID = -1;
            int order = -1;

            try {
                input = in.readLine();
                System.out.println("Got input: " + input);
            } catch (IOException e){
                System.out.println("Invalid Input");
            }

            String split[] = input.split("\\|");

            try {
                senderID = Integer.parseInt(split[0]);
                recipientID = Integer.parseInt(split[1]);
                order = Integer.parseInt(split[2]);
            } catch (NumberFormatException e){
                System.out.println("Invalid Input");
            }

            System.out.println("Broker : Requesting " + order + " shares...");

            if (order <= apples && order > 0){
                response = "1";
                apples -= order;
                System.out.println("Transaction Approved!");
            }
            else {
                response = "0";
                System.out.println("Transaction Refused!");
            }

            socket = new Socket("localhost", 5001);

            System.out.println(apples + " shares are availabe...");
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println(id + "|" + senderID + "|" + response);
        }*/
    }
}