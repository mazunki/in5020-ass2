package com.ass2;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SimulateClients {

    public static void main(String[] args) throws UnknownHostException, InterruptedException {
        if (args.length != 4) {
            System.out.println("Usage: java SimulateClients <server> <account> <no_replicas> <pathToCommands>");
            return;
        }

        String serverAddress = args[0];  // Server address
        String accountName = args[1];    // Account name for the replica group
        int noOfReplicas = Integer.parseInt(args[2]);
        String pathToCommands = args[3];

		String commandPrefix = pathToCommands + "/commands";
		String commandSuffix = ".txt";

        InetAddress address = InetAddress.getByName(serverAddress);
        Client[] clients = new Client[noOfReplicas];

        // Create a thread pool to run each Client in its own thread
        ExecutorService executorService = Executors.newFixedThreadPool(noOfReplicas);

        // Submit each client to the thread pool with their respective commands file
        for (int i = 0; i < 2; i++) {
            final int index = i;
			System.out.println("building new");
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


        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
				executorService.shutdownNow();
			}
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (Client client : clients) {
            System.out.println(client.toString() + " final balance: " + client.getReplica().getQuickBalance());
        }
    }
}

