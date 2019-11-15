package com.broker;

import com.core.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.core.MathUtil.round;

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

    public ArrayList<Order> SMAInstruments(){
        ArrayList<Order> orders = new ArrayList<>();
        Iterator<Map.Entry<Integer, Portfolio>> it = marketMap.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry<Integer, Portfolio> pair = it.next();
            for (Stock stock : pair.getValue().getPortfolio()){
                if (stock.getMaxDifference() > 0.2 && !stock.getName().equalsIgnoreCase("FIAT")){
                    int hold = calcBuyAmount(stock, 10);
                    if (hold > 0) {
                        System.out.printf("STOCK: %S | SMADiff: %f | ACTION: %S\n", stock.getName(), stock.getMaxDifference(), "BUYING " + hold + " UNITS at " + stock.getPrice() + " per UNIT");
                        orders.add(new Order("D", id, pair.getKey(), stock.getName(), stock.getPrice(), hold, true));
                    } else {
                        System.out.printf("STOCK: %S | SMADiff: %f | ACTION: %S\n", stock.getName(), stock.getMaxDifference(), "HOLD");
                    }
                } else if (stock.getMaxDifference() < -0.5 && !stock.getName().equalsIgnoreCase("FIAT")){
                    int hold = calcSellAmount(stock, 50);
                    if (hold > 0) {
                        System.out.printf("STOCK: %S | SMADiff: %f | ACTION: %S\n", stock.getName(), stock.getMaxDifference(), "SELLING " + hold + " UNITS at " + stock.getPrice() + " per UNIT");
                        orders.add(new Order("D", id, pair.getKey(), stock.getName(), stock.getPrice(), hold, false));
                    } else {
                        System.out.printf("STOCK: %S | SMADiff: %f | ACTION: %S\n", stock.getName(), stock.getMaxDifference(), "HOLD");
                    }
                }
                else if (!stock.getName().equalsIgnoreCase("FIAT")){
                    System.out.printf("STOCK: %S | SMADiff: %f | ACTION: %S\n", stock.getName(), stock.getMaxDifference(), "HOLD");
                }
            }
        }

        for (int i = 0; i < orders.size(); i++){
            orders.get(i).setId(orders.get(i).getId());
            orders.get(i).setFragments(orders.size());
        }
        return orders;
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

    public int calcBuyAmount(Stock stock, int percent){
        double stockPrice = stock.getPrice();
        int fiat = portfolio.getStock("FIAT").getHold();
        if (fiat <= 0)
            return 0;
        return (int)((fiat / 100 * percent) / stockPrice);
    }

    public int calcSellAmount(Stock stock, int percent){
        int hold = portfolio.getStock(stock.getName()).getHold();
        if (hold <= 0)
            return 0;
        return (int)((hold / 100 * percent));
    }
}
