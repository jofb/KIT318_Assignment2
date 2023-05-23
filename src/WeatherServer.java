import java.net.*;
import java.text.SimpleDateFormat;
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
	private static final String DATA_DIR = "data";
	
	static int queryCounter = 0;
	
	public static boolean priorityProcessing = false;


	// queue of all queries
	static Queue<Query> queryQueue;
	
	// handles the work nodes
	static WorkHandler workHandler;
	
	// list of passwords for registered users
    static List<String> passwordList = new ArrayList<String>();
    
    //public static Map<String, Map<String, List<String>>> dataByYearID;
    
    private static boolean runningServer = true;
    
    private static ServerSocket server;
    
    // https://stackoverflow.com/questions/7768071/how-to-delete-directory-content-in-java
    public static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if(files!=null) { //some JVMs return null for empty dirs
            for(File f: files) {
                if(f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

	public static void main(String[] args) throws Exception {
		
		TimestampedPrint timestampOut = new TimestampedPrint(System.out);
		
		System.setOut(timestampOut);
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("(0) for first-come first-serve processing (1) for priority based processing");
		priorityProcessing = br.read() != 0;
		
		queryQueue = new LinkedBlockingQueue<Query>();
		
		workHandler = new WorkHandler(priorityProcessing, queryQueue);
		
		// initialize dataset
		
		// can use command line arg or set manually
		System.out.println("Indexing datasets...");
		File requestDir = new File(DATA_DIR);
		// clear requests cache
		requestDir.mkdir();

		// check years we want to include and assert that they exist
		for(String arg : args)
		{
			File f = new File(requestDir.toPath() + "/" + arg);
			if(f.isDirectory()) System.out.println(arg + " dataset indexed!");
		}
		System.out.println("Datasets ready!");
		
		System.out.println("Clearing requests cache...");
		File cache = new File("requests");
		if(cache.isDirectory()) deleteFolder(cache);
		System.out.println("Requests cache cleared!");

		//if(priorityProcessing) return;
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
		server = new ServerSocket(9000);

		// user connection
		try {
			// list of client threads
			List<ClientConnectionThread> serverThreads = new ArrayList<>();

			int counter = 0;
			
			System.out.println("Server started...");

			// every time we want to accept a new user need a thread open
			while(runningServer) {
				counter++;
				// wait for new connection from user
				Socket client = server.accept();
				
				// create new thread to handle client, then start the thread
				ClientConnectionThread thread = 
						new ClientConnectionThread(client, counter, queryQueue);
				serverThreads.add(thread);
				thread.start();
			}		
		} catch(Exception e) {
			System.out.println(e);
		} finally {
			try {
				// this is a bit goofy, but lets the work handler shut down its workers
				workHandler.runningWorkHandler = false;
				synchronized(queryQueue)
				{
					queryQueue.notify(); 
				}
				System.out.println("Shutting down server...");
			} catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public static List<File> getAllStations(String year)
	{
		File dir = new File(DATA_DIR + "/" + year);
		File[] files = dir.listFiles();
		return Arrays.asList(files);
	}
	public static List<String> getStationData(File path)
	{
		List<String> data = null;
		try {
			data = processData(path.toString());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return data;
	}
	

	public static List<String> getStationData(String year, String station)
	{
		List<String> data = null;
		try {
			data = processData(DATA_DIR + "/" + year + "/" + station + ".csv");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return data;
	}
	
	public static void shutdownServer()
	{
		runningServer = false;
		if(server != null) {
			try {
				server.close();
			} catch (IOException e) {
				e.printStackTrace();
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
}

class TimestampedPrint extends PrintStream {

	private final SimpleDateFormat format;
	
	public TimestampedPrint(PrintStream out) {
		super(out);
		format = new SimpleDateFormat("HH:mm:ss");
	}
	
	@Override
	public void println(String x)
	{
		String timestamp = format.format(new Date());
		super.println(String.format("[%s] %s", timestamp, x));
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
