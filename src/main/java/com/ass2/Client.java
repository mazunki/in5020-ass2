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
            scheduler.scheduleAtFixedRate(this::broadcastOutstandingTransactions, 0, 10, TimeUnit.SECONDS);

        } catch (SpreadException e) {
            e.printStackTrace();
            System.exit(1);
        }
	}
    public Client(InetAddress serverAddress, String accountName) {
        this(serverAddress, accountName, UUID.randomUUID().toString().substring(0, 6));
    }

    // Method to load commands from a file
    public void loadCommandsFromFile(String filename) {
		debug("reading from " + filename);
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
			case "getHistory" -> this.getHistory();
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

    public void addPending(Transaction transaction) {
        debug("Added to pending: " + transaction);
        outstanding.add(transaction);
    }

    public void getHistory() {
		StringJoiner sj = new StringJoiner("; ");
        for (Transaction entry : this.replica.getExecutedTransactions()) {
            sj.add(entry.toString());
        }
        for (Transaction entry : this.outstanding) {
            sj.add(entry.toString());
        }
        debug("transaction history: " + sj);
		System.out.println(sj);
    }

	public BigDecimal getOptimizedSyncedBalance() {
		// TODO: fix this shit
		return this.getNaiveSyncedBalance();
	}

    public BigDecimal getNaiveSyncedBalance() {
		debug("syncing balance");

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
		BigDecimal value = this.getNaiveSyncedBalance();
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
            Thread.currentThread().interrupt();
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
			this.scheduler.awaitTermination(15, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			System.err.println(this + ": Couldn't properly send all outstanding transactions");
		}
		debug("we gone");
    }

	public String toString() {
		return "Client<" + this.replica.getId() + ">";
	}

	public void debug(String s) {
		System.err.println("Client[" + this.getReplica().getId() + "]: " + s);
	}

}

