import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.net.Socket;

import java.util.Random;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Nodes class handles the distributed sum calculations.
 *
 * @author Jacob Barrios
 * @version 1.0
 */
public class Nodes {
	
	/**
	 * Main that takes arguments from gradle run command for the Leader host, and the Node port.
	 *
	 * @param args Contains Leader host, and Node port
	 */
	public static void main(String[] args) {
		String host = args[0];
		int port = Integer.parseInt(args[1]);
		boolean fault = Boolean.parseBoolean(args[2]);
		System.out.println("[DEBUG] Port used: " + port);
		
		Socket server = null;
		PrintWriter out = null;
		BufferedReader in = null;
		
		try {
			server = new Socket(host, port);
			out = new PrintWriter(server.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(server.getInputStream()));
			
			System.out.println("[DEBUG] Connected to Leader");
			
			while(true) {
				try {
					String stringRequest = in.readLine();
					JSONObject request = new JSONObject(stringRequest);
					JSONArray arrayDataList = request.getJSONArray("List");
					int[] partialDataList = convertJSONArray(arrayDataList);
					int delay = request.getInt("Delay");
					System.out.println("[DEBUG] Received data from Leader");
					
					// Starts calculating the sum of the given list
					int sum = calculateSum(partialDataList, delay, fault);
					System.out.println("[DEBUG] Result: " + sum);
					
					JSONObject result = new JSONObject();
					if(request.get("Type").equals("Data")) {
						result.put("Type", "Result");
						result.put("Result", sum);
						out.println(result);
						System.out.println("[DEBUG] Sent result to Leader");
					}
					else if(request.get("Type").equals("Consensus")) {
						result.put("Type", "Consensus");
						result.put("Result", sum);
						result.put("Consensus", sum == request.getInt("Sum"));
						out.println(result);
						System.out.println("[DEBUG] Sent consensus to Leader");
					}
				}
				catch(IOException e) {
					System.out.println("Error reading response");
					return;
				}
			}
		}
		catch(IOException e) {
			throw new RuntimeException(e);
		}
		finally {
			// Ensure resources are closed
			try {
				if (in != null) {in.close();}
				if (out != null) {out.close();}
				if (server != null) {server.close();}
			} catch (IOException e) {
				System.out.println("Error closing resources: " + e.getMessage());
			}
		}
	}
	
	/**
	 * Converts the giving JSONArray into int[].
	 *
	 * @param JSONList List sent from client
	 * @return new int[] list
	 */
	public static int[] convertJSONArray(JSONArray JSONList) {
		int[] dataList = new int[JSONList.length()];
		
		for(int i = 0; i < JSONList.length(); i++) {
			dataList[i] = JSONList.getInt(i);
		}
		
		return dataList;
		
	}
	
	/**
	 * Calculates the sum of the partialDataList.
	 *
	 * @param partialDataList List of integers for calculation
	 * @param delay Delay between each computation
	 * @return Sum of the partial list
	 */
	public static int calculateSum(int[] partialDataList, int delay, boolean fault) {
		int sum = 0;
		Random randomNumber = new Random();
		
		// Convert delay into seconds
		int seconds = delay * 1000;
		try {
			for(int j : partialDataList) {
				sum += j;
				
				Thread.sleep(seconds);
			}
			
			// Simulates faulty node
			if(fault) {
				sum += randomNumber.nextInt(20 - 1 + 1) + 1;
			}
		}
		catch(InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		return sum;
		
	}
}
