package com.market;

import java.io.PrintWriter;
import java.net.Socket;

public class Message {

    private String message;
    private int senderID;
    private int recipientID;
    private Stock stock;
    private boolean buy;
    private double bid;
    private int quantity;
    private Socket socket;
    private boolean status;

    public Message(String fixMessage, Socket socket){
        String soh = "" + (char)1;
        String split[] = fixMessage.split(soh);
        for (int i = 0 ; i < split.length; i++){
            String tag[] = split[i].split("=");
            if (tag[1] != null) {
                switch (tag[0]) {
                    //BUY OR SELL
                    case "54":
                        if (tag[1] == "1")
                            buy = true;
                        else
                            buy = false;
                        break;
                    //RECIPIENT
                    case "56":
                        try {
                            recipientID = Integer.parseInt(tag[1]);
                        } catch (NumberFormatException e){
                            System.out.println("FIX ERROR");
                        }
                        break;
                    //CLIENT
                    case "115":
                        try {
                            senderID = Integer.parseInt(tag[1]);
                        } catch (NumberFormatException e){
                            System.out.println("FIX ERROR");
                        }
                        break;
                    //STOCK
                    case "55":
                        stock = Market.portfolio.getStock(tag[1]);
                        break;
                    //PRICE
                    case "44":
                        bid = Double.parseDouble(tag[1]);
                        break;
                }
            }
        }
        this.message = fixMessage;
        this.socket = socket;
        this.status = false;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public int getRecipientID() {
        return recipientID;
    }

    public int getSenderID() {
        return senderID;
    }

    public boolean getStatus() {
        return this.status;
    }

    public void setRecipientID(int recipient) {
        this.recipientID = recipient;
    }

    public void setSenderID(int sender) {
        this.senderID = sender;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public double getBid() {
        return bid;
    }

    public Stock getStock() {
        return stock;
    }

    public String getMessage() {
        return message;
    }

    public void setBid(double bid) {
        this.bid = bid;
    }

    public void setBuy(boolean buy) {
        this.buy = buy;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public boolean isBuy() {
        return buy;
    }

    public boolean isStatus() {
        return status;
    }

    @Override
    public String toString() {
        return this.message;
    }
}
