package com.ass2;

import java.io.Serializable;

public class Transaction implements Serializable {
    private String command;
    private String uniqueId;

    public Transaction(String command, String uniqueId) {
        this.command = command;
        this.uniqueId = uniqueId;
    }

    public String getCommand() {
        return command;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "command='" + command + '\'' +
                ", uniqueId='" + uniqueId + '\'' +
                '}';
    }
}

