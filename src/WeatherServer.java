import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.SynchronousQueue;
public class WeatherServer{
	

	// queue of all work units/requests
	// TODO should we move this to the workHandler thread?
	// TODO take a look at the WorkUnit class
	static Queue<WorkUnit> workQueue = new SynchronousQueue<WorkUnit>();
	// handles the work nodes
	static WorkHandler workHandler = new WorkHandler(workQueue);
	
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
		
		// TODO remove this, admin password
		passwordList.add("password");
		
		// open up server on port
		ServerSocket server = new ServerSocket(8888);

		// user connection
		try {
			// list of client threads
			List<ClientConnectionThread> serverThreads = new ArrayList<>();
			
			List<String> Data = processData();
			HashMap<String, List<String>> dataByID = dataIDSplit(Data);
			HashMap<String, List<String>> dataByYear = dataYearSplit(Data);
			
//			for (String i : dataByYear.keySet()) {  //debug function
//				System.out.println("key: " + i + " value: " + dataByYear.get(i));
//			}
			
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
	
	//gets a line of data from the file, splits it by commas, gets the relevant data, and returns it as a list
	private static List<String> processData() throws FileNotFoundException {		
		Scanner sc = new Scanner(new File("C:\\Users\\adaml\\Downloads\\1863.csv"));  //CHANGE THIS TO YOUR FILE LOCATION
		List<String> allData = new ArrayList<String>();  //list of all our data
		
		do {			
			String[] splitLine; //temporary string array for when we split our csv's
			String relevantData = "";  //used to get the data into the format (id,date,value type,temp)

			splitLine = sc.nextLine().split(",");  //splits by commas
			for (int j = 0; j <= 3; j++) {
				relevantData = relevantData + splitLine[j] + ","; //get weather station, date, value type, temp
			}
			allData.add(relevantData);  //adds our data to the list
		} while (sc.hasNextLine() == true);  //loops until we've gotten to the end of the list
		return allData;
	}
	
	//splits the data so that it is a hashmap, with the key as the weather station ID and value of a list of strings,
	//each of which is our data in the format (id,date,value type,temp)
	private static HashMap<String, List<String>> dataIDSplit(List<String> data) {
		HashMap<String, List<String>> dataByID = new HashMap<String, List<String>>();  //data sorted by weather station ID
		
		for (String item:data) {
			String[] splitLine = item.split(",");  //temporary string array for split csv data
			List<String> newList = new ArrayList<String>();  //new list for if one is needed in the data hashmap
			
			if (dataByID.isEmpty()) {  //if the hashmap is completely empty				
				newList.add(item);  //adds our item to the empty list
				dataByID.put(splitLine[0], newList);  //adds to the hashmap. key is weather station and value is the list (which has just one data point here)
			} else {
				if (dataByID.containsKey(splitLine[0])) {  //if the weather station id is a key within the hashmap
					dataByID.get(splitLine[0]).add(item);  //gets the value associated with the station id (a list) and adds the new data to that list
				} else {
					newList.add(item);  //adds our item to the empty list
					dataByID.put(splitLine[0], newList);  //adds to the hashmap. key is weather station and value is the list (which has just one data point here)
				}
			}
		}
		return dataByID;
	}
	
	//splits the data so that it is a hashmap, with the key as the year and value of a list of strings,
	//each of which is our data in the format (id,date,value type,temp)
	//see dataIDSplit for all comments, as this is mostly the same code so I've only commented the different parts
	private static HashMap<String, List<String>> dataYearSplit(List<String> data) {
		HashMap<String, List<String>> dataByYear = new HashMap<String, List<String>>();  //data sorted by weather station ID
		
		for (String item:data) {
			String[] splitLine = item.split(",");
			List<String> newList = new ArrayList<String>();
			String year = splitLine[1].substring(0,4);  //string which gets the date and then the first 4 characters of the date, which is the year
			
			if (dataByYear.isEmpty()) {			
				newList.add(item);
				dataByYear.put(year, newList);  //now we use the year as the key, instead of id like in the other method. Everything else is the same.
			} else {
				if (dataByYear.containsKey(year)) {
					dataByYear.get(year).add(item);
				} else {
					newList.add(item);
					dataByYear.put(year, newList); 
				}
			}
		}
		return dataByYear;
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
	List<Integer> data; 
	Integer workId; // TODO might not be necessary
}
