package com.ass2;

import java.io.Serializable;

public class Transaction implements Serializable {
    private String command;
    private String id;

    public Transaction(String command, String id) {
        this.command = command;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getReplicaId() {
        return this.id.split(" ")[0];
    }

    public String getCommand() {
        return command;
    }

    public String commandName() {
        String[] parts = command.split(" ");
        return parts[0];
    }

    // Makes a list of the arguments of the provided command
    public String[] commandArgs() {
        String[] parts = command.split(" ", 2);
        return parts.length > 1 ? parts[1].split(" ") : new String[0];
    }

    /*
     * used by the Spread Listener when messsages are
     * received and they are transactions
     * 
     * @param client the client that received the transaction
    **/
	public void process(Client client) {
		Replica replica = client.getReplica();

		/*
         * getSyncedBalance is treated differently because 
         * it's never intended for the replica so we just 
         * return to the client which requested a sync. 
        **/
        if (this.commandName().equals("getSyncedBalance")) {
			if (this.getReplicaId().equals(replica.getId())) {
				client.completeSync();
				return;
			}
		}
		replica.processTransaction(this);
	}

    @Override
    public String toString() {
        return "Transaction{cmd=" + command + ", id=" + id + "}";
    }
}

