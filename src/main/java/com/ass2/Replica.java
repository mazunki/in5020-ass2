package com.ass2;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Replica {
    private final Account account;
    private final String replicaId;
    private final List<String> transactionHistory = new ArrayList<>();

    public Replica(String accountName) {
        this.account = new Account(accountName);
        this.replicaId = accountName + "-" + UUID.randomUUID().toString().substring(0, 6);
    }

    public String getId() {
        return replicaId;
    }

    public void deposit(BigDecimal amount) {
        account.deposit(amount);
        transactionHistory.add("Deposit: " + amount);  // Log transaction
        System.out.println("Deposit complete. Current balance: " + account.getBalance());
    }

    // Process adding interest
    public void addInterest(int percent) {
        account.addInterest(percent);
        transactionHistory.add("Interest: " + percent + "%");  // Log transaction
        System.out.println("Interest added. Current balance: " + account.getBalance());
    }

    // Get current balance
    public BigDecimal getBalance() {
        System.out.println("Current balance: " + account.getBalance());
        return account.getBalance();
    }

    // Return the transaction history
    public void getHistory() {
        System.out.println("Transaction History:");
        for (String entry : transactionHistory) {
            System.out.println(entry);
        }
    }

    // Clear the transaction history
    public void cleanHistory() {
        transactionHistory.clear();
        System.out.println("Transaction history cleaned.");
    }

    // Execute a transaction (called by the Client)
    public void execute(Transaction transaction) {
        transaction.execute(this);
    }

    // Process a command manually from a file or user input (for batch mode)
    public void processCommand(String commandName, String[] args) {
        switch (commandName) {
            case "deposit" -> {
                if (args.length == 1) {
                    deposit(new BigDecimal(args[0]));
                } else {
                    System.out.println("Invalid deposit command.");
                }
            }
            case "addInterest" -> {
                if (args.length == 1) {
                    addInterest(Integer.parseInt(args[0]));
                } else {
                    System.out.println("Invalid interest command.");
                }
            }
            case "getQuickBalance" -> getBalance();
            case "getHistory" -> getHistory();
            case "cleanHistory" -> cleanHistory();
            default -> System.out.println("Unknown command.");
        }
    }
}

