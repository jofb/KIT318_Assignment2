import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/*** Handles the client connection, reads input, authenticates user via server ***/
class ClientConnectionThread extends Thread {
	
	Socket serverClient;
	int clientNumber;
	private List<String> passwordList;  //list of all passwords

	ClientConnectionThread(Socket inSocket, int counter) {
		serverClient = inSocket;
		clientNumber = counter;
	}
	
	public void run() {
		try {
			// output and input streams
			DataOutputStream output = new DataOutputStream(serverClient.getOutputStream());
			BufferedReader input = new BufferedReader(new InputStreamReader(serverClient.getInputStream()));
			
			//registers new user, or lets them login
			//i have used only a password to login, no id or username
			passwordList = WeatherServer.getPasswordList();  //gets the list of passwords from the server (may want to use setter here?)
			String verifiedPassword;  //string of the confirmed password, which the server has verified is in its system
			
			//menu to register or login
			output.writeBytes("Welcome! Are you... \n");
			output.writeBytes("1. A new user to be registered\n");
			output.writeBytes("2. A returning user needing to login\n");
			output.writeBytes("stop\n");  //stop is used to signify that this will be the last output written for this chunk of output
			String selection = input.readLine();
			//System.out.println(selection);  //debug code

			if (selection.equals("1")) {  //user wants to be registered
				output.writeBytes("You will now be registered. Below will be your password which you can use to login in the future\n");
				String password = UUID.randomUUID().toString();  //code to get a big, unique string
				passwordList.add(password);
				WeatherServer.setPasswordList(passwordList);  //password is added to local list, and the server list is then updated with the new one
				output.writeBytes(password + "\n");
				output.writeBytes("stop\n");
				verifiedPassword = password;  //this is the successful password, which can then be used later
				//from here goes to the "read command from user" section
			} else if (selection.equals("2")) {  //user wants to login
				boolean successfulPassword = false;  //boolean for if password is a success
				
				while (!successfulPassword) {  //while we don't have a verified password
					output.writeBytes("Please input your password to be able to manage requests:\n");
					output.writeBytes("stop\n");
					String attempt = input.readLine();  //password the user wants to login with
					//System.out.println(attempt);  //debug code
					for (String password:passwordList) {  //goes through password list to check if it's in there
						if (attempt.equals(password)) {
							successfulPassword = true;
						}
					}
					if (successfulPassword) {  //it's a success
						output.writeBytes("Your password has been verified. Continuing to the next menu...\n");
						output.writeBytes("DONE\n");
						output.writeBytes("stop\n");
						verifiedPassword = attempt;  //verified password
						successfulPassword = true;  //exits loop because we've logged in correctly
					} else {
						output.writeBytes("Incorrect password...\n");
						output.writeBytes("stop\n");
					}
				}
				//from here goes to the "read command from user" section
			}
			
			
			/*** TODO Read command from user (this is where the loop would be) ***/
			// take command from user
			// e.g view request, create request, stop request, etc
			// execute the command
			// all commands will have to go back through the server to get the information from workers, so 
			// feel free to make getters/setters on the server
			
			//use verifiedPassword here, as this has been confirmed to be right. Put in a map with request id and stuff?
			
			
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
	
	//getter for password list (don't know if this is necessary)
	public List<String> getPasswordList() {
		return passwordList;
	}

	//setter for password list (don't know if this is necessary)
	public void setPasswordList(List<String> passwordList) {
		this.passwordList = passwordList;
	}

}