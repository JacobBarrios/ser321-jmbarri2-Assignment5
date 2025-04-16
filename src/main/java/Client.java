import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import org.json.JSONObject;

public class Client {
	public static void main(String[] args) {
		String host = args[0];
		int port = Integer.parseInt(args[1]);
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		
		try(Socket server = new Socket(host, port);
			PrintWriter out = new PrintWriter(server.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(server.getInputStream()))
		) {
			System.out.println("Connected to server at Host: " + host + " Port: " + port);
		
			while(true) {
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
				
				int[] list = new int[size];
				for (int i = 0; i < size; i++) {
					System.out.printf("Enter item %d of %d\n", i, size);
					
					try {
						list[i] = Integer.parseInt(input.readLine());
					}
					catch(NumberFormatException e) {
						System.out.println("Item must be an integer");
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
							selecting = false;
						}
					}
					catch(NumberFormatException e) {
						System.out.println("Delay must be an integer");
					}
				}
				
				JSONObject message = new JSONObject();
				message.put("Type", "Data");
				message.put("List", list);
				message.put("Delay", delay);
				out.println(message);
				
				String stringResponse = in.readLine();
				JSONObject response = new JSONObject(stringResponse);
				System.out.println("Received response");
				
				if(response.getString("Type").equals("Result")) {
					int singleSumResult = response.getInt("Single");
					System.out.println("Result: " + singleSumResult);
				}
				
			}
		}
		catch(IOException e) {
			System.out.println("Connection error");
		}
	}
}
