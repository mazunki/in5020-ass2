package com.ass2;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SimulateClients {

    public static void main(String[] args) throws UnknownHostException {
        if (args.length != 4) {
            System.out.println("Usage: java SimulateClients <server> <account> <no_replicas> <pathToCommands>");
            return;
        }

        String serverAddress = args[0];  // Server address
        String accountName = args[1];    // Account name for the replica group
        int noOfReplicas = Integer.parseInt(args[2]);  // Number of replicas
        String pathToCommands = args[3];  // Path to the command files

        InetAddress address = InetAddress.getByName(serverAddress);
        Client[] clients = new Client[noOfReplicas];

        // Create a thread pool to run each Client in its own thread
        ExecutorService executorService = Executors.newFixedThreadPool(noOfReplicas);

        // Submit each client to the thread pool with their respective commands file
        for (int i = 0; i < noOfReplicas; i++) {
            final int index = i;
            executorService.submit(() -> {
                String commandFile = pathToCommands + (index + 1) + ".txt";
                clients[index] = new Client(address, accountName, "Rep" + (index + 1));
                clients[index].loadCommandsFromFile(commandFile);
            });
        }

        // Graceful shutdown of the ExecutorService
        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (Client client : clients) {
            System.out.println(client.toString() + " final balance: " + client.getSyncedBalance());
        }
    }
}

