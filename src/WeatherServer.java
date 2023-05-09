import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.SynchronousQueue;
public class WeatherServer{
	
	// handles the work nodes
	static WorkerThread workHandler = new WorkerThread();
	
	// queue of all work units/requests
	// TODO should we move this to the workHandler thread?
	// TODO take a look at the WorkUnit class
	static Queue<WorkUnit> workQueue = new SynchronousQueue<WorkUnit>();

	// list of passwords for registered users
    static List<String> passwordList = new ArrayList<String>();

    //getter for password list
    public static List<String> getPasswordList() {
        return passwordList;
    }
    
    //setter for password list
    public static void setPasswordList(List<String> passwordList) {
        WeatherServer.passwordList = passwordList;
    }
	
	public static void main(String[] args) throws Exception {

		// TODO create work queue
		// TODO create request object
		
		// starting up the work handler thread
		workHandler.run();
		
		// open up server on port
		ServerSocket server = new ServerSocket(8888);

		// user connection
		try {
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
				// break; add this if you just want one connection and then the server close
			}		
		} catch(Exception e) {
			System.out.println(e);
		} finally {
			if(server != null) {
				server.close();
			}
		}
		
	}
	
	// TODO
	public void cancelRequest() {
		// remove all from work queue
		
		// inform worker thread
	}
	
	/* Views the status of the request
	 * id: the id of the request to check
	 *  */
	// TODO
	public void viewRequestStatus(int id) {
		// check how many of the work units have been completed, compare to total
	}
	
	// TODO this will take some work unit/request and add it to the work queue
	public void addToWorkQueue() {
		// generate request id?
		
		// add to queue
	}
	
	// TODO get the work queue, change return type
	public Queue<WorkUnit> getWorkQueue() {
		return workQueue;
	}
	
}

/* Class to store information about a requests work unit */
class WorkUnit {
	Integer requestId;
	Integer requestType;
	List<Integer> data; // TODO is this always an integer?
	Integer workId; // TODO might not be necessary
}
