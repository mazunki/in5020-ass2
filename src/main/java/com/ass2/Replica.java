package com.ass2;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Replica {
    private final Account account;
    private final String replicaId;

    // List to store executed transactions
    private final List<Transaction> executed = new ArrayList<>();

    public Replica(String accountName, String replicaName) {
        this.account = new Account(accountName);
		this.replicaId = replicaName;
    }

    public String getId() {
        return replicaId;
    }

    // Process the entire transaction instead of just a command string
    public void processTransaction(Transaction transaction) {
        String[] args = transaction.commandArgs();
        String cmd = transaction.commandName();

        switch (cmd) {
            case "deposit" -> {
                if (args.length == 1) {
                    deposit(new BigDecimal(args[0]));
                    executed.add(transaction);  // Log transaction in executed list
                } else {
                    System.out.println("Invalid deposit command.");
                }
            }
            case "addInterest" -> {
                if (args.length == 1) {
                    addInterest(Integer.parseInt(args[0]));
                    executed.add(transaction);  // Log transaction in executed list
                } else {
                    System.out.println("Invalid interest command.");
                }
            }
            case "checkTxStatus" -> checkTxStatus(args);
            case "getQuickBalance" -> getQuickBalance();
            case "cleanHistory" -> cleanHistory();
            default -> System.err.println("Unknown command.");
        }
    }

	public boolean checkTxStatus(String[] transactionUniqueId) {
		String txId = String.join(" ", transactionUniqueId);

		boolean found = false;
		for (Transaction t : this.executed) {
			if (t.getId().equals(txId)) {
				found = true;
				break;
			}
		}

		debug(transactionUniqueId + " was found: " + found);
		System.out.println(found);

		return found;
	}

    public void deposit(BigDecimal amount) {
		debug("depositing " + amount);
        account.deposit(amount);
    }

    public void addInterest(int percent) {
		debug("depositing " + percent + "%");
        account.addInterest(percent);
    }

    public BigDecimal getQuickBalance() {
		BigDecimal value = account.getQuickBalance();
		debug("quickBalance: " + value);
		System.out.println(value);
        return value;
    }

    public void cleanHistory() {
        executed.clear();
        debug("transaction history cleaned");
    }

	public void debug(String s) {
		System.err.println("Replica[" + this.getId() + "]: " + s);
	}

    public List<Transaction> getExecutedTransactions() {
        return executed;
    }
}

