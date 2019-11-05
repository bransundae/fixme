package com.fixme.lib;

import java.util.ArrayList;

public class Portfolio {

    private ArrayList<Stock> portfolio;

    public Portfolio (Stock... stocks){
        this.portfolio = new ArrayList<>();
        for (Stock s : stocks){
            this.portfolio.add(s);
        }
    }

    public void setPortfolio(ArrayList<Stock> portfolio) {
        this.portfolio = portfolio;
    }

    public ArrayList<Stock> getPortfolio() {
        return portfolio;
    }

    public void addStock(Stock stock){
        this.portfolio.add(stock);
    }

    public void removeStock(Stock stock){
        this.portfolio.remove(stock);
    }

    public Stock getStock(String name){
        for (Stock s : this.portfolio){
            if (s.getName().equalsIgnoreCase(name))
                return s;
        }
        return null;
    }

    @Override
    public String toString() {

        String toReturn = "";

        for (Stock s : portfolio){
            if (s != portfolio.get(0))
                toReturn += "\n";
            toReturn += s.toString();
        }

        return toReturn;
    }
}
