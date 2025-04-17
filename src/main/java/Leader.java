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
		
		try(ServerSocket server = new ServerSocket(Cport)) {
			System.out.println("[DEBUG] Leader running on port: " + Cport);
			
			while(true) {
				System.out.println("Waiting for client connection");
				client = server.accept();
				System.out.println("Connected to client");
				
				LeaderThread leaderThread = new LeaderThread(client, Nport);
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
	private Socket node1;
	private Socket node2;
	private Socket node3;
	
	private PrintWriter outClient;
	
	private BufferedReader inClient;
	
	private int nodePort;
	
	public LeaderThread(Socket client, int nodePort) {
		this.client = client;
		this.nodePort = nodePort;
	}
	
	public void run() {
		try {
			// Set up client communication streams
			outClient = new PrintWriter(client.getOutputStream(), true);
			inClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
		}
		catch (IOException e) {
			System.out.println("Error initializing client I/O streams");
		}
		
//		connectNodes(nodePort);
		
		JSONObject response = new JSONObject();
		response.put("Type", "Start");
		response.put("Message", "Connected to server");
		outClient.println(response);
		System.out.println("Sent start message");
		
		while(true) {
			try {
				String stringMessage = inClient.readLine();
				JSONObject clientMessage = new JSONObject(stringMessage);
				JSONArray arrayDataList = clientMessage.getJSONArray("List");
				ClientData clientData = new ClientData(convertJSONArray(arrayDataList), clientMessage.getInt("Delay"));
				System.out.println("Received client data");

				int singleSumResult = singleSum(clientData);
				System.out.println("Single sum calculations: " + singleSumResult);
				
				response = new JSONObject();
				response.put("Type", "Result");
				response.put("Single", singleSumResult);
				response.put("Distributed", "Distributed sample result");
				
				outClient.println(response);
				System.out.println("Sent results");
			}
			catch(IOException e) {
				System.out.println("Error reading message from client");
				return;
			}
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
	
	public void connectNodes(int nodePort) {
		try(ServerSocket node1Server = new ServerSocket(nodePort);
			ServerSocket node2Server = new ServerSocket(nodePort + 1);
			ServerSocket node3Server = new ServerSocket(nodePort + 2)) {
			System.out.printf("Started node servers at port: %d, %d, %d\n", nodePort, nodePort + 1, nodePort + 2);
			
			System.out.println("Waiting for node connections");
			
			this.node1 = node1Server.accept();
			System.out.println("Connected node 1");
			
			this.node2 = node2Server.accept();
			System.out.println("Connected node 2");
			
			this.node3 = node3Server.accept();
			System.out.println("Connected node 3");
			
			
		}
		catch(IOException e) {
			System.out.println("Error with not connections");
		}
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
	
	NodesThread(Socket node) {
		this.node = node;
	}
	
	public void run() {
	
	}
}

class ClientData {
	private int[] dataList;
	private int delay;
	
	public ClientData(int[] dataList, int delay) {
		this.dataList = dataList;
		this.delay = delay;
	}
	
	public int[] getDataList() {
		return dataList;
	}
	
	public void setDataList(int[] dataList) {
		this.dataList = dataList;
	}
	
	public int getDelay() {
		return delay;
	}
	
	public void setDelay(int delay) {
		this.delay = delay;
	}
}