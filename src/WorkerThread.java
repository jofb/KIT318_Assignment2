import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class WorkerThread extends Thread {
	
	Socket clientSocket;
	int clientNumber;
	
	public WorkerThread(Socket clientSocket, int clientNumber) {
		this.clientSocket = clientSocket;
		this.clientNumber = clientNumber;
	}
	
	public void run()  {
		try {
			BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());
	
			// wait for input list
			while(true)
			{
				// read the line
				String line = input.readLine();
				if(line == null)
					break;

				// split it based on commas
				String[] numbers = line.split(",");

				// compute average
				int sum = 0;
				for(String s : numbers) {
					sum += Integer.parseInt(s);
				}				
				int avg = sum / numbers.length;				
				
				// then return to master
				output.writeBytes(avg + "\n");
			}

		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}
