package com.fixme;

import com.fixme.lib.Message;

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
                        if (pair.getKey().get().getType().equalsIgnoreCase("A")) {
                            if (clientMap.get(pair.getKey().get().getSenderID()) == null) {
                                clientMap.put(pair.getKey().get().getSenderID(), pair.getKey().get().getSocket());
                            }
                        }
                        //If the sender is previously registered to the router then simply overwrite it's stored socket with the socket it used to send the current message
                        else if (clientMap.get(pair.getKey().get().getSenderID()) != null){
                            clientMap.replace(pair.getKey().get().getSenderID(), pair.getKey().get().getSocket());
                        }
                        jobList.add(pair.getKey().get());
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
                if (clientMap.get(jobList.get(i).getRecipientID()) != null)
                    executorService.submit(new ClientWriter(clientMap.get(jobList.get(i).getRecipientID()), jobList.get(i)));
                else {
                    Message message = new Message(500, jobList.get(i).getSenderID(), "0", clientMap.get(jobList.get(i).getSenderID()));
                    message.setMessage(message.toFix());
                    executorService.submit(new ClientWriter(clientMap.get(jobList.get(i).getSenderID()), message));
                }
                jobList.remove(jobList.get(i));
            }
        }
    }
}
