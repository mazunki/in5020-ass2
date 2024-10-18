package com.ass2;

import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Client {
    private SpreadConnection connection;
    private Replica replica;
    private SpreadGroup group;

    // Client constructor: each Client creates its own Replica
    public Client(InetAddress serverAddress, String accountName, String filename) {
        // Create a unique replica for this client
        this.replica = new Replica(accountName, filename);

        try {
            // Establish connection to the Spread server
            connection = new SpreadConnection();
            connection.connect(serverAddress, 4803, this.replica.getId(), false, true);
            System.out.println("Connected to Spread server at: " + serverAddress);

            group = new SpreadGroup();
            group.join(connection, accountName);
            System.out.println("Joined group: " + accountName);

            connection.add(new Listener(this));
        } catch (SpreadException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    // Process and broadcast transactions
    public void addPending(Transaction transaction) {
        System.out.println("Processing transaction: " + transaction);
        broadcastTransaction(transaction);
        replica.execute(transaction);  // Execute the transaction locally
    }

    // Broadcast transaction to other replicas in the group
    public void broadcastTransaction(Transaction transaction) {
        SpreadMessage message = new SpreadMessage();
        message.setFifo(); // Ensure FIFO ordering of messages
        message.addGroup(group);
        try {
            message.setObject(transaction);
        } catch (SpreadException e) {
            e.printStackTrace();
        }

        try {
            connection.multicast(message); // Send the transaction to the group
            System.out.println("Broadcasted transaction: " + transaction);
        } catch (SpreadException e) {
            e.printStackTrace();
        }
    }

    // Command: Get the quick balance of the local replica
    public void getQuickBalance() {
        replica.getBalance();
    }

    // Command: Placeholder for getting synchronized balance
    public void getSyncedBalance() {
        System.out.println("Synchronized balance: [To be implemented]");
    }

    // Command: Placeholder for transaction history
    public void getHistory() {
        System.out.println("Transaction history: [To be implemented]");
    }

    // Command: Placeholder for cleaning history
    public void cleanHistory() {
        System.out.println("History cleaned.");
    }

    // Command: Display current group members
    public void memberInfo() {
        System.out.println("Current group members: " + group);
    }

    // Command: Check transaction status by ID
    public void checkTxStatus(String transactionId) {
        System.out.println("Transaction status: [To be implemented for " + transactionId + "]");
    }

    // Sleep for a given duration
    public void sleep(int duration) {
        try {
            Thread.sleep(duration * 1000L); // Sleep in seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Slept for " + duration + " seconds.");
    }

    // Exit the client, leave the group, and disconnect
    public void exit() {
        try {
            group.leave();  // Leave the group
            connection.disconnect();  // Disconnect from the server
            System.out.println("Client exited.");
        } catch (SpreadException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    // Main method to run the Client
    public static void main(String[] args) throws UnknownHostException {
        if (args.length < 3 || args.length > 4) {
            System.out.println("Usage: java Client <server> <account> <no_replicas> [filename]");
            return;
        }

        String serverAddress = args[0];  // Server address
        String accountName = args[1];    // Account name for the replica group
        int noOfReplicas = Integer.parseInt(args[2]);  // Number of replicas
        String filename = (args.length == 4) ? args[3] : null;  // Optional filename for batch mode

        InetAddress address = InetAddress.getByName(serverAddress);

        // Create each Client and its own Replica for the given number of replicas
        for (int i = 0; i < noOfReplicas; i++) {
            new Client(address, accountName, filename);
        }
    }
}

