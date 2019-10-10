package com.fixme.lib;

import java.net.Socket;

public class Message {

    protected String message;
    protected String type;
    protected int senderID = -1;
    protected int recipientID = -1;
    protected Socket socket;
    protected boolean status;

    public Message(String fixMessage, Socket socket){
        this.message = fixMessage;
        String soh = "" + (char)1;
        String split[] = fixMessage.split(soh);
        for (int i = 0 ; i < split.length; i++){
            String tag[] = split[i].split("=");
            if (tag.length > 1) {
                switch (tag[0]) {
                    case "35":
                        if (tag[1] == "A"){
                            type = "register";
                        } else if (tag[1] == "0"){
                            type = "pulse";
                        }
                        else {
                            type = "order";
                        }
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
                }
            }
            else {
                this.message = tag[0];
            }
        }
        this.message = fixMessage;
        this.socket = socket;
        this.status = false;
    }

    public Message(int senderID, int recipientID, String type, Socket socket){
        this.senderID = senderID;
        this.recipientID = recipientID;
        this.type = type;
        this.socket = socket;
        this.status = false;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isStatus() {
        return status;
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
            toReturn += type;
            i++;
        }

        if (senderID != -1){
            if (i == 1)
                toReturn += soh;
            toReturn += senderID;
        }

        if (recipientID != -1){
            if (i > 0)
                toReturn += soh;
            toReturn += recipientID;
        }
        return toReturn;
    }
}
