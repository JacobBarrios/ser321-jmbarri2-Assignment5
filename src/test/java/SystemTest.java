import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class SystemTest {
	Socket client;
	List<Process> nodeProcesses = new ArrayList<>();
	
	@DisplayName("")
	public void setUp() {}
	
	@AfterEach
	@DisplayName("Close all connections")
	public void tearDown() throws IOException {}
	
	@Test
	@DisplayName("Send data test")
	public void exampleList() {
		PrintWriter outClient = null;
		BufferedReader inClient = null;
		
		try {
			client = new Socket("localhost", 8080);
			outClient = new PrintWriter(client.getOutputStream(), true);
			inClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
			System.out.println("Made in out client");
			// Read initial welcome message
			String startMessage = inClient.readLine();
			System.out.println("Client received: " + startMessage);
			
			int[] exampleList = new int[12];
			for(int i = 1; i < exampleList.length; i++) {
				exampleList[i] = i;
			}
			System.out.println("Example list created");
			
			JSONObject request = new JSONObject();
			request.put("Type", "Data");
			request.put("List", exampleList);
			request.put("Delay", 1);
			
			outClient.println(request);
			System.out.println("Sent request");
			
			// Read response
			String stringResponse = inClient.readLine();
			JSONObject response = new JSONObject(stringResponse);
			System.out.println("Response to string: " + response);
			int singleSumResult = response.getInt("Single");
			int distributedSumResult = response.getInt("Distributed");
			int singleTime = response.getInt("SingleTime");
			int distributedTime = response.getInt("DistributedTime");
			System.out.println("Single Time: " + singleTime);
			System.out.println("Distributed Time: " + distributedTime);
			
			assertEquals(66, singleSumResult);
			assertEquals(66, distributedSumResult);
			
		}
		catch(IOException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if(client != null) {client.close();}
				if(outClient != null) outClient.close();
				if(inClient != null) inClient.close();
			}
				catch(IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	@Test
	@DisplayName("Tests with a faulty node")
	public void faultyNode() {
		PrintWriter outClient = null;
		BufferedReader inClient = null;
		
		try {
			client = new Socket("localhost", 8080);
			outClient = new PrintWriter(client.getOutputStream(), true);
			inClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
			
			// Read initial welcome message
			String startMessage = inClient.readLine();
			System.out.println("Client received: " + startMessage);
			
			int[] exampleList = new int[12];
			for(int i = 1; i < exampleList.length; i++) {
				exampleList[i] = i;
			}
			
			JSONObject request = new JSONObject();
			request.put("Type", "Data");
			request.put("List", exampleList);
			request.put("Delay", 1);
			
			outClient.println(request);
			
			// Read response
			String stringResponse = inClient.readLine();
			JSONObject response = new JSONObject(stringResponse);
			
			assertEquals(response.getString("Type"), "Error");
			
			tearDown();
			
		}
		catch(IOException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if(client != null) {client.close();}
				if(outClient != null) outClient.close();
				if(inClient != null) inClient.close();
			}
			catch(IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}