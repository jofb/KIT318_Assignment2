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
	
	// send an arbitrary number of user inputs to the server
	private static void multiLineUserInput(BufferedReader serverInput, BufferedReader input, DataOutputStream output) throws IOException
	{
		// get as many inputs as the server needs by having the server first send that number
		int inputs = serverInput.read();
		for(int i = 0; i < inputs; i++)
		{
			// first read in the question from server
			System.out.println(serverInput.readLine());
			// next send the user input
			output.writeBytes(input.readLine() + "\n");
		}
	}
	
	public static void main(String[] args) throws Exception {
		try {
			// change to match destination address
			Socket socket = new Socket("203.101.231.239", 9000);
			
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

	            		// send password attempt
	            		String attempt = br.readLine();
	            		output.writeBytes(attempt + "\n");
	            		
	            		// check verification
	            		verified = input.read() != 0;
	            		
	            		// verification messages
	            		System.out.println(input.readLine());

	            	} while(!verified);
	            	// if there was more to the verification, do it here
	            	break;
            }
            
            // TODO this isn't done yet
            // user commands
			String actionChoice="";

			boolean validCommand = false;
			
			menuPrint(input);
			
			while(!validCommand)
			{
				// user input
				actionChoice = br.readLine();
				output.writeBytes(actionChoice + "\n");
				
				// check if valid 
				validCommand = input.read() != 0;
				
				// response message
				System.out.println(input.readLine());
			}
			// it is now a valid command
			
			switch(actionChoice)
			{
			case "1":
			case "3":
				// get the user input for one line
				System.out.println(input.readLine());
				output.writeBytes(br.readLine() + "\n");
				break;
				
			case "2":
				// print out a second menu
				menuPrint(input);
				// and validate the command
				validCommand = false;
				while(!validCommand)
				{
					actionChoice = br.readLine();
					output.writeBytes(actionChoice + "\n");
					
					// check if valid
					validCommand = input.read() != 0;
					
					// response
					System.out.println(input.readLine());
				}
				// now have a valid command
				// input multiple lines (arbitrary number)
				multiLineUserInput(input, br, output);
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
