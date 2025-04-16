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
		Scanner scanner = new Scanner(System.in);
		
		try(Socket server = new Socket(host, port);
			PrintWriter out = new PrintWriter(server.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(server.getInputStream()))
		) {
			System.out.println("Connected to server at Host: " + host + " Port: " + port);
		
			while(true) {
				int size = 0;
				boolean selecting = true;
				while(selecting) {
					try {
						System.out.println("Enter size of array: ");
						size = scanner.nextInt();
						
						if(size < 0) {
							System.out.println("Size must be a positive int");
						}
						else {
							selecting = false;
						}
					}
					catch(NumberFormatException e) {
						System.out.println("Size must be a positive int");
						scanner.next();
					}
				}
				
				int[] list = new int[size];
				for(int i = 0; i < size; i++) {
					try {
						System.out.printf("Enter item %d of %d\n", i, size);
						list[i] = scanner.nextInt();
						
					}
					catch(NumberFormatException e) {
						System.out.println("Item should be an int");
						scanner.next();
						i--;
					}
				}
				
				int delay = 0;
				selecting = true;
				while(selecting) {
					try {
						System.out.println("Enter desired delay time");
						delay = scanner.nextInt();
						
						if(delay < 0) {
							System.out.println("Delay should be a positive integer");
						}
						else {
							selecting = false;
						}
					}
					catch(NumberFormatException e) {
						System.out.println("Delay should be a positive integer");
						scanner.next();
					}
				}
				
				JSONObject message = new JSONObject();
				message.put("Type", "Data");
				message.put("List", list);
				message.put("Delay", delay);
				
				out.println(message.toString());
				
				
			}
		}
		catch(IOException e) {
			System.out.println("Connection error");
		}
	}
}
