package com.ass2;

import java.util.Arrays;

import spread.AdvancedMessageListener;
import spread.SpreadException;
import spread.SpreadMessage;

public class Listener implements AdvancedMessageListener {
    @Override
	public void regularMessageReceived(SpreadMessage message) {
		String msg = null;
		try {
			msg = (String) message.getObject();
		} catch (SpreadException e) {
			throw new RuntimeException(e);
		}
		System.out.println(msg);
	}

	@Override
	public void membershipMessageReceived(SpreadMessage spreadMessage) {
		System.out.println(Arrays.toString(spreadMessage.getMembershipInfo().getMembers()));
	}
}
