package com.ass2;

import java.io.Serializable;
import java.math.BigDecimal;

public class Transaction implements Serializable {
    private String command;
    private String id;

    public Transaction(String command, String id) {
        this.command = command;
        this.id = id;
    }

    // Getter methods for command and id
    public String getId() {
        return id;
    }

    public String getCommand() {
        return command;
    }

    public String commandName() {
        String[] parts = command.split(" ");
        return parts[0];
    }

    public String[] commandArgs() {
        String[] parts = command.split(" ");
        return parts.length > 1 ? parts[1].split(" ") : new String[0];
    }

	public void process(Replica replica) {
		replica.processCommand(this.command);
	}

    @Override
    public String toString() {
        return "Transaction{cmd=" + command + ", id=" + id + "}";
    }
}

