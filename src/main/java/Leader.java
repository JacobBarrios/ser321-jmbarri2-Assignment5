import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * Leader class handles the single sum computation, and handles communication between nodes
 * for distributed sum calculations
 *
 * @author Jacob Barrios
 * @version 1.0
 */
public class Leader {
	
	/**
	 * Main that takes arguments from gradle run command for the client port, and the node port
	 * Creates threads for multiple client, and include 3 nodes for Distributed sum calculations
	 *
	 * @param args Contains client port, and node port
	 */
	public static void main(String[] args) {
		int Cport = Integer.parseInt(args[0]);
		int Nport = Integer.parseInt(args[1]);
		ServerSocket server = null;
		ServerSocket node1Server = null;
		ServerSocket node2Server = null;
		ServerSocket node3Server = null;
		
		Socket node1 = null;
		Socket node2 = null;
		Socket node3 = null;
		Socket client = null;
		
		try {
			server = new ServerSocket(Cport);
			node1Server = new ServerSocket(Nport);
			node2Server = new ServerSocket((Nport + 1));
			node3Server = new ServerSocket((Nport + 2));
			
			while(true) {
				System.out.println("[DEBUG] Waiting for node connections...");
				node1 = node1Server.accept();
				System.out.println("[DEBUG] Node 1 connected");
				node2 = node2Server.accept();
				System.out.println("[DEBUG] Node 2 connected");
				node3 = node3Server.accept();
				System.out.println("[DEBUG] Node 3 connected");
				
				System.out.println("[DEBUG] Waiting for client connection");
				client = server.accept();
				System.out.println("[DEBUG] Connected to client");
				
				// Start new Leader thread for new client
				LeaderThread leaderThread = new LeaderThread(client, node1, node2, node3);
				leaderThread.start();
			}
		}
		catch (IOException e) {
			System.out.println("Connection error");
		}
		finally {
			try {
				if(server != null) {server.close();}
				if(node1Server != null) {node1Server.close();}
				if(node2Server != null) {node2Server.close();}
				if(node3Server != null) {node3Server.close();}
				if(node1 != null) {node1.close();}
				if(node2 != null) {node2.close();}
				if(node3 != null) {node3.close();}
				if(client != null) {client.close();}
				
			}
			catch(IOException e){
				throw new RuntimeException(e);
			}
		}
	}
}

/**
 * Represents the thread for each client to handle single sum calculations
 * Handles result of Single and Distributed sum calculations
 */
class LeaderThread extends Thread {
	private final Socket client;
	private final Socket node1, node2, node3;
	
	/**
	 * Create a new instance of LeaderThread
	 *
	 * @param client Socket of client
	 * @param node1 Socket of node 1
	 * @param node2 Socket of node 2
	 * @param node3 Socket of node 3
	 */
	public LeaderThread(Socket client, Socket node1, Socket node2, Socket node3) {
		this.client = client;
		this.node1 = node1;
		this.node2 = node2;
		this.node3 = node3;
	}
	
	/**
	 * Runs the LeaderThread class
	 */
	public void run() {
		PrintWriter outClient = null;
		BufferedReader inClient = null;
		
		try {
			outClient = new PrintWriter(client.getOutputStream(), true);
			inClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
			
			// Sends start response to Client
			JSONObject response = new JSONObject();
			response.put("Type", "Start");
			response.put("Message", "Connected to server");
			outClient.println(response);
			System.out.println("[DEBUG] Sent start message");
			
			while(true) {
				// Receive request from Client
				String stringRequest = inClient.readLine();
				JSONObject clientRequest = new JSONObject(stringRequest);
				JSONArray arrayDataList = clientRequest.getJSONArray("List");
				ClientData clientData = new ClientData(convertJSONArray(arrayDataList), clientRequest.getInt("Delay"));
				System.out.println("[DEBUG] Received client data");
				
				// Single sum result
				long singleStartTime = System.nanoTime();
				int singleSumResult = singleSum(clientData);
				long singleEndTime = System.nanoTime();
				int singleTotalTimeSeconds = (int)((singleEndTime - singleStartTime) / 1000000000);
				System.out.println("[DEBUG] Single sum calculations: " + singleSumResult);
				System.out.printf("[DEBUG] Single sum total time: %d s\n", singleTotalTimeSeconds);
				
				// Distributed sum result
				long distributedStartTime = System.nanoTime();
				int distributedSumResult = distributedSum(clientData, node1, node2, node3);
				long distributedEndTime = System.nanoTime();
				int distributedTotalTimeSeconds = (int)((distributedEndTime - distributedStartTime) / 1000000000);
				System.out.printf("[DEBUG] Distributed sum calculations: %d s\n", distributedSumResult);
				System.out.printf("[DEBUG] Distributed sum total time: %d s\n", distributedTotalTimeSeconds);
				
				
				// Print time comparison
				System.out.printf("Single sum time: %d, Distributed sum time: %d", singleTotalTimeSeconds, distributedTotalTimeSeconds);
				
				boolean consensusCheck = computationConsensus(clientData, node1, node2, node3);
				
				// Builds and sends the response with the result
				response = new JSONObject();
				if(consensusCheck) {
					response.put("Type", "Result");
					response.put("Single", singleSumResult);
					response.put("Distributed", distributedSumResult);
					response.put("SingleTime", singleTotalTimeSeconds);
					response.put("DistributedTime", distributedTotalTimeSeconds);
				}
				else {
					response.put("Type", "Error");
					response.put("Message", "Error with computation");
				}
				
				outClient.println(response);
				System.out.println("[DEBUG] Sent response");
			}
		}
		catch(IOException e) {
			System.out.println("Error initializing client I/O streams");
		}
		catch(NullPointerException e) {
			System.out.println("Response from client is null");
		}
		// Ensure proper cleanup of resources
		try {
			if (inClient != null) inClient.close();
			if (outClient != null) outClient.close();
			System.out.println("[DEBUG] Resources closed successfully.");
		} catch (IOException e) {
			System.out.println("[DEBUG] Error during shutdown: " + e.getMessage());
		}
	}
	
	public boolean computationConsensus(ClientData clientData, Socket node1, Socket node2, Socket node3) {
		int[] clientDataList = clientData.getDataList();
		int partialSize = clientDataList.length / 3;
		int[] part1 = new int[partialSize];
		int[] part2 = new int[partialSize];
		int[] part3 = new int[clientDataList.length - (2 * partialSize)];
		
		System.arraycopy(clientDataList, 0, part1, 0, partialSize);
		System.arraycopy(clientDataList, partialSize, part2, 0, partialSize);
		System.arraycopy(clientDataList, 2 * partialSize, part3, 0, part3.length);
		
		// Sends that this is a consensus check, and passes different parts, with the corresponding nodeNumber
		Thread node1Thread = new NodesThread(node1, part2, 2, clientData, true);
		Thread node2Thread = new NodesThread(node2, part3, 3, clientData, true);
		Thread node3Thread = new NodesThread(node3, part1, 1, clientData, true);
		
		node1Thread.start(); node2Thread.start(); node3Thread.start();
		System.out.println("[DEBUG] Started consensus threads");
		try {
			node1Thread.join(); node2Thread.join(); node3Thread.join();
			System.out.println("[DEBUG] Consensus threads finished");
		}
		catch(InterruptedException e) {
			System.out.println("[DEBUG] Node thread interrupted");
		}
		
		return clientData.getConsensus();
		
	}
	
	public int distributedSum(ClientData clientData, Socket node1, Socket node2, Socket node3) {
		int[] clientDataList = clientData.getDataList();
		int partialSize = clientDataList.length / 3;
		int[] part1 = new int[partialSize];
		int[] part2 = new int[partialSize];
		int[] part3 = new int[clientDataList.length - (2 * partialSize)];
		
		System.arraycopy(clientDataList, 0, part1, 0, partialSize);
		System.arraycopy(clientDataList, partialSize, part2, 0, partialSize);
		System.arraycopy(clientDataList, 2 * partialSize, part3, 0, part3.length);
		
		Thread node1Thread = new NodesThread(node1, part1, 1, clientData, false);
		Thread node2Thread = new NodesThread(node2, part2, 2, clientData, false);
		Thread node3Thread = new NodesThread(node3, part3, 3, clientData, false);
		
		node1Thread.start(); node2Thread.start(); node3Thread.start();
		System.out.println("[DEBUG] Started threads");
		try {
			node1Thread.join(); node2Thread.join(); node3Thread.join();
			System.out.println("[DEBUG] Threads finished");
		}
		catch(InterruptedException e) {
			System.out.println("[DEBUG] Node thread interrupted");
		}
		
		return clientData.computeDistributeResult();
		
	}
	
	/**
	 * Method to handle the single sum calculations
	 *
	 * @param clientData Class that holds the list of data
	 * @return Result of calculations
	 */
	public int singleSum(ClientData clientData) {
		int[] dataList = clientData.getDataList();
		int sum = 0;
		int seconds = clientData.getDelay();
		
		try {
			for(int j : dataList) {
				sum += j;
				
				Thread.sleep(seconds);
			}
		}
		catch(InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		return sum;
		
	}
	
	/**
	 * Converts the giving JSONArray into int[]
	 *
	 * @param JSONList List sent from client
	 * @return new int[] list
	 */
	public int[] convertJSONArray(JSONArray JSONList) {
		int[] dataList = new int[JSONList.length()];
		
		for(int i = 0; i < JSONList.length(); i++) {
			dataList[i] = JSONList.getInt(i);
		}
		
		return dataList;
		
	}
}

/**
 * Represents the thread for each node to handle distributed sum calculations
 * Sends data to 3 nodes for distribute sum calculations
 */
class NodesThread extends Thread {
	private final Socket node;
	private final int[] partialDataList;
	private final int nodeNumber;
	private final ClientData clientData;
	private final boolean consensus;
	
	/**
	 * Creates a new instance of NodesThread
	 *
	 * @param node Socket of node
	 * @param partialDataList Partial list of data
	 * @param nodeNumber Number of the node
	 * @param clientData Class that holds all the data
	 */
	NodesThread(Socket node, int[] partialDataList, int nodeNumber, ClientData clientData, boolean consensus) {
		this.node = node;
		this.partialDataList = partialDataList;
		this.nodeNumber = nodeNumber;
		this.clientData = clientData;
		this.consensus = consensus;
	}
	
	/**
	 * Runs the NodesThread class
	 */
	public void run() {
		PrintWriter outNode = null;
		BufferedReader inNode = null;
		
		try {
			outNode = new PrintWriter(node.getOutputStream(), true);
			inNode = new BufferedReader(new InputStreamReader(node.getInputStream()));
			
			JSONObject request = new JSONObject();
			System.out.println("Consensus: " + this.consensus);
			if(!consensus) {
				request.put("Type", "Data");
				request.put("List", partialDataList);
				request.put("Delay", clientData.getDelay());
				System.out.println("[DEBUG] Sent data to Node " + nodeNumber);
			}
			else {
				request.put("Type", "Consensus");
				request.put("List", partialDataList);
				request.put("Delay", clientData.getDelay());
				request.put("Sum", clientData.getResult(nodeNumber));
				System.out.println("[DEBUG] Sent consensus from Node " + nodeNumber);
			}
			
			outNode.println(request);
			
			
			String stringResponse = inNode.readLine();
			JSONObject response = new JSONObject(stringResponse);
			System.out.println("[DEBUG] Consensus result: " + response.toString());
			if(response.get("Type").equals("Result")) {
				clientData.setResult(response.getInt("Result"), this.nodeNumber);
			}
			else if(response.get("Type").equals("Consensus")) {
				clientData.setConsensus(response.getBoolean("Consensus"), this.nodeNumber);
			}
		}
		catch(IOException e) {
			System.out.println("Connection error");
		}
	}
}

/**
 * This class represents the shared data between Leader and Nodes
 */
class ClientData {
	private int[] dataList;
	private int delay;
	private int result1;
	private int result2;
	private int result3;
	private boolean consensus1;
	private boolean consensus2;
	private boolean consensus3;
	
	/**
	 * Creates a new instance of ClientData
	 * @param dataList
	 * @param delay
	 */
	public ClientData(int[] dataList, int delay) {
		this.dataList = dataList;
		this.delay = delay;
	}
	
	public int[] getDataList() {return this.dataList;}
	
	public void setDataList(int[] dataList) {this.dataList = dataList;}
	
	public int getDelay() {return this.delay;}
	
	public void setDelay(int delay) {this.delay = delay;}
	
	public int getResult(int nodeNumber) {
		if(nodeNumber == 1) {
			return this.result1;
		}
		else if(nodeNumber == 2) {
			return this.result2;
		}
		else {
			return this.result3;
		}
	}
	
	public void setResult(int result, int nodeNumber) {
		if(nodeNumber == 1) {
			this.result1 = result;
		}
		else if(nodeNumber == 2) {
			this.result2 = result;
		}
		else {
			this.result3 = result;
		}
	}
	
	public void setConsensus(boolean consensus, int nodeNumber) {
		if(nodeNumber == 1) {
			this.consensus1 = consensus;
		}
		else if(nodeNumber == 2) {
			this.consensus2 = consensus;
		}
		else {
			this.consensus3 = consensus;
		}
	}
	
	public boolean getConsensus() {
		return this.consensus1 && this.consensus2 && this.consensus3;
	}
	
	public int computeDistributeResult() {return this.result1 + this.result2 + this.result3;}
}