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
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;

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

	private volatile boolean waitingForSync = false;

	public Client(InetAddress serverAddress, String accountName, String replicaName) {
		this.replica = new Replica(accountName, replicaName);
		try {
			connection = new SpreadConnection();
			connection.connect(serverAddress, 4803, this.replica.getId(), false, true);
			debug("Connected to Spread server at: " + serverAddress);

			group = new SpreadGroup();
			group.join(connection, accountName);
			debug("Joined group: " + accountName);

			this.listener = new Listener(this);
			connection.add(listener);

			// Start periodic broadcasting of transactions
			scheduler.scheduleAtFixedRate(this::broadcastOutstandingTransactions, 10, 10, TimeUnit.SECONDS);

		} catch (SpreadException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	public Client(InetAddress serverAddress, String accountName) {
		this(serverAddress, accountName, UUID.randomUUID().toString().substring(0, 6));
	}

	public void loadCommandsFromFile(String filename) {
		debug("reading from " + filename);
		try (Scanner scanner = new Scanner(new java.io.File(filename))) {
			while (scanner.hasNextLine()) {
				this.parseInputLine(scanner.nextLine());
				try {
					Thread.sleep(ThreadLocalRandom.current().nextInt(500, 1501));
				} catch (InterruptedException e) {
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void loadCommandsFromInteractive() {
		System.out.println("replicated system. please enter your commands (type 'exit' to quit):");

		try (Scanner scanner = new Scanner(System.in)) {
			while (true) {
				System.out.print("> ");
				String line = scanner.nextLine();
				if (line.equalsIgnoreCase("exit")) {
					break;
				}
				this.parseInputLine(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	// Parsing and processing each line from the input file
	private void parseInputLine(String line) {
		if (line.isBlank()) return;
		if (line.startsWith("#")) return;

		String[] contents = line.split("#");
		String[] parts = contents[0].trim().split(" ");

		String cmdName = parts[0];
		String[] args = Arrays.copyOfRange(parts, 1, parts.length);

		this.processCommand(cmdName, args);
	}

	public Replica getReplica() {
		return this.replica;
	}

	public void processCommand(String commandName, String[] args) {
		switch (commandName.toLowerCase()) {
			case "getsyncedbalance", "getsync" -> this.getSyncedBalance();
			case "memberinfo", "members" -> this.memberInfo();
			case "exit", "quit" -> this.exit();
			case "gethistory", "history" -> this.getHistory();
			case "sleep" -> {
				if (args.length == 1) this.sleep(Integer.parseInt(args[0]));
				else System.err.println("Invalid sleep command.");
			}
			default -> {
				Transaction transaction = this.replica.makeTransaction(commandName, args, this.outstanding_counter);
				if (transaction == null) return;
				this.outstanding_counter++;
				this.addPending(transaction);
			}
		}
	}

	public void addPending(Transaction transaction) {
		debug("added to outstanding: " + transaction);
		outstanding.add(transaction);
	}

	public void getHistory() {
		StringJoiner sj = new StringJoiner("; ");
		for (Transaction entry : this.replica.getExecutedTransactions()) {
			sj.add(entry.toString());
		}
		// we think it's a bit weird that the history includes things that aren't actually applied yet
		for (Transaction entry : this.outstanding) {
			sj.add(entry.toString());
		}
		int firstOrderCounter = this.replica.getOrderCounter() - this.replica.getExecutedTransactions().size() + 1;
		debug("transaction history: (#" + firstOrderCounter +  ") " + sj);
		System.out.println(sj);
	}

	public void broadcastExecutedTransactions() {
		for (Transaction transaction : this.replica.getExecutedTransactions()) {
			this.broadcastTransaction(transaction);
		}
	}

	public void requestSyncBalance() {
		Transaction transaction = this.replica.makeTransaction("getSyncedBalance", new String[0], this.outstanding_counter);
		this.outstanding_counter++;
		this.addPending(transaction);
	}

	public void completeSync() {
		this.waitingForSync = false;
	}

	public synchronized BigDecimal getOptimizedSyncedBalance() {
		waitingForSync = true;
		this.requestSyncBalance();

		while (waitingForSync) {
			Thread.yield();
		}

		BigDecimal value = this.replica.getQuickBalance();
		waitingForSync = false;
		debug("optimizedSyncedBalance: " + value);
		return value;
	}

	public BigDecimal getNaiveSyncedBalance() {

		while (this.outstanding.size() != 0) {
			Thread.yield();
		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		BigDecimal value = this.replica.getQuickBalance();
		debug("naiveSyncedBalance: " + value);

		return value;
	}

	public BigDecimal getSyncedBalance() {
		debug("getting synced balance");

		BigDecimal value;
		// value = this.getNaiveSyncedBalance();
		value = this.getOptimizedSyncedBalance();

		System.out.println(value);
		return value;
	}


	// Display current group members
	public void memberInfo() {
		debug("memberInfo: " + group);
		System.out.println(group);
	}

	// Broadcast all outstanding transactions
	private void broadcastOutstandingTransactions() {
		for (Transaction transaction : outstanding) {
			this.broadcastTransaction(transaction);
		}
		outstanding.clear();
	}

	// Broadcast a single transaction
	public void broadcastTransaction(Transaction transaction) {
		SpreadMessage message = new SpreadMessage();
		message.setFifo();
		message.addGroup(group);

		try {
			message.setObject(transaction);
			connection.multicast(message);
			debug("broadcasted transaction: " + transaction);
		} catch (SpreadException e) {
			e.printStackTrace();
		}
	}

	public void sleep(int duration) {
		debug("sleeping for " + duration + "s");
		try {
			Thread.sleep(duration * 1000L);
		} catch (InterruptedException e) {
		}
		debug("woke up from sleep");
	}

	public void exit() {
		debug("exiting...");
		try {
			group.leave();
			this.connection.remove(this.listener);
			connection.disconnect();
		} catch (SpreadException e) {
			e.printStackTrace();
		}
		this.scheduler.shutdown();

		try {
			if (!this.scheduler.awaitTermination(15, TimeUnit.SECONDS)) {
				scheduler.shutdownNow(); // Force shutdown after waiting period
				if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
					debug("scheduler did not terminate properly.");
				}
			}
		} catch (InterruptedException e) {
			debug("couldn't properly send all outstanding transactions");
			scheduler.shutdownNow();
		}
		this.scheduler.shutdownNow();

		debug("we gone");
	}

	public String toString() {
		return "Client<" + this.replica.getId() + ">";
	}

	public void debug(String s) {
		System.err.println("Client[" + this.getReplica().getId() + "]: " + s);
	}

	public static void main(String[] args) throws UnknownHostException {
		if (args.length != 2) {
			System.out.println("Usage: java Client <server> <account>");
			return;
		}

		String serverAddress = args[0];  // Server address
		String accountName = args[1];    // Account name for the replica group

		InetAddress address = InetAddress.getByName(serverAddress);

		Client client = new Client(address, accountName);

		// Run interactive mode
		client.loadCommandsFromInteractive();

		client.scheduler.shutdown();
		try {
			if (!client.scheduler.awaitTermination(15, TimeUnit.SECONDS)) {
				client.scheduler.shutdownNow();
			}
		} catch (InterruptedException e) {
		};

		// Print final balance after interactive mode ends
		System.out.println("Final balance: " + client.getSyncedBalance());
	}


}

