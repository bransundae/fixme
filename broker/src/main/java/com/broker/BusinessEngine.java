package com.broker;

import com.core.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class BusinessEngine {
 
    Portfolio portfolio;
    int id = -1;
    HashMap<Integer, Portfolio> marketMap = new HashMap<>();
    HashMap<Integer, HashMap<String, ArrayList<Double>>> SMAMap = new HashMap<>();

    public BusinessEngine(Portfolio portfolio, int id){
        this.portfolio = portfolio;
        this.id = id;
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

    public void setPortfolio(Portfolio portfolio) {
        this.portfolio = portfolio;
    }

    public void updateMarketMap(MarketSnapshot marketSnapshot){
        if (marketMap.get(marketSnapshot.getSenderID()) == null){
            marketMap.put(marketSnapshot.getSenderID(), marketSnapshot.getPortfolio());
        } else {
            marketMap.replace(marketSnapshot.getSenderID(), marketSnapshot.getPortfolio());
        }
    }

    //TODO: MAKE THIS FUNCTION RETURN AN ORDER OBJECT
    public void SMAInstruments(){
        Iterator<Map.Entry<Integer, Portfolio>> it = marketMap.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry<Integer, Portfolio> pair = it.next();
            for (Stock stock : pair.getValue().getPortfolio()){
                if (stock.getMaxDifference() > 0.5){
                    System.out.printf("STOCK: %S | SMADiff: %f | ACTION: %S\n", stock.getName(), stock.getMaxDifference(), "BUY");
                    Order order = new Order("D", id, pair.getKey(), stock.getName(), stock.getPrice(), (stock.getHold()/2);
                } else if (stock.getMaxDifference() < -0.5){
                    System.out.printf("STOCK: %S | SMADiff: %f | ACTION: %S\n", stock.getName(), stock.getMaxDifference(), "SELL");
                }
                else{
                    System.out.printf("STOCK: %S | SMADiff: %f | ACTION: %S\n", stock.getName(), stock.getMaxDifference(), "HOLD");
                }
            }
        }
    }

    public void SMAPeriod(){
        Iterator<Map.Entry<Integer, Portfolio>> it = marketMap.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry<Integer, Portfolio> pair = it.next();
            for (Stock stock : pair.getValue().getPortfolio()){
                Iterator<Map.Entry<Integer, Double>> iterator = stock.getSmaMap().entrySet().iterator();
                while (iterator.hasNext()){
                    Map.Entry<Integer, Double> pairit = iterator.next();
                    System.out.printf("STOCK: %S | PERIOD: %d | AVERAGE: %f\n", stock.getName(), pairit.getKey(), pairit.getValue());
                }
            }
        }
    }
}
