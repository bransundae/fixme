package com.broker;

import com.core.Message;
import com.core.Portfolio;
import com.core.Stock;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class BusinessEngine {

    Portfolio portfolio;
    HashMap<Integer, Portfolio> marketMap;

    public BusinessEngine(Portfolio portfolio){
        this.portfolio = portfolio;
    }

    public void setMarketMap(String fixMessage){

    }

    public void SMAInstruments(){
        Iterator<Map.Entry<Integer, Portfolio>> it = marketMap.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry<Integer, Portfolio> pair = it.next();
            for (Stock stock : pair.getValue().getPortfolio()){
                
            }
        }
    }
}
