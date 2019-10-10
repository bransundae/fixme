package com.fixme.lib;

import java.net.Socket;

public class Message {

    protected String message;
    protected String type;
    protected int senderID = -1;
    protected int recipientID = -1;
    protected Socket socket;
    protected String status;

    public Message(String fixMessage, Socket socket){
        this.message = fixMessage;
        String soh = "" + (char)1;
        String split[] = fixMessage.split(soh);
        for (int i = 0 ; i < split.length; i++){
            String tag[] = split[i].split("=");
            if (tag.length > 1) {
                switch (tag[0]) {
                    case "35":
                        if (tag[1].equalsIgnoreCase("A")){
                            type = "A";
                        } else if (tag[1].equalsIgnoreCase("0")){
                            type = "0";
                        }
                        else if (tag[1].equalsIgnoreCase("D")){
                            type = "D";
                        }
                        else{
                            type = "9";
                        }
                        System.out.println(type);
                        break;
                    //RECIPIENT
                    case "56":
                        try {
                            recipientID = Integer.parseInt(tag[1]);
                        } catch (NumberFormatException e){
                            System.out.println("FIX ERROR RECIPIENT");
                        }
                        break;
                    //SENDER
                    case "115":
                        try {
                            senderID = Integer.parseInt(tag[1]);
                        } catch (NumberFormatException e){
                            System.out.println("FIX ERROR SENDER");
                        }
                        break;
                }
            }
            else {
                this.message = tag[0];
            }
        }
        this.message = fixMessage;
        this.socket = socket;
    }

    public Message(int senderID, int recipientID, String type, Socket socket){
        this.senderID = senderID;
        this.recipientID = recipientID;
        this.type = type;
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public String getStatus() {
        return this.status;
    }

    public void setRecipientID(int recipient) {
        this.recipientID = recipient;
    }

    public void setSenderID(int sender) {
        this.senderID = sender;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return this.message;
    }

    public String toFix(){
        String soh = "" + (char)1;
        String toReturn = "";
        int i = 0;

        if (type != null){
            toReturn += "35="+type;
            i++;
        }

        if (senderID != -1){
            if (i == 1)
                toReturn += soh;
            toReturn += "115="+senderID;
        }

        if (recipientID != -1){
            if (i > 0)
                toReturn += soh;
            toReturn += "56="+recipientID;
        }
        return toReturn;
    }
}
