import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

/*** Single client, takes in user input from console ***/
public class UserConnection {

	private static Socket serverSocket;
	private static BufferedReader input;
	private static DataOutputStream output;
	
	public UserConnection() {

	}
	// reads multiple lines from server, exiting on an empty line
	private static void menuPrint(BufferedReader serverInput) throws IOException {
		String line;
		while ((line = serverInput.readLine()) != null) { 
			if(line.length() == 0) break;
			// print the line
			System.out.println(String.format("[%s] %s",  new SimpleDateFormat("HH:mm:ss").format(new Date()), line));
			//System.out.println("Server: " + line);
		}
	}
	
	// send an arbitrary number of user inputs to the server
	private static void multiLineUserInput(BufferedReader serverInput, BufferedReader userInput, DataOutputStream output) throws IOException
	{
		// get as many inputs as the server needs by having the server first send that number
		int inputs = serverInput.read();
		for(int i = 0; i < inputs; i++)
		{
			// first read in the question from server
			System.out.println(serverInput.readLine());
			// next send the user input
			output.writeBytes(userInput.readLine() + "\n");
		}
	}
	
	public static void main(String[] args) throws Exception {
		try {
			// change to match destination address
			serverSocket = new Socket("203.101.228.233", 9000);

			// input and output streams
			input = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
			output = new DataOutputStream(serverSocket.getOutputStream());
			// inputstream for user input
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

			// waits for login menu
			menuPrint(input);

			// menu selection
			String selection;  
			// validate response (ideally this validation should be on server side, but is fine here)
			do {
				System.out.println("Please enter a number (1 or 2)...");
				selection = br.readLine();
			}
			while (!(selection.equals("1") || selection.equals("2") || selection.equals("shutdown")));

			// return selection to server
            output.writeBytes(selection + "\n");
			if(selection.equals("shutdown")) return;
             
            // options
            switch(selection) 
            {
            	// register user case
	            case "1":
	            	// print out the register user menu
	            	menuPrint(input);
	            	System.out.println();
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

			System.out.println("Your query is being processed. Please wait a moment...");
            // get the response from server
            menuPrint(input);

		} catch(Exception e){
			System.out.println(e);
		} finally
		{
			input.close();
			output.close();
			serverSocket.close();
		}
	}
}
