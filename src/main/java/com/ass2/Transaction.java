package com.ass2;

public class Transaction{
    String command;
    String id;


    public Transaction(String command, String id) {
        this.command = command;
        this.id = id;
    
    }

    // Getter methods for command and id.

    public String getCommand() {
        return command;
    }

    public String getId() {
        return id;
    }
}