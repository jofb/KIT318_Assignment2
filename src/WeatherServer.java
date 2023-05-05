import java.net.*;
import java.io.*;
import java.util.*;
public class WeatherServer{

	public static void main(String[] args) throws Exception {
		/* args list
		 * 0: input path
		 * 1: output path
		 * 2: number of threads
		 * */
		
		String inputPath = args[0];
		String outputPath = args[1];

		// either manually set threads or leave to args
		int threads = Integer.parseInt(args[2]); 
		threads = 4; // manual thread settings
		
		// timing variables
		long oldTime = 0, newTime;
		// these are used to omit the connection time from the total time
		long timeConnectOne = 0, timeConnectTwo = 0;

		// final list of averages
		Map<String, Integer> averages = new HashMap<>();
		
		// map of all values with keys
	  	Map<String, List<Integer>> data = new HashMap<>();
	  	
	  	// list of maps, to be allocated to each thread
	  	List<Map<String, List<Integer>>> threadData = new ArrayList<>();

	  	// start timer
	  	oldTime = System.currentTimeMillis();

		// read in input from csv file, adapted from https://www.baeldung.com/java-csv-file-array
	  	try (Scanner scanner = new Scanner(new File(inputPath));) {
	  		while (scanner.hasNextLine()) {
	  		  	/* weather data
	  		  	 * 0: weather station id
	  		  	 * 1: date in format (YYYYMMDD)
	  		  	 * 2: temp type
	  		  	 * 3: temp value
	  		  	 * */
	  			// values on each line as a list
	  			List<String> values = new ArrayList<String>();
	  			try (Scanner rowScanner = new Scanner(scanner.nextLine())) {
	  				rowScanner.useDelimiter(",");
	  				while (rowScanner.hasNext()) {
	  					values.add(rowScanner.next());
	  				}
	  			}
	  			// we only care about the records covering TMAX
	  			if(values.get(2).equals("TMAX"))
	  			{
	  				// mapping all temperature values into unique keys based on station ID + month
	  				// generate a unique key, format: stationID, MM
	  				String key = values.get(0) + "," + values.get(1).substring(4,6);
	  		  		// if key doesn't already exist, create new list
	  		  		if(!data.containsKey(key))
	  		  			data.put(key, new ArrayList<Integer>());

	  		  		// then add our temperature value
	  	  			data.get(key).add(Integer.parseInt(values.get(3)));
	  			}
	  		}
	  	}

	  	// initializing maps in the threadData lists
	  	for(int j = 0; j < threads; j++)
	  	{
	  		threadData.add(j, new HashMap<>());
	  	}
	  	
	  	int i = 0;
	  	// iterate over entries, adapted from https://stackoverflow.com/questions/4234985/how-to-for-each-the-hashmap
	  	// allocates smaller maps for each thread
	  	for(Map.Entry<String, List<Integer>> entry : data.entrySet())
	  	{
	  		if(i > threads - 1)
	  			i = 0;
	  		threadData.get(i).put(entry.getKey(), entry.getValue());
		  	i++;

	  	}
	  	
		// socket stuff
		try {
			// start connection timer
		  	timeConnectOne = System.currentTimeMillis();
		  	
			// list of server threads
			List<ServerThread> serverThreads = new ArrayList<>();
			ServerSocket server = new ServerSocket(8888);
			int counter = 0;
			
			System.out.println("Server started ...");
			
			// wait for all workers to connect
			while(counter < threads) {
				counter++;
				Socket serverClient = server.accept();
				// add thread to list with client inside
				serverThreads.add(new ServerThread(serverClient, counter));
			}
			// end connection timer
			timeConnectTwo = System.currentTimeMillis();
			
			// start all workers sequentially
			for(ServerThread thread : serverThreads) {
				// start thread
				System.out.println(" >> Client No: " + thread.clientNumber + " started!");
				thread.setData(threadData.get(thread.clientNumber - 1));
				thread.start();
				
			}
			// wait for each thread to finish, then add its results to a list
			for(ServerThread thread : serverThreads) {
				thread.join();
				// aggregate averages from each thread
				averages.putAll(thread.getAverages());
			}
			
			server.close();
			
		} catch(Exception e) {
			System.out.println(e);
		}
		
		// write to csv file using file writer
		File csvOutput = new File(outputPath);
		FileWriter fileWriter = new FileWriter(csvOutput);
		
	  	for(Map.Entry<String, Integer> entry : averages.entrySet())
	  	{
	  		// small bit of formatting for csv file
	  		String line = entry.toString().replace('=', ',') + "\n";
	  		fileWriter.write(line);
	  	}
	  	
	  	fileWriter.close();
	  	
	  	// timing
	  	newTime = System.currentTimeMillis();
	  	
	  	// tiemWithConnection includes the time it took to connect to workers, time omits that
	  	long timeWithConnection = newTime - oldTime;
	  	long time = (newTime - oldTime) - (timeConnectTwo - timeConnectOne);

	  	System.out.println("Time Taken (w/ connection time): " + timeWithConnection + "ms");
	  	System.out.println("Time Taken (w/out connection time): " + time + "ms");

	}
}
