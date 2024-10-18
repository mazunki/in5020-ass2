package com.ass2;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/*
 * A replica is the underlying implementation for a client without
 * network awareness and thus does not handle broadcasting nor
 * synchronization
**/
public class Replica {
    private final Account account;
    private final String replicaId;

    private final List<Transaction> executed = new ArrayList<>();
	private int order_counter = 0;

    public Replica(String accountName, String replicaName) {
        this.account = new Account(accountName);
		this.replicaId = replicaName;
    }

    public String getId() {
        return replicaId;
    }

	public int getOrderCounter() {
		return this.order_counter;
	}

	/*
	 * Manly used by the client listener to recive transactions.
	 *
	 * We execute tasks, add it to our history, and count it. Some transactions are not
	 * added to the history and are not counted either.
	 * 
	 * This method also called by the client directly when doing synchronization.
	**/
    public void processTransaction(Transaction transaction) {
        String[] args = transaction.commandArgs();
        String cmd = transaction.commandName();

        switch (cmd.toLowerCase()) {
            case "deposit", "add" -> {
                if (args.length == 1) {
                    deposit(new BigDecimal(args[0]));
                    executed.add(transaction);
					order_counter++;
                } else {
                    System.err.println("Invalid deposit command.");
                }
            }
            case "addinterest", "interest" -> {
                if (args.length == 1) {
                    addInterest(Integer.parseInt(args[0]));
                    executed.add(transaction);
					order_counter++;
                } else {
                    System.err.println("Invalid interest command.");
                }
            }
            case "getquickbalance", "balance" -> {
				getQuickBalance();
				executed.add(transaction);
				order_counter++;
			}
            case "getsyncedbalance" -> getQuickBalance();
            case "checktxstatus", "check" -> checkTxStatus(transaction.getId());
            case "cleanhistory", "clearHistory" -> cleanHistory();
            default -> System.err.println("unknown command: " + cmd);
        }
    }

	/*
	 * Used by the client to build a new transaction from a command. The caller is responsible
	 * for incrementing the counter.
	**/
	public Transaction makeTransaction(String commandName, String[] args, int counter) {
		String cmd;
		String commandArgs = String.join(" ", args);

        switch (commandName.toLowerCase()) {
            case "deposit", "add" -> cmd = "deposit";
            case "addinterest", "interest" -> cmd = "addInterest";
            case "checktxstatus", "check" -> cmd = "checkTxStatus";
            case "getquickbalance", "balance" -> cmd = "getQuickBalance";
            case "getsyncedbalance" -> cmd = "getSyncedBalance";
            case "cleanhistory", "clearHistory" -> cmd = "cleanHistory";
            default -> {
				System.err.println("no such replica command: " + commandName);
				return null;
			}
        }

		if (commandArgs.length() > 0) {
			cmd = cmd + " " + commandArgs;
		}

		return new Transaction(cmd, this.getId() + " "  + counter);
	}

	public boolean checkTxStatusImpl(String transactionUniqueId) {
		boolean found = false;
		for (Transaction t : this.executed) {
			if (t.getId().equals(transactionUniqueId)) {
				found = true;
				break;
			}
		}
		return found;
	}
	
	// Check if the transaction exist in our history
	public boolean checkTxStatus(String transactionUniqueId) {
		boolean found = this.checkTxStatusImpl(transactionUniqueId);
		debug("checking for " + transactionUniqueId);
		return found;
	}

    public void deposit(BigDecimal amount) {
		debug("depositing " + amount);
        account.deposit(amount);
    }

    public void addInterest(int percent) {
		debug("adding interest " + percent + "%");
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
	/*
	 * Allows us to run the replica as a standalone instance. Follows similar
	 * input handling as an interactive client.
	**/
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Usage: java Replica <accountName>");
			return;
		}

		String accountName = args[0];  // Account name for the replica group

		Replica replica = new Replica(accountName, "standalone");

		// Interactive mode for a non-replicated instance
		System.out.println("Single non-replicated instance. Enter your commands (type 'exit' to quit):");
		try (Scanner scanner = new Scanner(System.in)) {
			while (true) {
				System.out.print("> ");
				String line;
				try {
					line = scanner.nextLine();
					if (line.equalsIgnoreCase("exit")) {
						break;
					}
					if (line.isBlank()) continue;
					if (line.startsWith("#")) continue;
				} catch (NoSuchElementException e) {
					break;
				}

				// Parse and process the command
				String[] parts = line.split(" ");
				String command = parts[0];
				String[] argsArray = Arrays.copyOfRange(parts, 1, parts.length);

				Transaction transaction = new Transaction(command + " " + String.join(" ", argsArray), "1");
				replica.processTransaction(transaction);
			}
		}

		System.out.println("quickBalance: " + replica.getQuickBalance());
	}

}

