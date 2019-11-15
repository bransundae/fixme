package com.core;

import java.net.Socket;

public class Message {

    protected String message;
    protected String type;
    protected int senderID = -1;
    protected int recipientID = -1;
    protected String status;
    protected boolean done = false;
    protected String checksum;

    public Message(String fixMessage){
        parseFix(fixMessage);
        this.message = fixMessage;
    }

    public Message(int senderID, int recipientID, String type, boolean done){
        this.senderID = senderID;
        this.recipientID = recipientID;
        this.type = type;
        this.done = done;
        this.setMessage(this.toFix());
    }

    public Message(){ 

    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public void returnToSender(String responseType){
        int temp = this.senderID;
        this.senderID = this.recipientID;
        this.recipientID = temp;
        this.type = responseType;
    }

    public void parseFix(String fixMessage){
        String soh = "" + (char)1;
        String split[] = fixMessage.split(soh);
        for (int i = 0 ; i < split.length; i++){
            String tag[] = split[i].split("=");
            if (tag.length > 1) {
                switch (tag[0]) {
                    //MESSAGE TYPE
                    case "35":
                        if (tag[1].equalsIgnoreCase("A")){
                            type = "A";
                        } else if (tag[1].equalsIgnoreCase("0")){
                            type = "0";
                        }
                        else if (tag[1].equalsIgnoreCase("D")){
                            type = "D";
                        }
                        else if (tag[1].equalsIgnoreCase("W")){
                            type = "W";
                        }
                        else if (tag[1].equalsIgnoreCase("V")){
                            type = "V";
                        }
                        else if (tag[1].equalsIgnoreCase("8")){
                            type = "8";
                        }
                        else if (tag[1].equalsIgnoreCase("j")){
                            type = "j";
                        }
                        else if (tag[1].equalsIgnoreCase("3")){
                            type = "3";
                        }
                        else{
                            type = "9";
                        }
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
                    //CHECKSUM
                    case "10":
                        checksum = tag[1];
                        break;
                        //STATUS
                    case "39":
                        if (tag[1].equalsIgnoreCase("1")){
                            done = false;
                        } else {
                            done = true;
                        }
                }
            }
            else {
                this.message = tag[0];
            }
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
        return this.message;
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

        if (done){
            if (i > 0)
                toReturn += soh;
            toReturn += "39=0";
        } else{
            if (i > 0)
                toReturn += soh;
            toReturn += "39=1";
        }

        if (checksum != null){
            if (i > 0)
                toReturn += soh;
            toReturn += "10="+checksum;
        }
        return toReturn;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getChecksum() {
        return checksum;
    }

    public String generateChecksum(String fixMessage){
        String soh = "" + (char)1;
        String arr[] = fixMessage.split(soh);
        long checksum = 0;
        for(int i = 0; i < arr.length; i++){
            String tag[] = arr[i].split("=");
            for (int j = 0; j < arr[i].length(); j++){
                if (tag[0] != "10")
                    checksum += (int)arr[i].charAt(j);
            }
        }
        checksum = 256 % checksum;

        if (checksum >= 100)
            return "" + checksum;
        else if (checksum >= 10)
            return "0" + checksum;
        else
            return "00" + checksum;

    }

    public boolean validateChecksum(String fixMessage){
        if (checksum != null){
            if (generateChecksum(fixMessage).equalsIgnoreCase(checksum)) {
                return true;
            }
        }
        return false;
    }
}
