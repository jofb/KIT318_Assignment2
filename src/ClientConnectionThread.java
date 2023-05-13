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
					output.writeBytes("Please input your password to be able to manage requests:\n");

					// password attempt
					String attempt = input.readLine();  

					// check if password exists
					successfulPassword = passwordList.contains(attempt);

					// output the verification attempt
					output.writeBoolean(successfulPassword);
					
					// verification message
					if(successfulPassword)
					{
						output.writeBytes("Your password has been verified. Continuing to the next menu...\n");
					} else {
						output.writeBytes("Incorrect password...\n");
					}
				}
				//from here goes to the "read command from user" section
			}
			
			
			/*** TODO Read command from user (this is where the loop would be) ***/
			/** TODO this should print out the options: view, create, stop request (or exit) */
			// create request gives you the three request type options, then takes in the input for the request
			// view request takes in a request id
			// stop request takes in a request id
			String actionChoice="";
			String requestType="";
			int stationId;
			int year;
			String maxMin;
			int id;
			while (!(actionChoice.equals("0") ||actionChoice.equals("1") || actionChoice.equals("2") || actionChoice.equals("3"))) { 
			do{
				output.writeBytes("0. Quit\n");
				output.writeBytes("1. View requests\n");
				output.writeBytes("2. Create request\n");
				output.writeBytes("3. Stop request\n");
				output.writeBytes("Select your action:\n\n");
				actionChoice = input.readLine();

				switch(actionChoice){
					case "1":
					id = Integer.parseInt(input.readLine());
					//WeatherServer.viewRequestStatus(id);
					output.writeBytes("Printing...\n");
					break;

					case "2":
					while (!(requestType.equals("1") || requestType.equals("2") || requestType.equals("3"))){
						output.writeBytes("1. Average monthly max/min temperature\n");
						output.writeBytes("2. Average yearly max/min temperature\n");
						output.writeBytes("3. Month with highest/lowest temperature\n");
						output.writeBytes("Choose a type of request\n\n");
						requestType=input.readLine();

						switch(requestType){
							case "1":
							output.writeBytes("Input station\n");
							stationId=Integer.parseInt(input.readLine());
							output.writeBytes("Input year\n");
							year=Integer.parseInt(input.readLine());
							output.writeBytes("Max or Min? (0 or 1)\n");
							maxMin = input.readLine();
							if(maxMin.equals("0")){ //execute commands
							} else if(maxMin.equals("1")){
							}
							break;
							case "2":
							output.writeBytes("Input year\n");
							year=Integer.parseInt(input.readLine());
							output.writeBytes("Max or Min? (0 or 1)\n");
							maxMin = input.readLine();
							if(maxMin.equals("0")){
							} else if(maxMin.equals("1")){
							}
							break;
							case "3":
							output.writeBytes("Input station\n");
							stationId=Integer.parseInt(input.readLine());
							output.writeBytes("Input year\n");
							year=Integer.parseInt(input.readLine());
							output.writeBytes("Max or Min? (0 or 1)\n");
							maxMin = input.readLine();
							if(maxMin.equals("0")){ //execute commands
							} else if(maxMin.equals("1")){
							}
							break;
						}
					}
					requestType="";
					break;

					case "3":
					output.writeBytes("Input request ID to stop\n");
					id = Integer.parseInt(input.readLine());
					output.writeBytes("Removing request...\n");
					//WeatherServer.stopRequest(id);
					break;
				}
			}
			while (!actionChoice.equals("0"));
		}
			// take command from user
			// e.g view request, create request, stop request, etc
			// execute the command
			// all commands will have to go back through the server to get the information from workers, so 
			// feel free to make getters/setters on the server
			
			//use verifiedPassword here, as this has been confirmed to be right. Put in a map with request id and stuff?
			
			// TODO change this to the correct request with correct params
			Query q = WeatherServer.addRequest(QueryType.CREATE, "");
			
			// wait for query to be notified (when the response is ready)
			synchronized(q)
			{
				q.wait();
			}

			output.writeBytes("Response: " + q.response.responseBody + "\n");
			requestQueue.remove(q);
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