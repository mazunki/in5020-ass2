package com.ass2;

import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Scanner;

public class Client {
	private SpreadConnection connection;
	private Replica replica;
	private SpreadGroup group;
	Listener listener;

	public Client(InetAddress serverAddress, String accountName, String filename) {
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

			if (filename != null) {
				loadCommandsFromFile(filename);
			}

		} catch (SpreadException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void loadCommandsFromFile(String filename) {
		try (Scanner scanner = new Scanner(new java.io.File(filename))) {
			while (scanner.hasNextLine()) {
				this.parseInputLine(scanner.nextLine());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void parseInputLine(String line) {
		String[] parts = line.split(" ");
		String cmdName = parts[0];
		String[] args = Arrays.copyOfRange(parts, 1, parts.length);

		this.processCommand(cmdName, args);
	}

	private void processCommand(String commandName, String[] args) {
		switch (commandName) {
			case "getSyncedBalance" -> this.getSyncedBalance();
			case "memberInfo" -> this.memberInfo();
			case "exit" -> this.exit();
			case "sleep" -> {
				if (args.length == 1) this.sleep(Integer.parseInt(args[0]));
				else System.out.println("Invalid sleep command.");
			}
			default -> replica.processCommand(commandName, args);
		}
	}

	public void getSyncedBalance() {
		System.out.println("Synchronized balance: [To be implemented]");
	}

	public void memberInfo() {
		System.out.println("Current group members: " + group);
	}

	public void addPending(Transaction transaction) {
		System.out.println("Processing transaction: " + transaction);
		broadcastTransaction(transaction);
		replica.execute(transaction);  // Execute the transaction locally
	}

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

	public void sleep(int duration) {
		try {
			Thread.sleep(duration * 1000L); // Sleep in seconds
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		System.out.println("Slept for " + duration + " seconds.");
	}

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

		for (int i = 0; i < noOfReplicas; i++) {
			new Client(address, accountName, filename);
		}
	}
}

