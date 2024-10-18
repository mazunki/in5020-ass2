package com.ass2;

import java.util.Arrays;

import spread.AdvancedMessageListener;
import spread.SpreadException;
import spread.SpreadMessage;

public class Listener implements AdvancedMessageListener {
	Client client;

	public Listener(Client client) {
		this.client = client;
	}

    @Override
	public void regularMessageReceived(SpreadMessage message) {
		try {
			if (message.getObject() instanceof Transaction transaction) {
				System.err.println("Received transaction: " + transaction);
				transaction.process(this.client.getReplica());
			}
		} catch (SpreadException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void membershipMessageReceived(SpreadMessage spreadMessage) {
		System.out.println(Arrays.toString(spreadMessage.getMembershipInfo().getMembers()));
		//TODO: Handle new clients
	}
}

