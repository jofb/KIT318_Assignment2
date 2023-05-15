import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Scanner;

/*** Handles the client connection, reads input, authenticates user via server ***/
class ClientConnectionThread extends Thread {
	
	private Socket serverClient;
	private int clientNumber;
	private Queue<Query> requestQueue;
	private Lock lock;
	private Condition condition;
	
	private List<String> passwordList;  //list of all passwords
	
	
	ClientConnectionThread(Socket inSocket, int counter, Queue<Query> _requestQueue) {
		serverClient = inSocket;
		clientNumber = counter;
		requestQueue = _requestQueue;
	}
	
	public void run() {
		try {
			// output and input streams
			DataOutputStream output = new DataOutputStream(serverClient.getOutputStream());
			BufferedReader input = new BufferedReader(new InputStreamReader(serverClient.getInputStream()));
			
			// password list & verification
			passwordList = WeatherServer.getPasswordList();  
			String verifiedPassword;  
			
			// menu to register or login
			output.writeBytes("Welcome! Are you... \n");
			output.writeBytes("1. A new user to be registered\n");
			output.writeBytes("2. A returning user needing to login\n");
			output.writeBytes("\n");  
			
			// get the user selection
			String selection = input.readLine();
			
			// registration
			if (selection.equals("1")) {
				// generate a new password
				String password = UUID.randomUUID().toString();
				passwordList.add(password);
				// update global password list
				WeatherServer.setPasswordList(passwordList); 

				// write new password to user
				output.writeBytes("You will now be registered. Below is your password which you can use to login in the future\n");
				output.writeBytes(password + "\n");
				output.writeBytes("\n");
				verifiedPassword = password;  //this is the successful password, which can then be used later
			} 
			// login
			else if (selection.equals("2")) 
			{
				// if password has been verified
				boolean successfulPassword = false;
				
				while (!successfulPassword) {
					output.writeBytes("Please enter password...\n");

					// password attempt
					String attempt = input.readLine();  

					// check if password exists
					successfulPassword = passwordList.contains(attempt);

					// output the verification attempt
					output.writeBoolean(successfulPassword);
					
					// verification message
					if(successfulPassword)
					{
						output.writeBytes("Your password has been verified.\n");
					} else {
						output.writeBytes("Incorrect password...\n");
					}
				}
				//from here goes to the "read command from user" section
			}
			
			/* Command selection */

			Query query = new Query(0);
			
			String actionChoice="";
			Map<String, String> queryParams = new HashMap<String, String>();
			
			boolean validCommand = false;
			// print out the first menu
			output.writeBytes("Select your action...\n");
			output.writeBytes("0. Quit\n");
			output.writeBytes("1. View request\n");
			output.writeBytes("2. Create request\n");
			output.writeBytes("3. Stop request\n\n");
			
			// validate the first menu input
			while(!validCommand)
			{
				actionChoice = input.readLine();
				validCommand = (actionChoice.equals("0") ||actionChoice.equals("1") || actionChoice.equals("2") || actionChoice.equals("3"));
				output.writeBoolean(validCommand);
				if(!validCommand)
				{
					output.writeBytes("Please enter a valid command\n");
				} else {
					output.writeBytes("\n");
				}
			}
			// at this point actionChoice is now valid, so continue
			switch(actionChoice)
			{
			case "1":
				output.writeBytes("Enter a request ID...\n");
				queryParams.put("requestId", input.readLine());
				query.queryType = QueryType.VIEW;
				break;
			case "2":
				query.queryType = QueryType.CREATE;
				// need to validate command again
				validCommand = false;
				// print second menu
				output.writeBytes("Choose a request type...\n");
				output.writeBytes("1. Average monthly max/min temperature\n");
				output.writeBytes("2. Average yearly max/min temperature\n");
				output.writeBytes("3. Month with highest/lowest temperature\n\n");

				// validate inner command
				while(!validCommand)
				{
					actionChoice = input.readLine();
					validCommand = (actionChoice.equals("0") ||actionChoice.equals("1") || actionChoice.equals("2") || actionChoice.equals("3"));
					output.writeBoolean(validCommand);
					if(!validCommand)
					{
						output.writeBytes("Please enter a valid command\n");
					} else {
						output.writeBytes("\n");
					}
				}
				// now validated
				switch(actionChoice)
				{
				case "1":
					// no input validation here
					// this is the number of inputs we need from user
					output.write(3);
					output.writeBytes("Enter a station ID: \n");
					queryParams.put("stationId", input.readLine());
					output.writeBytes("Enter a year: \n");
					queryParams.put("year", input.readLine());
					output.writeBytes("Minimum (0) or maximum (1) temperatures? \n");
					queryParams.put("minMax", input.readLine());
					break;
				case "2":
					output.write(2);
					output.writeBytes("Enter a year: \n");
					queryParams.put("year", input.readLine());
					output.writeBytes("Minimum (0) or maximum (1) temperatures? \n");
					queryParams.put("minMax", input.readLine());
					break;
				case "3":
					output.write(3);
					output.writeBytes("Enter a station ID: \n");
					queryParams.put("stationId", input.readLine());
					output.writeBytes("Enter a year: \n");
					queryParams.put("year", input.readLine());
					output.writeBytes("Lowest (0) or highest (1) temperatures? \n");
					queryParams.put("minMax", input.readLine());
					break;
				}
				break;
			case "3":
				output.writeBytes("Enter a request ID...\n");
				queryParams.put("requestId", input.readLine());
				query.queryType = QueryType.STOP;
				break;
			}
			query.queryParams = queryParams;
			
			// take command from user
			// e.g view request, create request, stop request, etc
			// execute the command
			// all commands will have to go back through the server to get the information from workers, so 
			// feel free to make getters/setters on the server

			WeatherServer.addQuery(query);
			
			// wait for query to be notified (when the response is ready)
			synchronized(query)
			{
				query.wait();
			}

			// TODO can change this to format response body
			output.writeBytes("Response: " + query.response.responseBody + "\n");
			requestQueue.remove(query);
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
	public static void menu(Scanner sc, DataOutputStream output) {
		
	}
}