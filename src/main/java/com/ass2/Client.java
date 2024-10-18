package com.ass2;

import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {
	private SpreadConnection connection;
	private Replica replica;
	private SpreadGroup group;
	Listener listener;

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

	// Processes a command by determining if it's handled locally or needs to be broadcast
	private void processCommand(String commandName, String[] args) {
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
				Transaction transaction = new Transaction(commandName + " " + String.join(" ", args), this.replica.getId());
				this.broadcastTransaction(transaction);
			}
		}
	}

	// Method to get synchronized balance (placeholder)
	public void getSyncedBalance() {
		System.out.println("Synchronized balance: [To be implemented]");
	}

	// Display current group members
	public void memberInfo() {
		System.out.println("Current group members: " + group);
	}

	// Adds a pending transaction (called when receiving a transaction)
	public void addPending(Transaction transaction) {
		System.out.println("Processing transaction: " + transaction);
		replica.processCommand(transaction.getCommand());  // Process the transaction locally
	}

	// Broadcast a transaction to all replicas in the group
	public void broadcastTransaction(Transaction transaction) {
		SpreadMessage message = new SpreadMessage();
		message.setFifo();  // Ensure FIFO ordering of messages
		message.addGroup(group);  // Send to all members in the group

		try {
			message.setObject(transaction);  // Serialize the transaction into the message
		} catch (SpreadException e) {
			e.printStackTrace();
		}

		try {
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
		System.exit(0);
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
	}
}

