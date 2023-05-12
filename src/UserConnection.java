import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.UUID;

/*** Single client, takes in user input from console ***/
public class UserConnection {
	
	Socket clientSocket;
	int clientNumber;
	
	public UserConnection(Socket clientSocket, int clientNumber) {
		this.clientSocket = clientSocket;
		this.clientNumber = clientNumber;
	}
	// reads multiple lines from server, exiting on an empty line
	private static void menuPrint(BufferedReader input) throws IOException {
		String line;
		while ((line = input.readLine()) != null) { 
			if(line.length() == 0) break;
			// print the line
			System.out.println("Server: " + line);
		}
	}
	
	public static void main(String[] args) throws Exception {
		try {
			// change to match destination address
			Socket socket = new Socket("127.0.0.1", 8888);
			
			// TODO this should be a loop similar to on server side, only breaking when user exits
			
			// input and output streams
			BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			DataOutputStream output = new DataOutputStream(socket.getOutputStream());
			// inputstream for user input
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			
			// input line
			String line;

			// waits for login menu
			menuPrint(input);

			// menu selection
			String selection;  
			// validate response (ideally this validation should be on server side, but is fine here)
			do {
				System.out.println("Please enter a number (1 or 2)...");
				selection = br.readLine();
			}
			while (!(selection.equals("1") || selection.equals("2")));
            
			// return selection to server
            output.writeBytes(selection + "\n");
            
            // options
            switch(selection) 
            {
            	// register user case
	            case "1":
	            	// print out the register user menu
	            	menuPrint(input);
	            	break;
	            
	            // login user case
	            case "2":
	            	// print out the login user menu
	            	boolean verified = false;
	            	do 
	            	{
	            		System.out.println(input.readLine());
	            		System.out.println("Please enter password...");
	            		
	            		// send password attempt
	            		String attempt = br.readLine();
	            		output.writeBytes(attempt + "\n");
	            		
	            		// check verification
	            		verified = input.read() != 0;
	            		
	            		// verification messages
	            		System.out.println(input.readLine());

	            	} while(!verified);
	            	break;
            }
            
            // TODO this isn't done yet
            // user commands
			String actionChoice="";
			while (!(actionChoice.equals("0") ||actionChoice.equals("1") || actionChoice.equals("2") || actionChoice.equals("3"))) {  //wait for valid response
				do{
				System.out.println("\nSelect your action:");
				System.out.println("0. Quit");
				System.out.println("1. Find average monthly maximum or minimum temperature of a given station id in a given year");
				System.out.println("2. Find yearly average maximum or minimum temperature of all the stations in a given year");
				System.out.println("3. Find month which has highest/lowest maximum temperature in a given year and station");
	            actionChoice = br.readLine();
				}
				while(!actionChoice.equals("0"));
				output.writeBytes(actionChoice+"\n");
			}
            
            // get the response from server
            line = input.readLine();
            System.out.println(line);

			// TODO move these closes to a finally block
			input.close();
			output.close();
			socket.close();
		} catch(Exception e){
			System.out.println(e);
		}
	}
}
