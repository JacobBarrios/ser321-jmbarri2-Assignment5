# ser321-jmbarri2-Assignment5

# Commands and Gradle Tasks

# Program's Purpose and Functionality

# Description of Protocol

# Explanation of Workflow

# Requirements List
## System Should Consist of
- [ ] Client: Accepts a list of numbers and delay value from the user and
sends it to the leader for processing
- [ ] Leader: Divides the list into smaller portions, sends each portion to
a different node, waits for results, and combines these to get the final
sum. THe leader also performs a simple consensus to verify results
- [ ] Nodes: Each node calculates the sum of its portion of the list,
simulating computation time by sleeping for a given duration (100ms to 500 ms, then sends
the results back to the leader

## Detailed Requirements
- [ ] Leader started via Gradle task
- [ ] Client started via Gradle task
- [ ] Each node started via Gradle task
  - [ ] Should only be one Node.java class, with all nodes in the system being instances of this single class
- [ ] 3 nodes are connected to the Leader
  - [ ] Send error if fewer, and stop processing
- [ ] Client asks user to input a list of numbers and a delay time, and sent to the leader
- [ ] Single Sum Calculations
  - [ ] Leader calculates sum on its own
- [ ] Distributed Sum Calculations
  - [ ] Leader divides list into equal parts with 3 nodes
  - [ ] Should be threaded
  - [ ] Computes the sum of its portion, apply delay between each addition
- [ ] Performance Comparison
  - [ ] Leader compares the time between Single sum, and Distributed sum
- [ ] Simulates Faulty Node
  - [ ] Use Gradle flag '-pFault=1' to make a node perform an incorrect calculation
- [ ] Consensus Check for Result Verification
  - [ ] Leader sends each node the sum and list from another node
  - [ ] Sending this information should be threaded
  - [ ] Each node recalculates the sum, and compares sum sent from leader
  - [ ] Node return a true/false response to indicate agreement
  - [ ] If all nodes agree on the result, leader sends the final sum and computation times to the client
  - [ ] If fails leader sends an error message to the client
- [ ] Client displays the results clearly, showing the sum and calculations time
- [ ] Good error handling
- [ ]

# Screencast

# Analysis