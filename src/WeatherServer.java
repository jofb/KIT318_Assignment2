import java.net.*;
import java.io.*;
import java.util.*;
public class WeatherServer{

	public static void main(String[] args) throws Exception {
		
		// open up server on port
		ServerSocket server = new ServerSocket(8888);

		// TODO create work queue
		// TODO create request object
		// user connection
		try {
			// TODO also need a seperate thread just handling work requests
			
			// list of client threads
			List<ClientConnectionThread> serverThreads = new ArrayList<>();
			
			int counter = 0;
			
			System.out.println("Server started ...");
			
			// TODO this loop needs an exit condition
			// every time we want to accept a new user need a thread open
			while(true) {
				counter++;
				// wait for new connection from user
				Socket client = server.accept();
				
				// create new thread to handle client, then start the thread
				ClientConnectionThread thread = new ClientConnectionThread(client, counter);
				serverThreads.add(thread);
				thread.start();
			}		
		} catch(Exception e) {
			System.out.println(e);
		} finally {
			if(server != null) {
				server.close();
			}
		}
		
	}
	
	// TODO this will take some work unit/request and add it to the work queue
	public void addToWorkQueue() {
		
	}
}
