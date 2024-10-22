# IN5020 --- Replicated Bank Account

Repository for the second assignment of IN5020, by group 7.


## Building and running

To build the program, simply run the following
```sh
make clean all
```

To run the program, you have a few options. If you're running a simulation, make sure to have your command files available under `src/main/java/resources/Rep{1,2,3}.txt`.

```sh
# runs 3 clients
make run_simulation

# starts a spread server connection in interactive mode
make run_client

# starts a single replica, offline, in interactive mode
make run_single_replica
```

If you want to disable the debugging output, and only care about the commands, simply slap on `2>/dev/null` on the end of your invocations.

## Differences between the two requested implementations for getSyncedBalance command
The naive implementation of the synced balance will simply wait until the outstanding collection is empty. This can cause deadlocks, and cause our program to never actually exit.

The better (optimized) implementation circumvents this issue entirely by simply queueing a new event which it waits on. Because our spread group is a FIFO queue, we can guarantee the order is maintained just fine.

## Distributed of the workload

As a team, we discussed the architecture of the project, figuring out what the requirements were. Lise implemented everything related to the account, and the listener; Mazunki worked on the naive synchronization, and together handled the rest of the Client, Replica and improved the implementation of the synchronization. Mouaz implemented the rest of the functions.

