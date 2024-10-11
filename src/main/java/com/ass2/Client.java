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
    private final Listener listener = new Listener();

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

        if (!interactive) {
            return;
        }

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

	// Execute tast in outstanding collection and update all lists
	private void processOutstanding(){

		// TODO: Sort outstanding transactions by vector id
		for (Transaction transaction : this.outstanding) {
			transaction.execute(this);
			this.executed.add(transaction);
			this.outstanding.remove(transaction);
		}
	}

	// Handle incoming messages
	public void handleMessage(String message) {
		System.out.println("Received message: " + message);
		Transaction transaction = new Transaction(message, this.id + " " + this.order_counter);
		this.order_counter++;
		transaction.execute(this);
		this.executed.add(transaction);
	}

    // Join Spread group
    private void joinGroup(String accountName) {
        try {
            this.connection.add(this.listener);
            this.connection.connect(InetAddress.getByName(Client.ADDRESS), Client.PORT, this.id, false, true);

            this.group = new SpreadGroup();
            group.join(this.connection, accountName);

            SpreadMessage message = new SpreadMessage();
            message.addGroup(group);
            message.setFifo();
            message.setObject("Client name: " + this.id);

            this.connection.multicast(message);
        } catch (SpreadException | UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

	private void broadcastTransaction(Transaction transaction){
		 if (!this.outstanding.isEmpty()) {
			try {
				SpreadMessage message = new SpreadMessage();
				message.addGroup(this.group);
				message.setFifo();

				message.setObject(transaction); // Send a copy of the outstanding collection

				this.connection.multicast(message);
				System.out.println("Broadcasting outstanding transactions...");
			} catch (SpreadException e) {
				System.err.println("Error broadcasting transaction: " + e.getMessage());
			}
		}
	}

	private void broadcastAllTransactions(){
		for (Transaction transaction : this.outstanding) {
			this.broadcastTransaction(transaction);
			this.outstanding.remove(transaction);
		}
	}

    // Schedule periodic broadcasting of outstanding transactions called every 10 seconds
    private void scheduleBroadcasting() {
        this.scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::broadcastAllTransactions, 0, 10, TimeUnit.SECONDS);  
    }

	public BigDecimal getQuickBalance() {
		BigDecimal balance = this.account.getBalance();
		System.out.println("Quick balance: " + balance);
		return balance;
	}

	public BigDecimal getSyncedBalance(){
		// TODO: actually sync balance
		BigDecimal balance = this.account.getBalance();
		System.out.println("Synced balance (NYI lmao): " + balance);
		throw new UnsupportedOperationException();
	}

	public void deposit(BigDecimal amount){
		this.account.deposit(amount);
	}

	public void deposit(int amount){
		this.deposit(new BigDecimal(amount));
	}
	public void deposit(float amount){
		this.deposit(new BigDecimal(amount));
	}
	
	public void addInterest(int interest){
		this.account.addInterest(interest);
	}

	public List<Transaction> getHistory(){
		throw new UnsupportedOperationException();
	}

	public boolean checkTxStatus(String transactionId){
		for (Transaction transaction : this.outstanding) {
			if (transaction.getId().equals(transactionId)) {
				return false;
			}
		}
		return true;
	}

	public void cleanHistory(){
		this.executed.clear();
	}

	public Collection<String> memberInfo(){
		throw new UnsupportedOperationException();
	}

	public  void sleep(int seconds){
		try {
			Thread.sleep(seconds * 1000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public  void exit(){
		try {
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
