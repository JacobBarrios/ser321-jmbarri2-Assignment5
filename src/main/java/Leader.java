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
		
		try(ServerSocket server = new ServerSocket(Cport);
			ServerSocket node1Server = new ServerSocket(Nport);
			ServerSocket node2Server = new ServerSocket((Nport + 1));
			ServerSocket node3Server = new ServerSocket((Nport + 2))) {
			
			while(true) {
				System.out.println("[DEBUG] Waiting for node connections...");
				Socket node1 = node1Server.accept();
				System.out.println("[DEBUG] Node 1 connected");
				Socket node2 = node2Server.accept();
				System.out.println("[DEBUG] Node 2 connected");
				Socket node3 = node3Server.accept();
				System.out.println("[DEBUG] Node 3 connected");
				
				System.out.println("[DEBUG] Waiting for client connection");
				Socket client = server.accept();
				System.out.println("[DEBUG] Connected to client");
				
				// Start new Leader thread for new client
				LeaderThread leaderThread = new LeaderThread(client, node1, node2, node3);
				leaderThread.start();
			}
		}
		catch (IOException e) {
			System.out.println("Connection error");
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
		try (PrintWriter outClient = new PrintWriter(client.getOutputStream(), true);
			 BufferedReader inClient = new BufferedReader(new InputStreamReader(client.getInputStream()))
		) {
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
				
				// TODO Add consensus logic
				
				// Builds and sends the response with the result
				response = new JSONObject();
				response.put("Type", "Result");
				response.put("Single", singleSumResult);
				response.put("Distributed", distributedSumResult);
				response.put("SingleTime", singleTotalTimeSeconds);
				response.put("DistributedTime", distributedTotalTimeSeconds);
				outClient.println(response);
				System.out.println("[DEBUG] Sent results");
			
			}
		}
		catch(IOException e) {
			System.out.println("Error initializing client I/O streams");
		}
		catch(NullPointerException e) {
			System.out.println("Response from client is null");
		}
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
	
	public int distributedSum(ClientData clientData, Socket node1, Socket node2, Socket node3) {
		int[] clientDataList = clientData.getDataList();
		int partialSize = clientDataList.length / 3;
		int[] part1 = new int[partialSize];
		int[] part2 = new int[partialSize];
		int[] part3 = new int[clientDataList.length - (2 * partialSize)];
		
		System.arraycopy(clientDataList, 0, part1, 0, partialSize);
		System.arraycopy(clientDataList, partialSize, part2, 0, partialSize);
		System.arraycopy(clientDataList, 2 * partialSize, part3, 0, part3.length);
		
		Thread node1Thread = new NodesThread(node1, part1, 1, clientData);
		Thread node2Thread = new NodesThread(node2, part2, 2, clientData);
		Thread node3Thread = new NodesThread(node3, part3, 3, clientData);
		
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
	
	/**
	 * Creates a new instance of NodesThread
	 *
	 * @param node Socket of node
	 * @param partialDataList Partial list of data
	 * @param nodeNumber Number of the node
	 * @param clientData Class that holds all the data
	 */
	NodesThread(Socket node, int[] partialDataList, int nodeNumber, ClientData clientData) {
		this.node = node;
		this.partialDataList = partialDataList;
		this.nodeNumber = nodeNumber;
		this.clientData = clientData;
	}
	
	/**
	 * Runs the NodesThread class
	 */
	public void run() {
		try {
			PrintWriter outNode = new PrintWriter(node.getOutputStream(), true);
			BufferedReader inNode = new BufferedReader(new InputStreamReader(node.getInputStream()));
			
			JSONObject computation = new JSONObject();
			computation.put("Type", "Data");
			computation.put("List", partialDataList);
			computation.put("Delay", clientData.getDelay());
			outNode.println(computation);
			System.out.println("[DEBUG] Sent data to Node " + nodeNumber);
			
			String stringResult = inNode.readLine();
			JSONObject result = new JSONObject(stringResult);
			clientData.setResult(result.getInt("Result"), this.nodeNumber);
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
	
	/**
	 * Creates a new instance of ClientData
	 * @param dataList
	 * @param delay
	 */
	public ClientData(int[] dataList, int delay) {
		this.dataList = dataList;
		this.delay = delay;
	}
	
	public int[] getDataList() {return dataList;}
	
	public void setDataList(int[] dataList) {this.dataList = dataList;}
	
	public int getDelay() {return delay;}
	
	public void setDelay(int delay) {this.delay = delay;}
	
	public int getResult(int nodeNumber) {
		if(nodeNumber == 1) {
			return result1;
		}
		else if(nodeNumber == 2) {
			return result2;
		}
		else {
			return result3;
		}
	}
	
	public void setResult(int result, int nodeNumber) {
		if(nodeNumber == 1) {
			result1 = result;
		}
		else if(nodeNumber == 2) {
			result2 = result;
		}
		else {
			result3 = result;
		}
	}
	
	public int computeDistributeResult() {return result1 + result2 + result3;}
}