package com.broker;

import com.fixme.lib.Order;
import com.fixme.lib.Portfolio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Callable;

public class InputReader implements Callable {

    private BufferedReader reader;
    private Order order;
    private int senderID = -1;
    private Map<String, String> messagesMap = Map.of(
            "56=","Enter Recipient ID :",
            "54=","Enter Order Type 1 or 2 (buy or sell) :",
            "55=","Enter Stock to Trade :",
            "38=","Enter Quantity :",
            "44=","Enter Price per Unit :"
    );

    public InputReader(int id, Portfolio portfolio){
        this.order = new Order(portfolio);
        this.senderID = id;
    }

    @Override
    public Object call() throws Exception {
        String soh = "" + (char)1;
        String fix = "35=D" + soh + "115=" + this.senderID + soh;
        int count = 0;

        Iterator<Map.Entry<String, String>> it = messagesMap.entrySet().iterator();

        do {
            if (it.hasNext()) {
                Map.Entry<String, String> pair = it.next();
                reader = new BufferedReader(new InputStreamReader(System.in));
                try {
                    System.out.println(pair.getValue());
                    while (!reader.ready()) {
                        Thread.sleep(2);
                    }
                    fix += pair.getKey() + reader.readLine();
                    order.parseFix(fix);
                    if (count < 4)
                        fix += soh;
                    count++;
                } catch (InterruptedException e) {
                    return null;
                }
            }
            System.out.println(order.toFix());
        } while (!order.isReady());
        return order;
    }
}
