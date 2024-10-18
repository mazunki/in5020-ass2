package com.ass2;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/*
 * Runs a couple of clients on different threads and waits for them to finish
 * @param serverAddress The address of the spread server
 * @param accountName The name of the account to use
 * @param noOfReplicas The number of replicas to simulate
 * @param pathToCommands The directory path to the command files
 *                       this directory should contain files named Rep1.txt, Rep2.txt, etc.
**/
public class SimulateClients {

    public static void main(String[] args) throws UnknownHostException, InterruptedException {
        if (args.length != 4) {
            System.out.println("Usage: java SimulateClients <server> <account> <no_replicas> <pathToCommands>");
            return;
        }

        String serverAddress = args[0];
        String accountName = args[1];
        int noOfReplicas = Integer.parseInt(args[2]);
        String pathToCommands = args[3];

		String commandPrefix = pathToCommands + "/Rep";
		String commandSuffix = ".txt";

        InetAddress address = InetAddress.getByName(serverAddress);
        Client[] clients = new Client[noOfReplicas];

        // Create a thread pool to run each Client in its own thread
        ExecutorService executorService = Executors.newFixedThreadPool(noOfReplicas);

        /*
         * First we start up two clients, then we wait for 15 seconds and then we start the rest

         * for each client we use the same address and account name, but the command file will instead be 
         * Rep1.txt, Rep2.txt, etc. found in the pathToCommands directory
        **/
        for (int i = 0; i < 2; i++) {
            final int index = i;
            executorService.submit(() -> {
                String commandFile = commandPrefix + (index + 1) + commandSuffix;
                clients[index] = new Client(address, accountName, "Rep" + (index + 1));
                clients[index].loadCommandsFromFile(commandFile);
            });
        }

		Thread.sleep(15*1000);

        for (int i = 2; i < noOfReplicas; i++) {
            final int index = i;
            executorService.submit(() -> {
                String commandFile = commandPrefix + (index + 1) + commandSuffix;
                clients[index] = new Client(address, accountName, "Rep" + (index + 1));
                clients[index].loadCommandsFromFile(commandFile);
            });
        }

        // We do not need to submit any more tasks to the executor
        executorService.shutdown();

        // Wait for all thread on clients to finish
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
				executorService.shutdownNow();
			}
        } catch (InterruptedException e) {
        }
        
        for (Client client : clients) {
            System.out.println(client.toString() + " final balance: " + client.getReplica().getQuickBalance());
        }
    }
}

