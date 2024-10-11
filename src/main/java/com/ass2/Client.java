package com.ass2;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;

public class Client implements ClientInterface {
	private static final String ADDRESS = "127.0.0.1"; // ifi: "129.240.65.59"
	private static final int PORT = 4803;

	private final Collection<Transaction> outstanding;
	private final List<Transaction> executed;

	private int order_counter;
	private int outstanding_counter;

	private final String id;

	public Client() {
		this.id = String.valueOf((new Random()).nextInt());

		this.outstanding = new HashSet<>();
		this.executed = new ArrayList<>();
		this.order_counter = 0;
		this.outstanding_counter = 0;
	}

	public static void main(String[] args) throws InterruptedException {
		Client client = new Client();

		SpreadConnection connection = new SpreadConnection();
		Listener listener = new Listener();
		
		try {
			connection.add(listener);
			connection.connect(InetAddress.getByName(Client.ADDRESS), Client.PORT, client.id, false, true);

			SpreadGroup group = new SpreadGroup();
			group.join(connection, "group");
			SpreadMessage message = new SpreadMessage();
			message.addGroup(group);
			message.setFifo();
			message.setObject("client name : " + client.id);
			connection.multicast(message);

		} catch (SpreadException | UnknownHostException e) {
			throw new RuntimeException(e);
		}

		// System.out.println("Hello world!");
		Thread.sleep(100_000_000);
	}

	public BigDecimal getQuickBalance(){
		throw new UnsupportedOperationException();
	}

	public BigDecimal getSyncedBalance(){
		throw new UnsupportedOperationException();
	}

	public void deposit(BigDecimal amount){
		throw new UnsupportedOperationException();
	}

	public void deposit(int amount){
		this.deposit(new BigDecimal(amount));
	}
	public void deposit(float amount){
		this.deposit(new BigDecimal(amount));
	}
	
	public int addInterest(int interest){
		throw new UnsupportedOperationException();
	}

	public List<Transaction> getHistory(){
		throw new UnsupportedOperationException();
	}

	public boolean checkTxStatus(String transactionId){
		throw new UnsupportedOperationException();
	}

	public void cleanHistory(){
		throw new UnsupportedOperationException();
	}

	public Collection<String> memberInfo(){
		throw new UnsupportedOperationException();
	}

	public void sleep(int seconds){
		try {
			Thread.sleep(seconds * 1000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public void exit(){
		throw new UnsupportedOperationException();
	}
}
