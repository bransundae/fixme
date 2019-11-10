package com.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class Stock {

    private String name;
    private int hold;
    private double price;
    private HashMap<Integer, ArrayList<Double>> averageMap = new HashMap<>();

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

    public void recordPrice(int period){
        averageMap.get(period).add(this.price);
    }

    public void newSMAPeriod(int period){
        if (averageMap.get(period) != null){
            for (int i = 0; i < averageMap.size(); i++){
                averageMap.replace(i + 1, averageMap.get(i + 2));
            }
            averageMap.replace(averageMap.size(), new ArrayList<>());
        }
        else {
            averageMap.put(period, new ArrayList<>());
        }
        recordPrice(period);
    }

    public double getSMA() {
        double SMA = 0;
        for (int i = 0; i < averageMap.size(); i++){
            for (double price : averageMap.get(i + 1)){
                SMA += price;
            }
        }
        return (SMA / averageMap.size());
    }

    @Override
    public String toString() {
        return "Stock : " + name + "\nHold : " + hold + "\nPrice : " + price + "868=";
    }

    public String toFix(){
        String soh = "" + (char)1;

        return "55=" + name + soh
                + "38=" + hold + soh
                + "44=" + price + soh
                + "868=" + getSMA();
    }
}
