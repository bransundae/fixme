package com.broker;

import com.fixme.lib.Message;
import com.fixme.lib.Order;
import com.fixme.lib.Portfolio;
import com.fixme.lib.Stock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class Broker {

    private static int id = -1;
    private static Socket socket;
    private static ExecutorService readerService = Executors.newFixedThreadPool(2);
    private static ExecutorService writerService = Executors.newFixedThreadPool(2);

    public static Portfolio portfolio = new Portfolio(
            new Stock("FIAT", 1.0, 10),
            new Stock("ASTOCK", 12.30, 10),
            new Stock("BSTOCK", 21.20, 5),
            new Stock("CSTOCK", 128.60, 3));

    private static Socket connect() throws IOException, ExecutionException, InterruptedException {

        HashMap<Future<Message>, ClientReader> futureMap = new HashMap<>();
        ArrayList<Future<Message>> deadFutureList = new ArrayList<>();

        //Create a new Socket and send registration request to router
        socket = new Socket("localhost", 5000);

        writerService.submit(new ClientWriter(socket, "35=A"));

        while (id == -1) {
            ClientReader clientReader = new ClientReader(socket);
            futureMap.put(readerService.submit(clientReader), clientReader);

            Iterator<Map.Entry<Future<Message>, ClientReader>> it = futureMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Future<Message>, ClientReader> pair = it.next();
                if (pair.getKey().isDone()) {
                    if (pair.getKey() != null) {
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
        HashMap<Future<Message>, ClientReader> futureMap = new HashMap<>();
        ArrayList<Future<Message>> deadFutureList = new ArrayList<>();
        ArrayList<Order> responseList = new ArrayList<>();

        Future<String> inputFuture = null;
        String input;

        ExecutorService executorService = Executors.newFixedThreadPool(3);

        System.out.println("This Broker has been assigned ID : " + id + " for this session");
        System.out.println("This Broker is now trading the following instruments...");
        System.out.println(portfolio.toString());

        while (true){
            InputReader inputReader = new InputReader();

            if (inputFuture == null || inputFuture.isCancelled()) {
                inputFuture = executorService.submit(inputReader);
                try {
                    input = inputFuture.get(200, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    System.out.println("Cancelling Input Reading");
                    inputFuture.cancel(true);
                }
            }
            else if (inputFuture.isDone()){

            }

            ClientReader clientReader = new ClientReader(socket);
            futureMap.put(executorService.submit(clientReader), clientReader);

            Iterator<Map.Entry<Future<Message>, ClientReader>> it = futureMap.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry<Future<Message>, ClientReader> pair = it.next();
                if (pair.getKey().isDone()){
                    if (pair.getKey().get() != null){
                        //Message is not from server and therefore constitutes an order
                        if (pair.getKey().get().getSenderID() != 500 && pair.getKey().get().getMessage() != null){
                            //TODO Refactor Message to Parse FIX
                            responseList.add(new Order(pair.getKey().get().getMessage(), pair.getKey().get().getSocket(), portfolio));
                        }
                    }
                    deadFutureList.add(pair.getKey());
                }
            }

            for (Future<Message> f : deadFutureList)
                futureMap.remove(f);
            if (!deadFutureList.isEmpty())
                deadFutureList.clear();

            //Business Logic
            for (int i = 0; i < responseList.size(); i++){
                System.out.println("Response From Router : " + responseList.get(i).toFix());
            }
        }

        /*BufferedReader in;
        PrintWriter out;

        while (true) {
            String input = "";
            socket = new Socket("localhost", 5000);
            Scanner user = new Scanner(System.in);
            System.out.println("Enter Market ID...");
            input += user.next();
            System.out.println("Enter your Order...");
            input += "|"+user.next();
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println(id + "|" + input);

            System.out.println("Requesting " + input + " shares from Market...");

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            int status = Integer.parseInt(in.readLine().split("\\|")[2]);

            if (status == 1)
                System.out.println("Market : Executed");
            else if (status == 0)
                System.out.println("Market : Rejected");
            else if (status == -1)
                System.out.println("Router : Market address could not be found");
        }*/
    }
}
