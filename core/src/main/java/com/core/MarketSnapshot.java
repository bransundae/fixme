package com.core;

import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class MarketSnapshot extends Message {

    private ArrayList<String> stockSnapshots = new ArrayList<>();

    public MarketSnapshot(int senderID, int recipientID, String type, ArrayList<String> snapshots){
        super(senderID, recipientID, type);
        this.stockSnapshots = snapshots;
        this.message = toFix();
    }

    public MarketSnapshot(int senderID, int recipientID, String type, boolean done){
        super(senderID, recipientID, type);
        this.message = toFix();
    }

    public MarketSnapshot(String fixMessage){
        parseFix(fixMessage);
        this.message = toFix();
    }

    public void setStockSnapshots(ArrayList<String> stockSnapshots) {
        this.stockSnapshots = stockSnapshots;
    }

    public ArrayList<String> getStockSnapshots() {
        return stockSnapshots;
    }

    public void addStock(String stock){
        stockSnapshots.add(stock);
    }

    public Portfolio getPortfolio(){
        Portfolio portfolio = new Portfolio();
        for(String stock : stockSnapshots){
            portfolio.addStock(new Stock(stock));
        }
        return portfolio;
    }

    @Override
    public String toFix() {
        String soh = "" + (char) 1;
        String toReturn = "";

        if (stockSnapshots != null){
            for (int i = 0; i < stockSnapshots.size(); i++) {
                toReturn += stockSnapshots.get(i);
                if (i + 1 < stockSnapshots.size())
                    toReturn += soh;
            }
        }
        if (toReturn != "")
            return super.toFix() + soh + toReturn;
        else
            return super.toFix();
    }

    @Override
    public void parseFix(String fixMessage) {
        super.parseFix(fixMessage);
        String soh = "" + (char)1;
        String split[] = fixMessage.split(soh);
        for (int i = 0 ; i < split.length; i++) {
            String stock = "";
            String tag[] = split[i].split("=");
            if (tag.length > 1) {
                switch (tag[0]) {
                    //INSTRUMENT
                    case "55":
                        stock += split[i] + soh + split[i + 1] + soh + split[i + 2] + soh + split[i + 3];
                        stockSnapshots.add(stock);
                        break;
                }
            }
        }
    }
}
