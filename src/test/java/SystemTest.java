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
	
	@DisplayName("Make connections")
	public void setUp(boolean fault) {
		try {
			for (int i = 0; i < 3; i++) {
				int port = 8081 + i;
				
				// Mimic: gradle runNodes -PPort=8081 -PHost=localhost -PFault=false
				String gradleCommand = System.getProperty("os.name").toLowerCase().contains("win") ? "gradlew.bat" : "./gradlew";
				ProcessBuilder pb;
				if(i == 0 && fault) {
					pb = new ProcessBuilder(
							gradleCommand,
							"runNodes",
							"-PHost=localhost",
							"-PPort=" + port,
							"-PFault=" + true
					);
				}
				else {
					pb = new ProcessBuilder(
							gradleCommand,
							"runNodes",
							"-PHost=localhost",
							"-PPort=" + port,
							"-PFault=" + false
					);
				}
				
				pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
				pb.redirectError(ProcessBuilder.Redirect.INHERIT);
				nodeProcesses.add(pb.start());
				
				Thread.sleep(500); // brief delay to let each Node start
			}
		
			// Connect client to Leader
			client = new Socket("localhost", 8080);
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@DisplayName("Close all connections")
	public void tearDown() throws IOException {
		for (Process p : nodeProcesses) {
			p.destroy();
			if (p.isAlive()) {
				p.destroyForcibly();
			}
		}
		
		if (client != null) client.close();
	}
	
	@Test
	@DisplayName("Send data test")
	public void exampleList() {
		setUp(false);
		try {
			PrintWriter outClient = new PrintWriter(client.getOutputStream(), true);
			BufferedReader inClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
			
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
			int singleSumResult = response.getInt("Single");
			int distributedSumResult = response.getInt("Distributed");
			int singleTime = response.getInt("SingleTime");
			int distributedTime = response.getInt("DistributedTime");
			System.out.println("Single Time: " + singleTime);
			System.out.println("Distributed Time: " + distributedTime);
			
			assertEquals(66, singleSumResult);
			assertEquals(66, distributedSumResult);
			
			tearDown();
			
		}
		catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Test
	@DisplayName("Tests with a faulty node")
	public void faultyNode() {
		setUp(true);
		try {
			PrintWriter outClient = new PrintWriter(client.getOutputStream(), true);
			BufferedReader inClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
			
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
			int singleSumResult = response.getInt("Single");
			int distributedSumResult = response.getInt("Distributed");
			int singleTime = response.getInt("SingleTime");
			int distributedTime = response.getInt("DistributedTime");
			System.out.println("Single sum: " + singleSumResult);
			System.out.println("Distributed sum: " + distributedSumResult);
			System.out.println("Single Time: " + singleTime);
			System.out.println("Distributed Time: " + distributedTime);
			
			assertNotEquals(singleSumResult, distributedSumResult);
			
			tearDown();
			
		}
		catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
}
