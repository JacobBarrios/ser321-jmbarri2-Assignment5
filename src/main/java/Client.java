import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.net.Socket;

import org.json.JSONObject;

/**
 * Client class handles the communication between the Leader to calculate the sum of integers
 * in a list
 *
 * @author Jacob Barrios
 * @version 1.0
 */
public class Client {
	
	/**
	 * Main that takes arguments from gradle run command for the host, and port of the Leader
	 * Handles input of the user for list of integer, and delay time
	 *
	 * @param args Contains Leader host, and port
	 */
	public static void main(String[] args) {
		String host = args[0];
		int port = Integer.parseInt(args[1]);
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		
		try(Socket server = new Socket(host, port);
			PrintWriter out = new PrintWriter(server.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(server.getInputStream()))) {
			System.out.println("Connected to Leader at Host: " + host + ", Port: " + port);
		
			while(true) {
				// Gets response from Leader
				System.out.println("[DEBUG] Waiting for Leader response");
				String stringResponse = in.readLine();
				JSONObject response = new JSONObject(stringResponse);
				System.out.println("[DEBUG] Received Leader response");
				
				// Outputs response based on the type sent
				if(response.getString("Type").equals("Start")) {
					System.out.println("Message: " + response.getString("Message"));
				}
				else if(response.getString("Type").equals("Result")) {
					int singleSumResult = response.getInt("Single");
					int distributedSumResult = response.getInt("Distributed");
					System.out.println("Single: " + singleSumResult);
					System.out.println("Distributed: " + distributedSumResult);
				}
				
				// Get the desired size of the array
				int size = 0;
				boolean selecting = true;
				while(selecting) {
					System.out.println("Enter size of array: ");

					try {
						size = Integer.parseInt(input.readLine());

						if(size < 0) {
							System.out.println("Size must be positive");
						}
						else {
							selecting = false;
						}
					}
					catch(NumberFormatException e) {
						System.out.println("Size must be an integer");
					}
				}

				// Get the integers for list
				int[] list = new int[size];
				for (int i = 0; i < size; i++) {
					System.out.printf("Enter item %d of %d\n", i, size - 1);

					try {
						list[i] = Integer.parseInt(input.readLine());
					}
					catch(NumberFormatException e) {
						System.out.println("Item must be an integer");
						// If the input is not an integer go back an index
						i--;
					}
				}

				int delay = 0;
				selecting = true;
				while(selecting) {
					System.out.println("Enter desired delay time");

					try {
						delay = Integer.parseInt(input.readLine());

						if(delay < 0) {
							System.out.println("Delay must be positive");
						}
						else {
							// Exits loop when input is correct
							selecting = false;
						}
					}
					catch(NumberFormatException e) {
						// Input is not an integer
						System.out.println("Delay must be an integer");
					}
				}
				
				// Builds and sends the request
				JSONObject request = new JSONObject();
				request.put("Type", "Data");
				request.put("List", list);
				request.put("Delay", delay);
				System.out.println("Sent data to Leader");
				out.println(request);
			}
		}
		catch(IOException e) {
			System.out.println("[DEBUG] Connection error");
		}
	}
}
