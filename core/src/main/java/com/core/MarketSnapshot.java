package com.core;

import java.net.Socket;
import java.util.ArrayList;

public class MarketSnapshot extends Message {

    private ArrayList<String> stockSnapshots = new ArrayList<>();

    public MarketSnapshot(int senderID, int recipientID, String type, Socket socket, ArrayList<String> snapshots ){
        super(senderID, recipientID, type, socket);
        this.stockSnapshots = snapshots;
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

    @Override
    public String toFix() {
        String soh = "" + (char)1;
        String toReturn = "";

        for(int i = 0; i < stockSnapshots.size(); i++){
            toReturn += stockSnapshots.get(i);
            if (i + 1 < stockSnapshots.size())
                toReturn += soh;
        }
        return super.toFix() + toReturn;
    }
}
