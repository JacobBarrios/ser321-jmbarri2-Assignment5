# ser321-jmbarri2-Assignment5

# Commands and Gradle Tasks
```gradle runLeader -PCPort=8080 -PNport=8081```

```gradle runNodes -PPort=8081```
```gradle runNodes -PPort=8082```
```gradle runNodes -PPort=8083```
  - Run it again with port numbers 8081-8083 or if using another port add 3 and run it on those ports

```gradle runClient -PHost=localhost -PPort=8080```


# Program's Purpose and Functionality
The purpose of this program to show how multithreaded calculations running in parallel work, compared to 
just calculating it without threads

# Description of Protocol
The Client will enter an array of numbers to add and send it to the Leader, the Leader will make threads for Nodes
so each one can calculate a part of the list from the client. The Nodes will send the result to the Leader, and the
Leader will send to the client

# Explanation of Workflow
1. Client sends list and delay to Leader
2. Leader spawns threads to parts of the list to 3 Nodes
3. Each Node computes partial sum with artificial delay
4. Leader collects results and checks consensus (optionally simulates faults)
5. Leader compares time of single-threaded vs. distributed
6. Results are sent back to Client for display

# Requirements List
## System Should Consist of
- [X] Client: Accepts a list of numbers and delay value from the user and
sends it to the leader for processing
- [X] Leader: Divides the list into smaller portions, sends each portion to
a different node, waits for results, and combines these to get the final
sum. THe leader also performs a simple consensus to verify results
- [X] Nodes: Each node calculates the sum of its portion of the list,
simulating computation time by sleeping for a given duration (100ms to 500 ms, then sends
the results back to the leader

## Detailed Requirements
- [x] Leader started via Gradle task
- [X] Client started via Gradle task
- [X] Each node started via Gradle task
  - [X] Should only be one Node.java class, with all nodes in the system being instances of this single class
- [X] 3 nodes are connected to the Leader
  - [ ] Send error if fewer, and stop processing
- [X] Client asks user to input a list of numbers and a delay time, and sent to the leader
- [X] Single Sum Calculations
  - [X] Leader calculates sum on its own
  - [X] Apply delay between each addition
- [X] Distributed Sum Calculations
  - [X] Leader divides list into equal parts with 3 nodes
  - [X] Should be threaded
  - [X] Computes the sum of its portion
  - [X] Apply delay between each addition
- [X] Performance Comparison
  - [X] Leader compares the time between Single sum, and Distributed sum
- [X] Simulates Faulty Node
  - [X] Use Gradle flag '-pFault=1' to make a node perform an incorrect calculation
- [X] Consensus Check for Result Verification
  - [X] Leader sends each node the sum and list from another node
  - [X] Sending this information should be threaded
  - [X] Each node recalculates the sum, and compares sum sent from leader
  - [X] Node return a true/false response to indicate agreement
  - [X] If all nodes agree on the result, leader sends the final sum and computation times to the client
  - [X] If fails leader sends an error message to the client
- [X] Client displays the results clearly
  - [X] Showing the sum
  - [X] Showing calculations time
- [X] Good error handling

# Screencast
https://youtu.be/3sbptXCuQwU

# Analysis
While the idea of the distributed, multithreaded computation, seems like a more efficient method. The results showed
that the single-threaded computation was faster in this scenario. This might be primarily due to:
  - Overhead from creating and managing threads
  - Time spent transmitting data to and from the nodes
  - Consensus checking and error simulation adding latency
Since the list size is small and the artificial delays are consistent, the cost of distributing the work outweighs 
the benefits of parallel execution. However, in larger-scale or real-world scenarios involving heavy computation, distributed systems 
would outperform single-threaded ones.