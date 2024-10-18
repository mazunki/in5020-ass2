package com.ass2;

import spread.AdvancedMessageListener;
import spread.SpreadException;
import spread.SpreadMessage;

/*
 * The Listener class is responsible for listening to messages
 * from the Spread server and processing them
 * 
 * Each client makes a listener which is added to the connection,
 * and runs on its own thread
 **/
public class Listener implements AdvancedMessageListener {
	Client client;

	public Listener(Client client) {
		this.client = client;
	}

    @Override
	public void regularMessageReceived(SpreadMessage message) {
		try {
			if (message.getObject() instanceof Transaction transaction) {

				// the client already knew about this event, let's ignore it
				if (this.client.getReplica().checkTxStatusImpl(transaction.getId())) {
					return;
				}
				
				transaction.process(this.client); 
			}
		} catch (SpreadException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void membershipMessageReceived(SpreadMessage spreadMessage) {

		if (spreadMessage.getMembershipInfo().isCausedByJoin()) {
			this.client.debug("member joined!");

			/* 
			 * Tells the new clients about the current state of the system
			 *
			 * We have to keep in mind all clients will do this broadcasting,
			 * so new clients need to filter potential redundancy
			 **/
			this.client.broadcastExecutedTransactions();
		} else if (spreadMessage.getMembershipInfo().isCausedByLeave()) {
			this.client.debug("member leaved!");
		}
	}
}

