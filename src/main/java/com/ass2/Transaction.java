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

    // Executes the transaction on the provided Replica instance
    public void execute(Replica replica) {
        String[] parts = this.command.split(" ");
        String cmd = parts[0];

        switch (cmd) {
            case "deposit" -> {
                if (parts.length == 2) {
                    replica.deposit(new BigDecimal(parts[1]));
                } else {
                    System.out.println("Invalid number of arguments for deposit.");
                }
            }
            case "addInterest" -> {
                if (parts.length == 2) {
                    replica.addInterest(Integer.parseInt(parts[1]));
                } else {
                    System.out.println("Invalid number of arguments for interest.");
                }
            }
            case "getHistory" -> replica.getHistory();
            default -> System.out.println("Unknown command: " + command);
        }
    }

    @Override
    public String toString() {
        return "Transaction{cmd=" + command + ", id=" + id + "}";
    }
}

