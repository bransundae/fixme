package com.fixme.lib;

import java.net.Socket;

public class Order extends Message{

    private Stock stock;
    private double quantity;
    private boolean buy;
    private double bid;

    public Order(String fixMessage, Socket socket, Portfolio portfolio){
        super(fixMessage, socket);
        String soh = "" + (char)1;
        String split[] = fixMessage.split(soh);
        for (int i = 0 ; i < split.length; i++){
            String tag[] = split[i].split("=");
            if (tag[1] != null) {
                switch (tag[0]) {
                    //BUY OR SELL
                    case "54":
                        if (tag[1] == "1")
                            buy = true;
                        else
                            buy = false;
                        break;

                    //STOCK
                    case "55":
                        stock = portfolio.getStock(tag[1]);
                        break;

                        //QUANTITY
                    case "38":
                        try {
                            quantity = Double.parseDouble(tag[1]);
                        } catch (NumberFormatException e){
                            System.out.println("FIX ERROR");
                        }

                    //PRICE
                    case "44":
                        try {
                            bid = Double.parseDouble(tag[1]);
                        } catch (NumberFormatException e){
                            System.out.println("FIX ERROR");
                        }
                        break;
                }
            }
        }
    }

    public Stock getStock() {
        return stock;
    }

    public double getQuantity() {
        return quantity;
    }

    public double getBid() {
        return bid;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public void setBid(double bid) {
        this.bid = bid;
    }

    public boolean isBuy() {
        return buy;
    }

    public void setBuy(boolean buy) {
        this.buy = buy;
    }
}
