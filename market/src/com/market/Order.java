package com.market;

public class Order {
    private int clientID;
    private Stock stock;
    private int quantity;
    private boolean buy;
    private double bid;

    public Order(int clientID, Stock stock, int quantity, double bid, boolean buy){
        this.clientID = clientID;
        this.stock = stock;
        this.quantity = quantity;
        this.bid = bid;
        this.buy = buy;
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
