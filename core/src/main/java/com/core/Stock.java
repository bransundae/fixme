package com.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.core.MathUtil.round;

public class Stock {

    private String name;
    private int hold;
    private double price;
    private HashMap<Integer, Double> smaMap = new HashMap<>();
    private ArrayList<Double> averages = new ArrayList<>();

    public Stock(String name, Double price, int hold){
        this.name = name;
        this.hold = hold;
        setPrice(price);
        averages.add(price);
    }

    public Stock(String name, Double price, int hold, ArrayList<Double> SMAs){
        this.name = name;
        this.hold = hold;
        setPrice(price);
        averages.add(price);
    }

    public int getHold() {
        return hold;
    }

    public void setHold(int hold) {
        this.hold = hold;
    }

    public Stock(String fixMessage){
        parseFix(fixMessage);
    }

    public HashMap<Integer, Double> getSmaMap() {
        return smaMap;
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
        this.price = round(price, 2);
        recordPrice();
    }

    public void modHold(int red){
        hold += red;
    }

    public void recordPrice(){
        averages.add(this.price);
    }

    public void newSMAPeriod(int period){
        if (smaMap.get(period) != null){
            for (int i = 0; i < smaMap.size(); i++){
                smaMap.replace(i + 1, smaMap.get(i + 2));
            }
            smaMap.replace(smaMap.size(), getSMA());
        }
        else {
            smaMap.put(period, getSMA());
        }
        averages.clear();
        recordPrice();
    }

    public double getSMA() {
        double SMA = 0;
        for (int i = 0; i < averages.size(); i++){
                SMA += averages.get(i);
        }
        return (round(SMA / averages.size(), 2));
    }

    @Override
    public String toString() {
        return "Stock : " + name + "\nHold : " + hold + "\nPrice : " + price;
    }

    public String toFix(){
        String soh = "" + (char)1;
        String toReturn = "55=" + name + soh
                + "38=" + hold + soh
                + "44=" + price + soh
                + "868=";
        Iterator<Map.Entry<Integer, Double>> it = smaMap.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry<Integer, Double> pair = it.next();
            toReturn += pair.getValue();
            if (it.hasNext()){
                toReturn += ",";
            }
        }
        return toReturn;
    }

    public void parseFix(String fixMessage){
        String soh = "" + (char)1;
        String split[] = fixMessage.split(soh);
        for (int i = 0 ; i < split.length; i++){
            String tag[] = split[i].split("=");
            if (tag.length > 1) {
                switch (tag[0]) {
                    //INSTRUMENT
                    case "55":
                        name = tag[1];
                        break;
                    //AMOUNT
                    case "38":
                        try {
                            hold = Integer.parseInt(tag[1]);
                        } catch (NumberFormatException e){
                            System.out.println("FIX ERROR HOLD");
                        }
                        break;
                    //PRICE
                    case "44":
                        try {
                            price = Double.parseDouble(tag[1]);
                        } catch (NumberFormatException e){
                            System.out.println("FIX ERROR SENDER");
                        }
                        break;
                    //SMA
                    case "868":
                        String SMA[] = tag[1].split(",");
                        String val = "";
                        try {
                            for (int j = 0; j < SMA.length; j++) {
                                if (SMA[j] != null) {
                                    smaMap.put(j + 1, Double.parseDouble(SMA[j]));
                                }
                            }
                        } catch (NumberFormatException e){
                            System.out.println("FIX ERROR SMA: " + val);
                        }
                        break;
                }
            }
        }
    }

    public double getMaxDifference(){
        double min = smaMap.get(1);
        double max = 0;
        Iterator<Map.Entry<Integer, Double>> it = smaMap.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry<Integer, Double> pair = it.next();
            if (!it.hasNext()){
                max = pair.getValue();
            }
        }
        return max - min;
    }
}
