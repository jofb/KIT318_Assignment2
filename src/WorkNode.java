import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/* WorkNode will continually wait for connection, and then complete the given work when it receives one */
public class WorkNode {

	// reads multiple lines from server and returns as a List of strings
	private static List<String> multiLineRead(BufferedReader input) throws IOException {
		List<String> lines = new ArrayList<String>();
		List<Integer> d = new ArrayList<Integer>();
		String line;
		while ((line = input.readLine()) != null) { 
			if(line.length() == 0) break;
			// print the line
			lines.add(line);
		}
		return lines;
	}
	
	public void main(String[] args) throws Exception
	{
		// the worker node is a server in of itself, it waits for connections from the weather server
		ServerSocket worker = new ServerSocket(8888);
		
		Socket weatherServer;
		BufferedReader input;
		DataOutputStream output;
		
		// TODO this needs some sort of exit condition
		while(true)
		{
			// wait for connection from server
			weatherServer = worker.accept();
			
			// input and output streams
			input = new BufferedReader(new InputStreamReader(weatherServer.getInputStream()));
			output = new DataOutputStream(weatherServer.getOutputStream());
			
			// parse the request type
			// for now lets assume the first response is the request type
			int requestType = input.read();
			
			/************************** TODO DOWNLOADING FILE **********************************/
			/* all of the input here can be changed to reflect the address of the file to download + name */
			
			// and then once we've downloaded, move to processing
			
			// now lets take in our list of strings, then convert to integers
			String line = input.readLine();
			String[] numberStrings = line.split(",");
			
			// once we have our input, close the socket
			weatherServer.close();
			
			List<Integer> numbers = new ArrayList<Integer>();
			for(String s : numberStrings) {
				numbers.add(Integer.parseInt(s));
			}

			int result = 0;
			// TODO should change this
			// 0 for avg, 1 for max, 2 for min
			switch(requestType)
			{
			case 0:
				int sum = 0;
				for(int n : numbers) {
					sum += n;
				}
				int avg = sum / numbers.size(); // TODO get this output out of the switch scope
				result = avg;
				break;
			case 1:
				break;
			case 2:
				break;
			}
			
			// the work is now complete, wait for a request from server again to respond with results
			weatherServer = worker.accept();
			
			output.writeBytes(Integer.toString(result) + "\n");
			
			//perhaps here, before we close the server again, check if we need to keep checking?
			weatherServer.close();
		}	
	}
}
