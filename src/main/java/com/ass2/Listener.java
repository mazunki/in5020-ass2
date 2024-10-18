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
				if (!this.client.getReplica().checkTxStatusImpl(transaction.getId())) {
					transaction.process(this.client);
				}
			}
		} catch (SpreadException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void membershipMessageReceived(SpreadMessage spreadMessage) {

		if (spreadMessage.getMembershipInfo().isCausedByJoin()) {
			this.client.debug("member joined!");
			this.client.broadcastExecutedTransactions();
		} else if (spreadMessage.getMembershipInfo().isCausedByLeave()) {
			this.client.debug("member leaved!");
		}
	}
}

