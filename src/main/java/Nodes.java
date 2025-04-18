import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.net.Socket;

import java.util.Random;

/**
 * Nodes class handles the distributed sum calculations
 *
 * @author Jacob Barrios
 * @version 1.0
 */
public class Nodes {
	
	/**
	 * Main that takes arguments from gradle run command for the Leader host, and the Node port
	 *
	 * @param args Contains Leader host, and Node port
	 */
	public static void main(String[] args) {
		String host = args[0];
		int port = Integer.parseInt(args[1]);
		boolean fault = Boolean.parseBoolean(args[2]);
		Random randomNumber = new Random();
		System.out.println("[DEBUG] Port used: " + port);
		
		try(Socket server = new Socket(host, port);
			PrintWriter out = new PrintWriter(server.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(server.getInputStream()))) {
			System.out.println("[DEBUG] Connected to Leader");
			
			while(true) {
				try {
					String stringComputation = in.readLine();
					JSONObject computation = new JSONObject(stringComputation);
					JSONArray arrayDataList = computation.getJSONArray("List");
					int[] partialDataList = convertJSONArray(arrayDataList);
					int delay = computation.getInt("Delay");
					System.out.println("[DEBUG] Received data from Leader");
					
					// Starts calculating the sum of the given list
					int sum = calculateSum(partialDataList, delay);
					System.out.println("[DEBUG] Result: " + sum);
					
					// Simulates faulty node
					if(fault) {
						sum += randomNumber.nextInt(20 - 1 + 1) + 1;
					}
					
					JSONObject result = new JSONObject();
					result.put("Type", "Result");
					result.put("Result", sum);
					out.println(result);
					System.out.println("[DEBUG] Sent result to Leader");
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
	}
	
	/**
	 * Converts the giving JSONArray into int[]
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
	 * Calculates the sum of the partialDataList
	 *
	 * @param partialDataList List of integers for calculation
	 * @param delay Delay between each computation
	 * @return Sum of the partial list
	 */
	public static int calculateSum(int[] partialDataList, int delay) {
		int sum = 0;
		// Convert delay into seconds
		int seconds = delay * 1000;
		try {
			for(int j : partialDataList) {
				sum += j;
				
				Thread.sleep(seconds);
			}
		}
		catch(InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		return sum;
		
	}
}
