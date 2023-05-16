import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


enum QueryType { CREATE, VIEW, STOP };

public class WeatherServer{
	
	static int queryCounter = 0;


	// queue of all queries
	static Queue<Query> queryQueue;
	
	// handles the work nodes
	static WorkHandler workHandler;
	
	// list of passwords for registered users
    static List<String> passwordList = new ArrayList<String>();

	public static void main(String[] args) throws Exception {
		
		queryQueue = new LinkedBlockingQueue<Query>();
		workHandler = new WorkHandler(queryQueue);

		// starting up the work handler thread
		workHandler.start();
		
		// TODO remove this, admin password
		passwordList.add("password");
		
		// open up server on port
		ServerSocket server = new ServerSocket(8888);

		// user connection
		try {
			// list of client threads
			List<ClientConnectionThread> serverThreads = new ArrayList<>();
			
//			List<String> Data = processData("C:\\Users\\adaml\\Downloads\\1863.csv");
//			HashMap<String, List<String>> dataByID = dataIDSplit(Data);
//			HashMap<String, List<String>> dataByYear = dataYearSplit(Data);
			
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
				ClientConnectionThread thread = 
						new ClientConnectionThread(client, counter, queryQueue);
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

    //getter for password list
    public static List<String> getPasswordList() {
        return passwordList;
    }
    
    //setter for password list
    public static void setPasswordList(List<String> passwordList) {
        WeatherServer.passwordList = passwordList;
    }

    /* add query to queue */
    public static void addQuery(Query q)
    {
    	queryCounter++;
    	// add to queue
    	q.queryId = queryCounter;
    	queryQueue.add(q);
    	
    	synchronized(queryQueue)
    	{
    		queryQueue.notify();
    	}
    }
	
	//gets a line of data from the file, splits it by commas, gets the relevant data, and returns it as a list
	private static List<String> processData(String path) throws FileNotFoundException {		
		Scanner sc = new Scanner(new File(path));  
		
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
		
		// data = (id, date, value type, temp) (every single one)
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
	
	/* Splits dataset based on given delimiter (0 - 3) */
	private static HashMap<String, List<String>> dataSplit(List<String> data, int delimiter) throws Exception
	{
		// this could be better (yucky manual 0 or 3) but is ok
		if(delimiter < 0 || delimiter > 3) throw new Exception("Invalid delimiter when splitting data");
		
		HashMap<String, List<String>> mappedData = new HashMap<String, List<String>>();

		for(String item : data)
		{
			// create the key based on the delimiter
			String[] line = item.split(",");
			String key = line[delimiter];
			
			// create new list
			if(!mappedData.containsKey(key))
			{
				mappedData.put(key, new ArrayList<String>());
			}
			mappedData.get(key).add(item);
		}
		return mappedData;
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
}

class QueryResponse {
	String responseBody;
	
	QueryResponse(String _response)
	{
		responseBody = _response;
	}
}

class Query {
	int queryId;
	QueryType queryType;
	Map<String, String> queryParams;
	QueryResponse response;
	
	Query(int id)
	{
		queryId = id;
	}
	
	Query(int id, QueryType type, Map<String, String> params)
	{
		queryId = id;
		queryType = type;
		queryParams = params;
	}
}
