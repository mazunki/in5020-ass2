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

    // Process commands sent to this replica
    public void processCommand(String command) {
        String[] parts = command.split(" ");
        String cmd = parts[0];
        String[] args = parts.length > 1 ? parts[1].split(" ") : new String[0];

        switch (cmd) {
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

    public void deposit(BigDecimal amount) {
        account.deposit(amount);
        transactionHistory.add("Deposit: " + amount);  // Log transaction
        System.out.println("Deposit complete. Current balance: " + account.getBalance());
    }

    public void addInterest(int percent) {
        account.addInterest(percent);
        transactionHistory.add("Interest: " + percent + "%");  // Log transaction
        System.out.println("Interest added. Current balance: " + account.getBalance());
    }

    public BigDecimal getBalance() {
        System.out.println("Current balance: " + account.getBalance());
        return account.getBalance();
    }

    public void getHistory() {
        System.out.println("Transaction History:");
        for (String entry : transactionHistory) {
            System.out.println(entry);
        }
    }

    public void cleanHistory() {
        transactionHistory.clear();
        System.out.println("Transaction history cleaned.");
    }
}

