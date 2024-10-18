package com.ass2;

import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.io.*;
import java.net.InetAddress;
import java.util.*;

public class Client {

    private SpreadConnection connection;
    private String accountName;
    private List<Transaction> executedList;
    private Collection<Transaction> outstandingCollection;
    private int orderCounter;
    private int outstandingCounter;
    private Listener listener;

    public Client(String serverAddress, String accountName, int numberOfReplicas) {
        this.accountName = accountName;
        this.executedList = new ArrayList<>();
        this.outstandingCollection = new LinkedList<>();
        this.orderCounter = 0;
        this.outstandingCounter = 0;
        this.connection = new SpreadConnection();

        // Establish connection to the Spread server
        try {
            connection.connect(InetAddress.getByName(serverAddress), 0, "client" + new Random().nextInt(1000), false, true);
            listener = new Listener(this);
            connection.add(listener);

            // Join the group (accountName)
            SpreadGroup group = new SpreadGroup();
            group.join(connection, accountName);

            // Wait for other replicas (based on numberOfReplicas)
            waitForReplicas(numberOfReplicas);

        } catch (SpreadException | IOException e) {
            e.printStackTrace();
        }
    }

    private void waitForReplicas(int numberOfReplicas) {
        System.out.println("Waiting for " + numberOfReplicas + " replicas to join...");
        // Implementation to wait until the required number of replicas have joined
    }

    public void addPending(Transaction transaction) {
        outstandingCollection.add(transaction);
        System.out.println("Added to outstanding: " + transaction);
    }

    public void executeCommand(String commandLine) {
        String[] parts = commandLine.split(" ");
        String command = parts[0];

        switch (command) {
            case "getQuickBalance":
                getQuickBalance();
                break;
            case "getSyncedBalance":
                getSyncedBalance();
                break;
            case "deposit":
                double amount = Double.parseDouble(parts[1]);
                deposit(amount);
                break;
            case "addInterest":
                double percent = Double.parseDouble(parts[1]);
                addInterest(percent);
                break;
            case "getHistory":
                getHistory();
                break;
            case "checkTxStatus":
                String transactionId = parts[1];
                checkTxStatus(transactionId);
                break;
            case "cleanHistory":
                cleanHistory();
                break;
            case "memberInfo":
                memberInfo();
                break;
            case "sleep":
                try {
                    Thread.sleep((long) (Double.parseDouble(parts[1]) * 1000));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case "exit":
                System.out.println("Exiting client...");
                disconnect();
                System.exit(0);
                break;
            default:
                System.out.println("Unknown command: " + command);
        }
    }

    // Command implementations

    private void getQuickBalance() {
        System.out.println("Quick balance: " + calculateBalance(false));
    }

    private void getSyncedBalance() {
        // Stronger consistency
        processOutstandingTransactions();
        System.out.println("Synced balance: " + calculateBalance(true));
    }

    private void deposit(double amount) {
        Transaction transaction = new Transaction("deposit " + amount, generateTransactionId());
        addOutstandingTransaction(transaction);
    }

    private void addInterest(double percent) {
        Transaction transaction = new Transaction("addInterest " + percent, generateTransactionId());
        addOutstandingTransaction(transaction);
    }

    private void getHistory() {
        System.out.println("Executed transactions:");
        for (int i = 0; i < executedList.size(); i++) {
            System.out.println((orderCounter - executedList.size() + i + 1) + ". " + executedList.get(i));
        }
        System.out.println("Outstanding transactions: " + outstandingCollection);
    }

    private void checkTxStatus(String transactionId) {
        boolean found = executedList.stream().anyMatch(tx -> tx.getUniqueId().equals(transactionId)) ||
                outstandingCollection.stream().anyMatch(tx -> tx.getUniqueId().equals(transactionId));
        System.out.println("Transaction status: " + (found ? "found" : "not found"));
    }

    private void cleanHistory() {
        executedList.clear();
        System.out.println("History cleaned.");
    }

    private void memberInfo() {
        // Handled in Listener.java
    }

    private void addOutstandingTransaction(Transaction transaction) {
        outstandingCollection.add(transaction);
        broadcastTransaction(transaction);
    }

    private void broadcastTransaction(Transaction transaction) {
        try {
            SpreadMessage message = new SpreadMessage();
            message.setReliable();
            message.addGroup(accountName);
            message.setObject(transaction);  // Broadcast the whole transaction object
            connection.multicast(message);
        } catch (SpreadException e) {
            e.printStackTrace();
        }
    }

    private String generateTransactionId() {
        return "client-" + outstandingCounter++;
    }

    private double calculateBalance(boolean synced) {
        // Apply all executed transactions
        double balance = 0.0;
        for (Transaction tx : executedList) {
            balance = applyTransaction(balance, tx);
        }

        if (!synced) {
            // For quick balance, apply outstanding transactions locally
            for (Transaction tx : outstandingCollection) {
                balance = applyTransaction(balance, tx);
            }
        }
        return balance;
    }

    private double applyTransaction(double balance, Transaction transaction) {
        if (transaction.getCommand().startsWith("deposit")) {
            double amount = Double.parseDouble(transaction.getCommand().split(" ")[1]);
            balance += amount;
        } else if (transaction.getCommand().startsWith("addInterest")) {
            double percent = Double.parseDouble(transaction.getCommand().split(" ")[1]);
            balance *= (1 + percent / 100);
        }
        return balance;
    }

    private void processOutstandingTransactions() {
        // Synchronize outstanding transactions
        while (!outstandingCollection.isEmpty()) {
            Transaction tx = ((LinkedList<Transaction>) outstandingCollection).poll();
            executedList.add(tx);
            orderCounter++;
        }
    }

    private void disconnect() {
        try {
            connection.disconnect();
        } catch (SpreadException e) {
            e.printStackTrace();
        }
    }

    public Listener getListener() {
        return listener;
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java Client <server_address> <account_name> <number_of_replicas> [<file_name>]");
            return;
        }

        String serverAddress = args[0];
        String accountName = args[1];
        int numberOfReplicas = Integer.parseInt(args[2]);

        Client client = new Client(serverAddress, accountName, numberOfReplicas);

        // Command-line or batch processing
        if (args.length == 4) {
            String fileName = args[3];
            try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
                String command;
                while ((command = reader.readLine()) != null) {
                    client.executeCommand(command);
                    Thread.sleep((long) (500 + Math.random() * 1000));
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("> ");
                String command = scanner.nextLine();
                client.executeCommand(command);
            }
        }
    }
}

