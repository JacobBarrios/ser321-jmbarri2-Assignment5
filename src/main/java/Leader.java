import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.Buffer;

public class Leader {
	public static void main(String[] args) {
		int Cport = Integer.parseInt(args[0]);
		int Nport = Integer.parseInt(args[1]);
		Socket client = null;
		Socket nodes = null;
		
		try(ServerSocket server = new ServerSocket(Cport);
			ServerSocket nodeServer = new ServerSocket(Nport)
			) {
			System.out.println("[DEBUG] Leader running on port: " + Cport);
			System.out.println("[DEBUG] Node running on port: " + Nport);
			
			while(true) {
				System.out.println("Waiting for client connection");
				client = server.accept();
				System.out.println("Connected to client");
				
//				System.out.println("Waiting for node connection");
//				nodes = nodeServer.accept();
//				System.out.println("Connected to node");
				
				LeaderThread leaderThread = new LeaderThread(client);
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
//	private final Socket nodes;
	
	private PrintWriter outClient;
	private PrintWriter outNodes;
	
	private BufferedReader inClient;
	private BufferedReader inNodes;
	
	private int[] dataList;
	private int delay;
	
	public LeaderThread(Socket client) {
		this.client = client;
//		this.nodes = nodes;
	}
	
	public void run() {
		buildReadersWriters();
		
		while(true) {
			try {
				String stringMessage = inClient.readLine();
				JSONObject clientMessage = new JSONObject(stringMessage);
				JSONArray arrayDataList = clientMessage.getJSONArray("List");

				this.dataList = convertJSONArray(arrayDataList);
				this.delay = clientMessage.getInt("Delay");
				
				int singleSumResult = singleSum();
				System.out.println("Single sum calculations: " + singleSumResult);
				
				JSONObject response = new JSONObject();
				response.put("Type", "Result");
				response.put("Single", singleSumResult);
				
				outClient.println(response.toString());
				System.out.println("Sent Response");
			}
			catch(IOException e) {
				System.out.println("Error reading message from client");
			}
		}
	}
	
	public int singleSum() {
		int sum = 0;
		
		for(int i = 0; i < dataList.length; i++) {
			sum += dataList[i];
		}
		
		return sum;
		
	}
	
	public void buildReadersWriters() {
		try {
			outClient = new PrintWriter(client.getOutputStream(), true);
//			outNodes = new PrintWriter(nodes.getOutputStream(), true);

			inClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
//			inNodes = new BufferedReader(new InputStreamReader(nodes.getInputStream()));
		}
		catch(IOException e) {
			System.out.println("Error creating readers and writers");
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