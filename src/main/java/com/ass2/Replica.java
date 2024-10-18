package com.ass2;

import java.math.BigDecimal;
import java.util.Scanner;

public class Replica {
    private final Account account;

    public Replica(String accountName) {
        // Initialize account with the given account name.
        this.account = new Account(accountName);
    }

    // Process the deposit transaction.
    public void deposit(BigDecimal amount) {
        account.deposit(amount);
        System.out.println("Deposit complete. Current balance: " + account.getBalance());
    }

    // Process adding interest.
    public void addInterest(int percent) {
        account.addInterest(percent);
        System.out.println("Interest added. Current balance: " + account.getBalance());
    }

    // Get the current balance of the account.
    public void getBalance() {
        System.out.println("Current balance: " + account.getBalance());
    }

    // Main method for initial testing.
    public static void main(String[] args) {
        // Initialize a simple Replica instance.
        Replica replica = new Replica("group01");

        // Using a Scanner to simulate simple command input from the user.
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to the replica client. Type commands: 'deposit', 'interest', 'balance', or 'exit'");

        while (true) {
            System.out.print("> ");
            String command = scanner.nextLine();
            String[] parts = command.split(" ");

            switch (parts[0]) {
                case "deposit" -> {
                    if (parts.length == 2) {
                        replica.deposit(new BigDecimal(parts[1]));
                    } else {
                        System.out.println("Usage: deposit <amount>");
                    }
                }
                case "interest" -> {
                    if (parts.length == 2) {
                        replica.addInterest(Integer.parseInt(parts[1]));
                    } else {
                        System.out.println("Usage: interest <percentage>");
                    }
                }
                case "balance" -> replica.getBalance();
                case "exit" -> {
                    System.out.println("Exiting...");
                    scanner.close();
                    return;
                }
                default -> System.out.println("Unknown command. Try 'deposit', 'interest', 'balance', or 'exit'.");
            }
        }
    }
}

