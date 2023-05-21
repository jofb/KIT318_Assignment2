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
    
    public static Map<String, Map<String, List<String>>> dataByYearID;

	public static void main(String[] args) throws Exception {
		
		TimestampedPrint timestampOut = new TimestampedPrint(System.out);
		
		System.setOut(timestampOut);
		
		queryQueue = new LinkedBlockingQueue<Query>();
		workHandler = new WorkHandler(queryQueue);
		
		// initialize dataset
		
		// can use command line arg or set manually
		System.out.println("Processing datasets...");
		dataByYearID = new HashMap<String, Map<String, List<String>>>();
		for(String arg : args)
		{
			List<String> data = processData(arg);
			HashMap<String, List<String>> dataByID = dataSplit(data, 0);
			String year = data.get(0).split(",")[1].substring(0, 4); // disgusting but works
			
			dataByYearID.put(year, dataByID);
			int size = 0;
			for(Map.Entry<String, List<String>> entry : dataByID.entrySet())
			{
				size += entry.getValue().size();
			}
			System.out.println("Processed dataset with size " + size);
		}
		System.out.println("Datasets initialized!");

		// starting up the work handler thread
		workHandler.start();
		
		// wait for workers to start
		synchronized(workHandler)
		{
			workHandler.wait();
		}
		
		// TODO remove this, admin password
		passwordList.add("password");
		
		// open up server on port
		ServerSocket server = new ServerSocket(9000);

		// user connection
		try {
			// list of client threads
			List<ClientConnectionThread> serverThreads = new ArrayList<>();

			int counter = 0;
			
			System.out.println("Server started...");

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
	
	/**
	 * Maps a dataset to a given delimiter key. (station_id, date, value type, temperature)
	 * @param data The data to split/map
	 * @param delimiter The delimiter to split by (0 - 3), as defined in processData
	 * @return The new mapped dataset
	 */
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
			
			// special case for year (not great)
			if(delimiter == 1) key = key.substring(0, 4);
			
			// create new list
			if(!mappedData.containsKey(key))
			{
				mappedData.put(key, new ArrayList<String>());
			}
			mappedData.get(key).add(item);
		}
		return mappedData;
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
