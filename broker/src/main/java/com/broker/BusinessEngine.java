package com.broker;

import com.core.MarketSnapshot;
import com.core.Message;
import com.core.Portfolio;
import com.core.Stock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class BusinessEngine {

    Portfolio portfolio;
    HashMap<Integer, Portfolio> marketMap = new HashMap<>();
    HashMap<Integer, HashMap<String, ArrayList<Double>>> SMAMap = new HashMap<>();

    public BusinessEngine(Portfolio portfolio){
        this.portfolio = portfolio;
    }

    public void updateMarketMap(ArrayList<MarketSnapshot> marketSnapshots){
        for(MarketSnapshot marketSnapshot : marketSnapshots){
            if (marketMap.get(marketSnapshot.getSenderID()) == null){
                marketMap.put(marketSnapshot.getSenderID(), marketSnapshot.getPortfolio());
            } else {
                marketMap.replace(marketSnapshot.getSenderID(), marketSnapshot.getPortfolio());
            }
        }

        ArrayList<Integer> deletion = new ArrayList<>();

        Iterator<Map.Entry<Integer, Portfolio>> it = marketMap.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry<Integer, Portfolio> pair = it.next();
            for (MarketSnapshot marketSnapshot : marketSnapshots){
                if (marketMap.get(marketSnapshot.getSenderID()) == null){
                    deletion.add(marketSnapshot.getSenderID());
                }
            }
        }

        for (Integer i : deletion){
            marketMap.remove(i);
        }
    }

    public void SMAInstruments(){
        Iterator<Map.Entry<Integer, Portfolio>> it = marketMap.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry<Integer, Portfolio> pair = it.next();
            for (Stock stock : pair.getValue().getPortfolio()){
                if (stock.getMaxDifference() > 0.5){
                    System.out.println("BUY");
                } else if (stock.getMaxDifference() < -0.5){
                    System.out.println("SELL");
                }
                else{
                    System.out.println("HOLD");
                }
            }
        }
    }
}
