package com.ass2;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Scanner; 
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;


public final class Client implements ClientInterface {
    private static final String ADDRESS = "127.0.0.1"; // ifi: "129.240.65.59"
    private static final int PORT = 4803;

    private final SpreadConnection connection = new SpreadConnection();
    private final Listener listener;

    private final Collection<Transaction> outstanding;
    private final List<Transaction> executed;

    private int order_counter;
    private int outstanding_counter;

    private final String id;
    private SpreadGroup group;
    private ScheduledExecutorService scheduler;

    Account account;

    // Constructor 1: Common initalization
    public Client(String spreadAddress, String accountName, int numberOfReplicas) {
        this.id = String.valueOf((new Random()).nextInt());

        this.outstanding = new HashSet<>();
        this.executed = new ArrayList<>();
        this.order_counter = 0;
        this.outstanding_counter = 0;

		this.listener = new Listener(this);
        this.account = new Account(accountName);

        this.joinGroup(accountName);

        // Schedule periodic broadcasting of outstanding transactions
        this.scheduleBroadcasting();
    }

    // Constructor 2: Client using a file for batch processing
    public Client(String spreadAddress, String accountName, int numberOfReplicas, String fileName) {
        this(spreadAddress, accountName, numberOfReplicas);
        this.readFromFile(fileName);
    }

    // Constructor 3: Interactive mode
    public Client(String spreadAddress, String accountName, int numberOfReplicas, boolean interactive) {
        this(spreadAddress, accountName, numberOfReplicas);
        if (!interactive) return;
    
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                String line = scanner.nextLine();
                if (line.equals("exit")) {
                    this.exit();
                    break;
                }
				this.handleMessage(line);
            }
        }
    }

    // Handle reading commands from a file
    private void readFromFile(String fileName) {
        try {
            File myObj = new File(fileName);
            try (Scanner myReader = new Scanner(myObj)) {
                while (myReader.hasNextLine()) {
                    String data = myReader.nextLine();
					this.handleMessage(data);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + fileName);
        }
    }

	// Handle incoming messages
	public void handleMessage(String message) {
		System.out.println("Handling string: " + message);
		Transaction transaction = new Transaction(message, this.id + " " + this.outstanding_counter);
		this.outstanding_counter++;
		this.outstanding.add(transaction);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
        }
	}

    // Join Spread group
    private void joinGroup(String accountName) {
        try {
            this.connection.add(this.listener);
            this.connection.connect(InetAddress.getByName(Client.ADDRESS), Client.PORT, this.id, false, true);

            this.group = new SpreadGroup();
            group.join(this.connection, accountName);
        } catch (SpreadException | UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

	private void broadcastTransaction(Transaction transaction){
			try {
				SpreadMessage message = new SpreadMessage();
				message.addGroup(this.group);
				message.setFifo();

				message.setObject(transaction); // Send a copy of the outstanding collection
				message.setSelfDiscard(true);
				
				this.connection.multicast(message);
				System.err.println("Broadcasted transaction: " + transaction);

			} catch (SpreadException e) {
				System.err.println("Error broadcasting transaction: " + e.getMessage());
			}
	}

	private void broadcastAllTransactions(){
		System.err.println("Broadcasting all outstanding transactions: " + this.outstanding.size());

		HashSet<Transaction> toRemove = new HashSet<>();

		for (Transaction transaction : this.outstanding) {
			if (!transaction.getClientName().equals(this.id)) {
				// System.err.println("Skipping broadcast of foreign: " + transaction);
				continue;
			}

			System.err.println("Broadcasting transaction: " + transaction);
			this.broadcastTransaction(transaction);

			if (this.executed.contains(transaction)) {
				toRemove.add(transaction);
			}
		}
		this.outstanding.removeAll(toRemove);
	}

    // Schedule periodic broadcasting of outstanding transactions called every 10 seconds
    private void scheduleBroadcasting() {
		System.err.println("initiating scheduler");

        this.scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::broadcastAllTransactions, 0, 10, TimeUnit.SECONDS);  
    }
	// Add transaction to outstanding collection
	public synchronized void addPending(Transaction transaction){
		System.err.println("adding outstanding transaction: " + transaction);
		this.outstanding.add(transaction);
		this.outstanding_counter++;
	}

	// Execute event in outstanding collection  	
	private void processOutstanding(){
        // TODO: Sort outstanding transactions by vector id
        
        for (Transaction transaction : this.outstanding) {
            if (this.executed.contains(transaction)){
                continue;
            }
            
            transaction.execute(this);
            this.executed.add(transaction);
        }		
	}

    @Override
	public BigDecimal getQuickBalance() {
		BigDecimal balance = this.account.getBalance();
		System.out.println("Quick balance: " + balance);
		return balance;
	}

    @Override
	public BigDecimal getSyncedBalance(){
		// TODO: actually sync balance
		BigDecimal balance = this.account.getBalance();
		System.out.println("Synced balance (NYI lmao): " + balance);
		throw new UnsupportedOperationException();
	}

    @Override
	public List<Transaction> getHistory(){
		return this.executed;
	}

    @Override
	public boolean checkTxStatus(String transactionId){
		for (Transaction transaction : this.outstanding) {
			if (transaction.getId().equals(transactionId)) {
				return false;
			}
		}
		return true;
	}

    @Override
	public void cleanHistory(){
		this.executed.clear();
	}

    @Override
	public Collection<String> memberInfo(){
		throw new UnsupportedOperationException();
	}

    @Override
	public  void sleep(int seconds){
		try {
			Thread.sleep(seconds * 1000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

    @Override
	public  void exit(){
		try {
			this.connection.remove(this.listener);
			this.connection.disconnect();
			System.out.println(this.id + " disconnected");	
		} catch (SpreadException e) {
			throw new RuntimeException(e);

		}

		if (this.scheduler != null && !this.scheduler.isShutdown()) {
			this.scheduler.shutdown();
		}
	}

	public static void main(String[] args) throws InterruptedException {
		/*
			Usage of the Client class (args):
		 		Client spreadAddress accountName numberOfReplicas fileName
				Client spreadAddress accountName numberOfReplicas
		*/
		Client client;

		switch (args.length) {
			case 4 -> client = new Client(args[0], args[1], Integer.parseInt(args[2]), args[3]);
			case 3 -> client = new Client(args[0], args[1], Integer.parseInt(args[2]), true);
			default -> throw new IllegalArgumentException("Invalid number of arguments");
		}

		client.processOutstanding();
	}
}
