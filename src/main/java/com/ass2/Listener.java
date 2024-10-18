package com.ass2;

import spread.AdvancedMessageListener;
import spread.SpreadGroup;
import spread.SpreadMessage;

public class Listener implements AdvancedMessageListener {

    private Client client;

    public Listener(Client client) {
        this.client = client;
    }

    @Override
    public void regularMessageReceived(SpreadMessage message) {
        try {
            Transaction transaction = (Transaction) message.getObject();
            String uniqueId = transaction.getUniqueId();

            System.out.println("Received transaction: " + transaction.getCommand() + " with ID: " + uniqueId);

            // Handle the received transaction (e.g., process and add it to executedList)
            client.addPending(transaction);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void membershipMessageReceived(SpreadMessage message) {
        // Handle membership changes here, if needed
        SpreadGroup group = message.getGroups()[0];
        System.out.println("Membership change in group: " + group);
    }
}

