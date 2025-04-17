import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.net.Socket;

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
					System.out.println("[DEBUG] Received data from Leader");
					
					int sum = calculateSum(partialDataList);
					System.out.println("[DEBUG] Result: " + sum);
					
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
	 * @return Sum of the partial list
	 */
	public static int calculateSum(int[] partialDataList) {
		int sum = 0;
		
		for(int j : partialDataList) {
			sum += j;
		}
		
		return sum;
		
	}
}
