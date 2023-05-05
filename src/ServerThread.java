import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ServerThread extends Thread {
	
	Socket serverClient;
	int clientNumber;
	
	private volatile Map<String, List<Integer>> data = new HashMap<>();
	private volatile Map<String, Integer> averages = new HashMap<>();
	
	ServerThread(Socket inSocket, int counter) {
		serverClient = inSocket;
		clientNumber = counter;
	}
	
	public void run() {
		try {
			DataOutputStream output = new DataOutputStream(serverClient.getOutputStream());
			BufferedReader input = new BufferedReader(new InputStreamReader(serverClient.getInputStream()));
			
			// loop through given data and pass to worker
			for(Map.Entry<String, List<Integer>> entry : data.entrySet())
			{
				// format the out string
				String out = entry.getValue().toString().replaceAll("\\s|\\[|\\]", "");
				// output the list of numbers
				output.writeBytes(out + "\n");
				// then add average to the return map
				averages.put(entry.getKey(), Integer.parseInt(input.readLine()));
			}

			// closing streams
			input.close();
			output.close();
			serverClient.close();
		} catch(Exception e) {
			System.out.println(e);
		} finally {
			System.out.println("Client - " + clientNumber + " exit!");
		}
	}

	// getters and setters for input/output
	public Map<String, Integer> getAverages()
	{
		return averages;
	}
	
	public void setData(Map<String, List<Integer>> d) {
		data = d;
	}
}