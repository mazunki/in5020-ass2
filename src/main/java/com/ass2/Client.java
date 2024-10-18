package com.ass2;

import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Client {
    private SpreadConnection connection;
    private Replica replica;
    private SpreadGroup group;
    Listener listener;

    // Add this collection to store the outstanding transactions
    private Collection<Transaction> outstanding = new ArrayList<>();
	private Integer outstanding_counter = 1;

    // Add a scheduled executor for broadcasting every 10 seconds
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public Client(InetAddress serverAddress, String accountName) {
        this.replica = new Replica(accountName);

        try {
            connection = new SpreadConnection();
            connection.connect(serverAddress, 4803, this.replica.getId(), false, true);
            System.out.println("Connected to Spread server at: " + serverAddress);

            group = new SpreadGroup();
            group.join(connection, accountName);
            System.out.println("Joined group: " + accountName);

            this.listener = new Listener(this);
            connection.add(listener);

            // Start periodic broadcasting of transactions
            scheduler.scheduleAtFixedRate(this::broadcastOutstandingTransactions, 0, 10, TimeUnit.SECONDS);

        } catch (SpreadException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    // Method to load commands from a file
    public void loadCommandsFromFile(String filename) {
        try (Scanner scanner = new Scanner(new java.io.File(filename))) {
            while (scanner.hasNextLine()) {
                this.parseInputLine(scanner.nextLine());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Parsing and processing each line from the input file
    private void parseInputLine(String line) {
        String[] parts = line.split(" ");
        String cmdName = parts[0];
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        this.processCommand(cmdName, args);
    }

	public Replica getReplica() {
		return this.replica;
	}

    // Processes a command by determining if it's handled locally or needs to be broadcast
    public void processCommand(String commandName, String[] args) {


        switch (commandName) {
            case "getSyncedBalance" -> this.getSyncedBalance();
            case "memberInfo" -> this.memberInfo();
            case "exit" -> this.exit();
            case "sleep" -> {
                if (args.length == 1) this.sleep(Integer.parseInt(args[0]));
                else System.out.println("Invalid sleep command.");
            }
            default -> {
                // For commands like deposit or addInterest, we need to create a transaction
                Transaction transaction = new Transaction(commandName + " " + String.join(" ", args), this.replica.getId() + " "  + this.outstanding_counter);
				this.outstanding_counter++;
                this.addPending(transaction); // Add to outstanding collection instead of immediate broadcast
            }
        }
    }

    // Adds a pending transaction to the outstanding collection
    public void addPending(Transaction transaction) {
        System.out.println("Added to pending: " + transaction);
        outstanding.add(transaction);
    }

    // Method to get synchronized balance (placeholder)
    public BigDecimal getSyncedBalance() {
        System.out.println("Synchronized balance: [To be implemented]");
		this.sleep(10);
		return this.replica.getBalance();
    }

    // Display current group members
    public void memberInfo() {
        System.out.println("Current group members: " + group);
    }

    // Broadcast all outstanding transactions
    private void broadcastOutstandingTransactions() {
        for (Transaction transaction : outstanding) {
            this.broadcastTransaction(transaction);
        }
        outstanding.clear();  // Clear the collection after broadcasting
    }

    // Broadcast a single transaction
    public void broadcastTransaction(Transaction transaction) {
        SpreadMessage message = new SpreadMessage();
        message.setFifo();  // Ensure FIFO ordering of messages
        message.addGroup(group);  // Send to all members in the group

        try {
            message.setObject(transaction);  // Serialize the transaction into the message
            connection.multicast(message);  // Send the message to the group
            System.out.println("Broadcasted transaction: " + transaction);
        } catch (SpreadException e) {
            e.printStackTrace();
        }
    }

    public void sleep(int duration) {
        try {
            Thread.sleep(duration * 1000L);  // Sleep in seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Slept for " + duration + " seconds.");
    }

    // Exit the client and clean up
    public void exit() {
        try {
            group.leave();
            this.connection.remove(this.listener);
            connection.disconnect();
            System.out.println("Client exited.");
        } catch (SpreadException e) {
            e.printStackTrace();
        }
		this.scheduler.shutdown();
        try {
			this.scheduler.awaitTermination(15, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			System.err.println(this + ": Couldn't properly send all outstanding transactions");
		}
    }

	public String toString() {
		return "Client<" + this.replica.getId() + ">";
	}

    // Main method to run the Client and use multithreading for command loading
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
        Client[] clients = new Client[noOfReplicas];

        // Create a thread pool to run each Client in its own thread
        ExecutorService executorService = Executors.newFixedThreadPool(noOfReplicas);

        // Submit each client to the thread pool
        for (int i = 0; i < noOfReplicas; i++) {
            final int index = i;
            executorService.submit(() -> {
                clients[index] = new Client(address, accountName);
                if (filename != null) {
                    clients[index].loadCommandsFromFile(filename);
                }
            });
        }

        // Graceful shutdown of the ExecutorService
        executorService.shutdown();

		try {
			executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		}

		System.out.println("ponies");

		for (Client client : clients) {
			System.out.println(client.toString() + " final balance: " + client.getSyncedBalance());
		}
    }
}

