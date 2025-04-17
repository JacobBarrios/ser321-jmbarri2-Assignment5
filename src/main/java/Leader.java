import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Leader {
	public static void main(String[] args) {
		int Cport = Integer.parseInt(args[0]);
		int Nport = Integer.parseInt(args[1]);
		Socket client = null;
		
		try(ServerSocket server = new ServerSocket(Cport);
			ServerSocket node1Server = new ServerSocket(Nport);
			ServerSocket node2Server = new ServerSocket(Nport + 1);
			ServerSocket node3Server = new ServerSocket(Nport + 2)) {
			System.out.println("[DEBUG] Leader running on port: " + Cport);
			
			System.out.println("Waiting for node connections...");
			Socket node1 = node1Server.accept();
			System.out.println("Node 1 connected");
			Socket node2 = node2Server.accept();
			System.out.println("Node 2 connected");
			Socket node3 = node3Server.accept();
			System.out.println("Node 3 connected");
			
			while(true) {
				System.out.println("Waiting for client connection");
				client = server.accept();
				System.out.println("Connected to client");
				
				LeaderThread leaderThread = new LeaderThread(client, node1, node2, node3);
				leaderThread.start();
			}
		}
		catch (IOException e) {
			System.out.println("Connection error");
		}
	}
}

class LeaderThread extends Thread {
	private final Socket client;
	private final Socket node1, node2, node3;
	
	private PrintWriter outClient;
	
	private BufferedReader inClient;
	
	public LeaderThread(Socket client, Socket node1, Socket node2, Socket node3) {
		this.client = client;
		this.node1 = node1;
		this.node2 = node2;
		this.node3 = node3;
	}
	
	public void run() {
		try {
			// Set up client communication streams
			outClient = new PrintWriter(client.getOutputStream(), true);
			inClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
		
			JSONObject response = new JSONObject();
			response.put("Type", "Start");
			response.put("Message", "Connected to server");
			outClient.println(response);
			System.out.println("Sent start message");
			
			while(true) {
				String stringRequest = inClient.readLine();
				JSONObject clientRequest = new JSONObject(stringRequest);
				JSONArray arrayDataList = clientRequest.getJSONArray("List");
				ClientData clientData = new ClientData(convertJSONArray(arrayDataList), clientRequest.getInt("Delay"));
				System.out.println("Received client data");

				int singleSumResult = singleSum(clientData);
				System.out.println("Single sum calculations: " + singleSumResult);
				
				int[] clientDataList = clientData.getDataList();
				int partialSize = clientDataList.length / 3;
				
				int[] part1 = new int[partialSize];
				int[] part2 = new int[partialSize];
				int[] part3 = new int[clientDataList.length - 2 * partialSize];
				
				System.arraycopy(clientDataList, 0, part1, 0, partialSize);
				System.arraycopy(clientDataList, partialSize, part2, 0, partialSize);
				System.arraycopy(clientDataList, 2 * partialSize, part3, 0, part3.length);
				
				Thread node1Thread = new NodesThread(node1, part1, 1, clientData);
				Thread node2Thread = new NodesThread(node2, part2, 2, clientData);
				Thread node3Thread = new NodesThread(node3, part3, 3, clientData);
				
				node1Thread.start(); node2Thread.start(); node3Thread.start();
				node1Thread.join(); node2Thread.join(); node3Thread.join();
				
				int distributedResult = clientData.computeDistributeResult();
				
				response = new JSONObject();
				response.put("Type", "Result");
				response.put("Single", singleSumResult);
				response.put("Distributed", distributedResult);
				
				outClient.println(response);
				System.out.println("Sent results");
			
			}
		}
		catch (IOException e) {
			System.out.println("Error initializing client I/O streams");
		}
		catch(InterruptedException e) {
			System.out.println("Error node thread interrupted");
		}
	}
	
	public int singleSum(ClientData clientData) {
		int[] dataList = clientData.getDataList();
		int sum = 0;
		
		for(int i = 0; i < dataList.length; i++) {
			sum += dataList[i];
		}
		
		return sum;
		
	}
	
	public int[] convertJSONArray(JSONArray JSONList) {
		int[] dataList = new int[JSONList.length()];
		
		for(int i = 0; i < JSONList.length(); i++) {
			dataList[i] = JSONList.getInt(i);
		}
		
		return dataList;
		
	}
}

class NodesThread extends Thread {
	private final Socket node;
	private final int[] partialDataList;
	private final int nodeNumber;
	private final ClientData clientData;
	
	NodesThread(Socket node, int[] partialDataList, int nodeNumber, ClientData clientData) {
		this.node = node;
		this.partialDataList = partialDataList;
		this.nodeNumber = nodeNumber;
		this.clientData = clientData;
	}
	
	public void run() {
		try {
			PrintWriter outNode = new PrintWriter(node.getOutputStream());
			BufferedReader inNode = new BufferedReader(new InputStreamReader(node.getInputStream()));
		
			JSONObject computation = new JSONObject();
			computation.put("Type", "Data");
			computation.put("List", partialDataList);
			computation.put("Delay", clientData.getDelay());
			outNode.println(computation);
			
			String stringResult = inNode.readLine();
			JSONObject result = new JSONObject(stringResult);
			clientData.setResult(result.getInt("Result"), this.nodeNumber);
		}
		catch(IOException e) {
			System.out.println("Connection error");
		}
		
	}
}

class ClientData {
	private int[] dataList;
	private int delay;
	private int result1;
	private int result2;
	private int result3;
	
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
	
	public int computeDistributeResult() {
		return result1 + result2 + result3;
	}
}