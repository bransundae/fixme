package com.fixme;

import java.net.Socket;

public class Message {

    private String message;
    private int sender;
    private int recipient;
    private Socket socket;
    private boolean status;

    public Message(int sender, int recipient, String message){
        this.message = message;
        this.recipient = recipient;
        this.sender = sender;
        this.status = false;
    }

    public Message(int sender, int recipient, String message, Socket socket){
        this.message = message;
        this.recipient = recipient;
        this.sender = sender;
        this.socket = socket;
        this.status = false;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public int getRecipient() {
        return recipient;
    }

    public int getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    public boolean getStatus() {
        return this.status;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setRecipient(int recipient) {
        this.recipient = recipient;
    }

    public void setSender(int sender) {
        this.sender = sender;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return sender + "|" + recipient + "|" + message + "|" + status;
    }
}