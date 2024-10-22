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
import java.io.File;

public class Client {
	private SpreadConnection connection;
	private Replica replica;
	private SpreadGroup group;
	Listener listener;

	private Collection<Transaction> outstanding = new ArrayList<>();
	private Integer outstanding_counter = 1;

	private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	/* atomic lock used by getOptimizedSyncBalance.
	 * locked by us, and unlocked by the listener
	 **/
	private volatile boolean waitingForSync = false;

	public Client(InetAddress serverAddress, String accountName, String replicaName) {
		this.replica = new Replica(accountName, replicaName);
		try {
			connection = new SpreadConnection();
			connection.connect(serverAddress, 4803, this.replica.getId(), false, true);
			debug("connected to Spread server at: " + serverAddress);

			group = new SpreadGroup();
			group.join(connection, accountName);
			debug("joined group: " + accountName);

			this.listener = new Listener(this);
			connection.add(listener);

			// Start periodic broadcasting of transactions after 10 seconds
			scheduler.scheduleAtFixedRate(this::broadcastOutstandingTransactions, 10, 10, TimeUnit.SECONDS);

		} catch (SpreadException e) {
			System.exit(1);
		}
	}
	public Client(InetAddress serverAddress, String accountName) {
		this(serverAddress, accountName, UUID.randomUUID().toString().substring(0, 6));
	}

	public Replica getReplica() {
		return this.replica;
	}

	public void loadCommandsFromFile(String filename) {
		debug("reading from " + filename);

		File f = new File(filename);

		try (Scanner scanner = new Scanner(f)) {
			while (scanner.hasNextLine()) {
				this.parseInputLine(scanner.nextLine());
				try {
					Thread.sleep(ThreadLocalRandom.current().nextInt(500, 1501));
				} catch (InterruptedException e) {
				}
			}
		} catch (Exception e) {
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
		}
	}


	/* line is a command line of the form "command arg1 arg2 ..." as
	 * they are required by Transactions
	 *
     * we ignore empty lines and comment lines (starting with #)
	 * we also ignore any arguments after a # in a line
	 **/
	private void parseInputLine(String line) {
		if (line.isBlank()) return;
		if (line.startsWith("#")) return;

		String[] contents = line.split("#");
		String[] parts = contents[0].trim().split(" ");

		String cmdName = parts[0];
		String[] args = Arrays.copyOfRange(parts, 1, parts.length);

		this.processCommand(cmdName, args);
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
				/*
				 * if the command is not a Client-specific command, we assume it's a transaction.
				 * meaning we should
				 * treat it as an outstanding transaction. this also means it should be replicated by other clients
				 */
				Transaction transaction = this.replica.makeTransaction(commandName, args, this.outstanding_counter);
				if (transaction == null) return;
				this.outstanding_counter++;
				this.addPending(transaction);
			}
		}
	}

	/*
	 * Adds a transaction to the outstanding list. This means two things:
	 	- we need to broadcast it to the group
		- we need to eventually execute it
	 **/
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

	/* special case for getSyncedBalance, which is not really a transaction, even if we treat it as one here.
	 *
	 * it's really a request for the current balance, after being synchronized. we need to make a "fake"
	 * transaction and wait for it to return from the FIFO queue of our listener
	 **/
	public void requestSyncBalance() {
		Transaction transaction = this.replica.makeTransaction("getSyncedBalance", new String[0], this.outstanding_counter);
		this.outstanding_counter++;
		this.addPending(transaction);
	}

	/* called by the listener to indicate that the sync balance has
	 * been received, and that we're now ready to continue our main thread
	 * pool, and can continue processing requests
	 */
	public void completeSync() {
		this.waitingForSync = false;
	}

	// the good implementation.
	// this will spin on our main thread until the listener unlocks our waitingForSync atomic lock
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

	// the dumb dumb implementation. falls into deadlocks, and we never see our thread actually complete
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

	// a wrapper for the two different implementations of getSyncedBalance
	public BigDecimal getSyncedBalance() {
		debug("getting synced balance");

		BigDecimal value;
		value = this.getNaiveSyncedBalance();
		//value = this.getOptimizedSyncedBalance();

		System.out.println(value);
		return value;
	}


	public void memberInfo() {
		debug("memberInfo: " + group);
		System.out.println(group);
	}

	private void broadcastOutstandingTransactions() {
		for (Transaction transaction : this.outstanding) {
			this.broadcastTransaction(transaction);
		}
		
		/*
		 * it should be safe to clear all these tasks, as they
		 * should be in the FIFO queue.
		 
		 * we haven't observed any concurrency issues with this, but it's possible that clearing
		 * the list when the listener is processing new transactions could cause issues
		 */
		this.outstanding.clear();
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


	/*
	 * cleans up the client, reverting all our connectiosn to the spread server.
	 
	 this means we do a few things:
	  - leave the spread group
	  - tell the connection to remove the listener
	  - disconnct ourselves
	  - shut down the broadcast scheduler
	**/
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

	/*
	 * Runs a single client in interactive mode
	 * 
	 * Usage: java Client <server> <account>
	 **/
	public static void main(String[] args) throws UnknownHostException {
		if (args.length != 2) {
			System.out.println("Usage: java Client <server> <account>");
			return;
		}

		String serverAddress = args[0];
		String accountName = args[1];

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

