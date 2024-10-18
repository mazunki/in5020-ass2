package com.ass2;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Replica {
    private final Account account;
    private final String replicaId;
    private final List<String> transactionHistory = new ArrayList<>(); // Added for tracking transaction history

    public Replica(String accountName) {
        this(accountName, null);  // For compatibility when no filename is provided
    }

    public Replica(String accountName, String filename) {
        this.account = new Account(accountName);
        this.replicaId = accountName + "-" + System.currentTimeMillis();  // Unique replica ID based on time

        if (filename != null) {
            System.out.println("Loading commands from file: " + filename);
            loadCommandsFromFile(filename);
        }
    }

    // Unique identifier for the replica
    public String getId() {
        return replicaId;
    }

    // Process a deposit
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

    // Get synchronized balance (placeholder for now)
    public void getSyncedBalance() {
        // Implementation needed based on outstanding transactions.
        System.out.println("Synchronized balance: " + getBalance());
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

    // Sleep for a given duration
    public void sleep(int duration) {
        try {
            Thread.sleep(duration * 1000L);
            System.out.println("Slept for " + duration + " seconds.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void execute(Transaction transaction) {
        transaction.execute(this);
    }

    // Load commands from a file and process them (if batch mode)
    private void loadCommandsFromFile(String filename) {
        try (Scanner scanner = new Scanner(new java.io.File(filename))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                processCommand(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Process a command manually from a file or user input (for batch mode)
    private void processCommand(String command) {
        String[] parts = command.split(" ");
        switch (parts[0]) {
            case "deposit" -> {
                if (parts.length == 2) {
                    deposit(new BigDecimal(parts[1]));
                } else {
                    System.out.println("Invalid deposit command.");
                }
            }
            case "interest" -> {
                if (parts.length == 2) {
                    addInterest(Integer.parseInt(parts[1]));
                } else {
                    System.out.println("Invalid interest command.");
                }
            }
            case "balance", "getQuickBalance" -> getBalance();
            case "getSyncedBalance" -> getSyncedBalance();
            case "getHistory" -> getHistory();
            case "cleanHistory" -> cleanHistory();
            case "sleep" -> {
                if (parts.length == 2) {
                    sleep(Integer.parseInt(parts[1]));
                } else {
                    System.out.println("Invalid sleep command.");
                }
            }
            default -> System.out.println("Unknown command.");
        }
    }
}

