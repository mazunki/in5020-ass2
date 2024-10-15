package com.ass2;

import java.io.Serializable;
import java.util.Arrays;

public class Transaction implements Serializable {
    String command;
    String id;


    public Transaction(String command, String id) {
        this.command = command;
        this.id = id;
    
    }

    // Getter methods for command and id.
    public String getId() {
        return id;
    }
    public String getCommand() {
        return command;
    }

    public String commandName() {
        String[] part = command.split(" ");
        return part[0];
    }

    public String[] commandArgs() {
        String[] part = command.split(" ");
        return Arrays.copyOfRange(part, 1, part.length);
    }

    public String getClientName() {
        String[] part = id.split(" ");
        return part[0];
    }

    public int getOutstandingCounter() {
        String[] part = id.split(" ");
        return Integer.parseInt(part[1]);
    }


    // Parses line, and figures out what to do with it
    public void execute(Client client) {
        String[] parts = this.command.split(" ");

        String cmd = parts[0];

        switch (cmd) {
            case "getQuickBalance" -> client.getQuickBalance();
            case "getSyncedBalance" -> client.getSyncedBalance();
            case "getHistory" -> client.getHistory();
            case "cleanHistory" -> client.cleanHistory();
            case "memberInfo" -> client.memberInfo();
            case "exit" -> client.exit();
            case "deposit" -> {

                if (parts.length == 2) {
                    client.account.deposit(Integer.parseInt(parts[1]));
                } else {
                    throw new IllegalArgumentException("Invalid number of arguments for deposit");
                }
            }
            case "addInterest" -> {
                if (parts.length == 2) {
                    client.account.addInterest(Integer.parseInt(parts[1]));
                } else {
                    throw new IllegalArgumentException("Invalid number of arguments for addInterest");
                }
            }
            case "checkTxStatus" -> {
                if (parts.length == 2) {
                    client.checkTxStatus(parts[1]);
                } else {
                    throw new IllegalArgumentException("Invalid number of arguments for checkTxStatus");
                }
            }
            case "sleep" -> {
                if (parts.length == 2) {
                    client.sleep(Integer.parseInt(parts[1]));
                } else {
                    throw new IllegalArgumentException("Invalid number of arguments for sleep");
                }
            }
            default -> throw new IllegalArgumentException("Unknown command: " + command);
        }


    }

    @Override
    public String toString() {
        return "Transaction{cmd=" + command +", id=" + id + "}";   
    }

}