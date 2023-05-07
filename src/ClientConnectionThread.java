import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*** Handles the client connection, reads input, authenticates user via server ***/
class ClientConnectionThread extends Thread {
	
	Socket serverClient;
	int clientNumber;

	ClientConnectionThread(Socket inSocket, int counter) {
		serverClient = inSocket;
		clientNumber = counter;
	}
	
	public void run() {
		try {
			// output and input streams
			DataOutputStream output = new DataOutputStream(serverClient.getOutputStream());
			BufferedReader input = new BufferedReader(new InputStreamReader(serverClient.getInputStream()));
			
			output.writeUTF("hey there");
			
			/*** TODO Authenticate the user ***/
			// either register new user
			// or let them login
			
			
			/*** TODO Read command from user ***/
			// take command from user
			// e.g view request, create request, stop request, etc
			// execute the command
			// all commands will have to go back through the server to get the information from workers, so 
			// feel free to make getters/setters on the server
			
			
			// closing streams
			input.close();
			output.close();
		} catch(Exception e) {
			System.out.println(e);
		} finally {
			try {
				serverClient.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("Client - " + clientNumber + " exit!");
		}
	}

}