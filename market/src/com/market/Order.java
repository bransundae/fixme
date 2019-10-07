package com.market;

import java.io.IOException;

public class Order {
    private int OrderID;
    private int clientID;
    private int recipientID;
    private Stock stock;
    private int quantity;
    private boolean buy;
    private double bid;

    public Order(String fixMessage){
        String soh = "" + (char)1;
        String split[] = fixMessage.split(soh);
        for (int i = 0 ; i < split.length; i++){
            String tag[] = split[i].split("=");
            if (tag[1] != null) {
                switch (tag[0]) {
                /*//FIX VERSION
                case "8" :
                    if (tag[1] == ""){
                    }
                    break ;

                    //ClientOrderID
                case "11" :


                //TYPE
                case "35" :
                    if () {

                    }
                    break ;*/
                    //BUY OR SELL
                    case "54":
                        if (tag[1] == "1")
                            buy = true;
                        else
                            buy = false;
                        break;
                    //RECIPIENT
                    case "56":
                        try {
                            recipientID = Integer.parseInt(tag[1]);
                        } catch (NumberFormatException e){
                            System.out.println("FIX ERROR");
                        }
                        break;

                    //CLIENT
                    case "115":
                        try {
                            clientID = Integer.parseInt(tag[1]);
                        } catch (NumberFormatException e){
                            System.out.println("FIX ERROR");
                        }
                        break;

                    //STOCK
                    case "55":
                        stock = Market.portfolio.getStock(tag[1]);
                        break;

                    //PRICE
                    case "44":
                        bid = Double.parseDouble(tag[1]);
                        break;
                }
            }
        }
    }

    public Order(int clientID, Stock stock, int quantity, double bid, boolean buy){
        this.clientID = clientID;
        this.stock = stock;
        this.quantity = quantity;
        this.bid = bid;
        this.buy = buy;
    }

    public boolean isBuy() {
        return buy;
    }

    public void setBuy(boolean buy) {
        this.buy = buy;
    }

    public void setClientID(int clientID) {
        this.clientID = clientID;
    }

    public void setBid(double bid) {
        this.bid = bid;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public double getBid() {
        return bid;
    }

    public int getClientID() {
        return clientID;
    }

    public int getQuantity() {
        return quantity;
    }

    public Stock getStock() {
        return stock;
    }
}
