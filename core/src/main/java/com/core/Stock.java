package com.core;

public class Stock {

    private String name;
    private int hold;
    private double price;

    public Stock(String name, Double price, int hold){
        this.name = name;
        this.hold = hold;
        this.price = price;
    }

    public double getPrice() {
        return price;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "Stock : " + name + "\nHold : " + hold + "\nPrice : " + price;
    }
}
