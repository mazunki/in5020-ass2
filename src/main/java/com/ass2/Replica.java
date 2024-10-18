package com.ass2;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Replica {
    private final Account account;
    private final String replicaId;

    // List to store executed transactions
    private final List<Transaction> executed = new ArrayList<>();

    public Replica(String accountName) {
        this.account = new Account(accountName);
        this.replicaId = accountName + "-" + UUID.randomUUID().toString().substring(0, 6);
    }

    public String getId() {
        return replicaId;
    }

    // Process the entire transaction instead of just a command string
    public void processTransaction(Transaction transaction) {
        String[] args = transaction.commandArgs();
        String cmd = transaction.commandName();

        System.out.println("EXECUTED: " + transaction.getCommand());

        switch (cmd) {
            case "deposit" -> {
                if (args.length == 1) {
                    deposit(new BigDecimal(args[0]));
                    executed.add(transaction);  // Log transaction in executed list
                } else {
                    System.out.println("Invalid deposit command.");
                }
            }
            case "addInterest" -> {
                if (args.length == 1) {
                    addInterest(Integer.parseInt(args[0]));
                    executed.add(transaction);  // Log transaction in executed list
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
        System.out.println("Deposit complete. Current balance: " + account.getBalance());
    }

    public void addInterest(int percent) {
        account.addInterest(percent);
        System.out.println("Interest added. Current balance: " + account.getBalance());
    }

    public BigDecimal getBalance() {
        System.out.println("Current balance: " + account.getBalance());
        return account.getBalance();
    }

    public void getHistory() {
        System.out.println("Transaction History:");
        for (Transaction entry : executed) {
            System.out.println(entry);
        }
    }

    public void cleanHistory() {
        executed.clear();
        System.out.println("Transaction history cleaned.");
    }

    // Return the executed transactions for further usage
    public List<Transaction> getExecutedTransactions() {
        return executed;
    }
}

