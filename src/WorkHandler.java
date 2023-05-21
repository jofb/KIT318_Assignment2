import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Base64;
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.ServerCreate;
import org.openstack4j.model.image.Image;
import org.openstack4j.openstack.OSFactory;



/*** This thread will be in charge of handling all worker nodes and reading from the work queue ***/
class WorkHandler extends Thread {
	private static final String REQUESTS_DIR = "requests";

	Socket serverClient;
	int clientNumber;
	
	int requestCounter = 0;
	
	List<WorkerNode> workers;

	// queue of queries
	Queue<Query> queryQueue;
	
	// map from requestid to request object
	Map<Integer, Request> requests = new HashMap<Integer, Request>();


	Queue<WorkUnit> workQueue = new LinkedBlockingQueue<WorkUnit>();

	WorkHandler(Queue<Query> requestQueue) {
		this.queryQueue = requestQueue;
	}

	public void run() {
		// ensure that the output folder exists
		File requestDir = new File(REQUESTS_DIR);
		requestDir.mkdir();
		
		/* INITIALIZE ALL AUTH INFORMATION FOR WORKER NODES*/
		WorkerNode w1 = new WorkerNode(true, "email", "password","projectID","imageID","keypair","securitygroup");
		WorkerNode w2 = new WorkerNode(true, "email", "password","projectID","imageID","keypair","securitygroup");
		WorkerNode w3 = new WorkerNode(true, "email", "password","projectID","imageID","keypair","securitygroup");
		WorkerNode w4 = new WorkerNode(false, "email", "password","projectID","imageID","keypair","securitygroup");
		WorkerNode w5 = new WorkerNode(false, "email", "password","projectID","imageID","keypair","securitygroup");

		//workers = new ArrayList<WorkerNode>(Arrays.asList(w1, w2, w3, w4, w5));
		w1.ipAddress = "131.217.174.74";
		w1.port = 9000;
		workers = new ArrayList<WorkerNode>();
		workers.add(w1);
		
		/* we also want to initialize the first three virtual machines */
		/* ideally this should pause the main thread while they're starting */
		/* this can be done with synchronized(this) notify();, and on main thread do synchronized(thread) thread.wait(); */
//		w1.initVM();
//		w2.initVM();
//		w3.initVM();
//		
		// and ideally we have some way of telling when they're done (can't remember if there's a way)
		// ITE00100550
		while(true)
		{
			System.out.println("Waiting for Requests...");
			// wait for updates to request queue
			synchronized(queryQueue)
			{
				try {
					// this will wait until requestQueue is notified, OR 1 minute TODO change to 5 (or w/e is appropriate)
					queryQueue.wait(60000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}


			for(Query query : queryQueue)
			{
				// parse each query and handle

				Map<String, String> params = query.queryParams;
				int id;
				String response = "";
				
				switch(query.queryType)
				{
				case CREATE:
					id = createRequest(params);
					//response to be output, which reminds the user of the request ID
					response = "A new query has now been created, and is associated with request ID " + id;
					break;
				case VIEW:
					id = Integer.parseInt(params.get("requestId"));
					
					String currentStatus = requests.get(id).checkStatus();
					response = currentStatus;
					break;
				case STOP:
					id = Integer.parseInt(params.get("requestId"));
					// cancel this id
					for(WorkUnit work : workQueue)
					{
						if(work.requestId == id)
						{
							workQueue.remove(work);
						}
					}
					response = String.format("Request [%d] has been deleted", id);
					requests.remove(id);
					break;
				}
				// once done, create a QueryResponse object and attach to the query
				query.response = new QueryResponse(response);

				// notify the connection thread that this query has a response ready
				synchronized(query)
				{
					query.notify();
				}
			}

			//TODO fix this start that I've made
			for (WorkerNode worker : workers) {
				if (!worker.active) {
					continue;
				}
				
				// try to connect to worker node
				try {
					Socket s = worker.connect();
					// input and output streams
					BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));
					DataOutputStream output = new DataOutputStream(s.getOutputStream());
					// to check status
					output.write(1);
					boolean finished = input.read() != 0;
					
					if (finished) {						
						int requestid = input.read();
						// finds current request in map	
						Request currentRequest = requests.get(Integer.valueOf(requestid));
						
						int result = input.read();
						
						// if its null, the request has been deleted, discard the results
						if(currentRequest != null)
						{
							// i think instead of just storing numbners, could also associate the particular workunit with each result, not sure
							currentRequest.results.add(input.read());  //adds the int to the results (add for each loop if more than 1 result?)
						}
						
						// set worker to not working
						worker.available = true;
					}
					
					s.close();					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
					// reassign the work to different worker
					WorkUnit w = worker.workingOn;
					if(w != null) workQueue.add(w);
					worker.available = false;
					worker.active = false;
					// restart the vm?
					
				}
			}
			
			if(workQueue.isEmpty()) continue;
			
			for(WorkerNode worker : workers)
			{
				if(!worker.active && !worker.available)
				{
					continue;
				}
				// first check if we're meant to be using priority (some bool on weather server likely)
				// if yes then can reduce based on highest priority
				// and set that to work
				// WorkUnit work = workQueue.stream().reduce((w1, w2) -> w1.priority >= w2.priority ? w1 : w2);
				
				// this is either first come first serve, or priority
				WorkUnit work = workQueue.poll();
				
				// if there's nothing left in the queue, just leave
				if(work == null) break;
				
				// connect to worker
				try {
					Socket s = worker.connect();
					DataOutputStream output = new DataOutputStream(s.getOutputStream());
					
					output.write(2);
					
					output.write(work.requestType);
					output.write(work.requestId);
					output.writeBytes(work.data + "\n");
					
					s.close();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				worker.available = false;
				worker.workingOn = work;
			}
			// check workqueue size compared to workers active
			// if larger then start new workers
			// else delte workers 
		}
	}
	private int createRequest(Map<String, String> params)
	{
		// get the request type (to be used in a switch statement)
		int requestType = Integer.parseInt(params.get("requestType"));
		
		int requestId = requestCounter;
		requestCounter++;
		
		params.put("requestId", Integer.toString(requestId));

		Map<String, List<Integer>> work = new HashMap<String, List<Integer>>();
		
		String option;
		String id;
		List<String> station;
		int steps = 1;
		
		switch(requestType)
		{
		// avg monthly temp by id-year
		case 1:
			id = params.get("stationId");

			option = (params.get("minMax") == "0")? "TMIN" : "TMAX";

			// grab the dataset by station id
			// List<String> station = WeatherServer.dataByID.get(id);
			station = WeatherServer.dataByYearID.get(params.get("year")).get(id);

			// iterate over station
			for(String data : station)
			{
				String[] line = data.split(",");

				if(!line[2].equals(option)) continue;
				// get parts of the line
				String month = line[1].substring(4, 6);
				int temp = Integer.parseInt(line[3]);
				
				// form the key from the month and id
				String key = id + "_" + month;
				
				if(!work.containsKey(key))
				{
					work.put(key, new ArrayList<Integer>());
				}
				
				work.get(key).add(temp);
			}
			break;
			// yearly average by year
		case 2:
			option = (params.get("minMax") == "0")? "TMIN" : "TMAX";

			Map<String, List<String>> year = WeatherServer.dataByYearID.get(params.get("year"));
			
			for(Entry<String, List<String>> entry : year.entrySet())
			{
				String stationId = entry.getKey();
				
				station = entry.getValue();
				
				for(String data : station)
				{
					String[] line = data.split(",");
					if(line[2].equals(option)) continue;

					int temp = Integer.parseInt(line[3]);
					
					String key = stationId;
					
					if(!work.containsKey(key))
					{
						work.put(key, new ArrayList<Integer>());
					}
					work.get(key).add(temp);
				}
			}

			break;
			// month which has highest/lowest max temperature in given year and station
		case 3:
			// TODO not done yet
			id = params.get("stationId");
			steps = 2;
			option = "TMAX";
			int highLow = Integer.parseInt(params.get("minMax")); // this needs to be put on the request object

			station = WeatherServer.dataByYearID.get(params.get("year")).get(id);
			
			for(String data : station)
			{
				String[] line = data.split(",");
				
				if(!line[2].equals(option)) continue;

				String month = line[1].substring(4, 6);
				
				int temp = Integer.parseInt(line[3]);
				
				String key = id + "_" + month;
				
				if(!work.containsKey(key))
				{
					work.put(key, new ArrayList<Integer>());
				}
				work.get(key).add(temp);
			}
			break;
		}
		
		Request r = new Request(requestId, steps, work.entrySet().size());
		r.results = new ArrayList<Integer>();
		requests.put(requestId, r);
		
		System.out.println(work.entrySet().size());

		for (Map.Entry<String, List<Integer>> entry : work.entrySet())
		{
			// use these to write to file
			String key = entry.getKey();
			List<Integer> values = entry.getValue();

			String filename = String.format("%s/%d_%s.txt", REQUESTS_DIR, requestId, key);

			// write to csv file using file writer
			File output = new File(filename);
			try {
				FileWriter fileWriter = new FileWriter(output);
				
				String line = values.toString().replace(" ", "");
				// need to exclude the square brackets from the list.toString()
				fileWriter.write(line.substring(1, line.length() - 1));
				
				fileWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			// create new work unit, pass in file name, then add to queue
			WorkUnit w = new WorkUnit();
			w.requestId = requestId;
			w.requestType = requestType;
			w.data = filename;
			
			switch(requestType)
			{
			case 1:
			case 3:
				w.priority = 1;
				break;
			case 2:
				w.priority = 5;
				break;
			}
			workQueue.add(w);
		}
		
		return requestId;
	}
}

class Request {
	int id;
	int step;
	int totalSteps;
	int expectedResults;
	List<Integer> results;
	
	long time;
	String start_date;
	String end_date;
	DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	
	Request(int id, int steps, int expectedResults) 
	{
		this.id = id;
		this.totalSteps = steps;
		this.expectedResults = expectedResults;
		time = System.currentTimeMillis();
		
		LocalDateTime now = LocalDateTime.now();
		start_date = dtf.format(now);
		
		this.step = 1;
	}

	//added a new function, in case you want to use the other one for different things
	String checkStatus() {
		int status = (results.size() * 100) / expectedResults;
		
		// if status is complete
		if (status == 100) {
			time = System.currentTimeMillis() - time;
			time = time / 1000;  //to seconds
			time = time / 60;  //to minutes (not going to bother with hours)
			end_date = dtf.format(LocalDateTime.now());
			String resultsString = results.toString();

			// would be cool if we could give info about their query here but don't think it's possible?
			String statement = String.format("The result for your query is: %s\n" +
					"Start Date: %s, End Date: %s, Time taken (minutes): %d\n" +
					"Total Cost ($3/minute): $%d", resultsString, start_date, end_date, time, 3 * time);			
			return statement;
		} else {
			String statement = "Your query is still being processed. It is currently " + status +
					"% complete";
			return statement;
		}
	}
}

/* Class to store information about a requests work unit */
class WorkUnit {
	Integer requestId;
	Integer requestType;
	String data;
	int priority = 1;
}


// TODO in here we could also create a method to create the VM
// TODO rather than port it should be storing an ip address since we will be working with VMs
// using ports for now on local machines since all on same machine
class WorkerNode {

	boolean active;
	boolean available;
	WorkUnit workingOn;
	
	Map<String, String> auth;
	
	OSClientV3 os = null;
	
	String ipAddress;
	int port = 9000;
	
	// TODO pass in the auth information
	WorkerNode(boolean start, String email, String password, String projectID, String imageID, String keyPairName, String securityID) {
		auth = new HashMap<String, String>();
		auth.put("email", email);
		auth.put("password", password);
		auth.put("projectID", projectID);
		auth.put("imageID", imageID);
		auth.put("keypairName", keyPairName);
		auth.put("securityID", securityID);
		
		active = start;
		available = start;
	}
	
	// image id
	// key pair name
	// security group id
	public void initVM() {
		// authenticate openstack builder
		os = OSFactory.builderV3()//Setting up openstack client with  -OpenStack factory
				.endpoint("https://keystone.rc.nectar.org.au:5000/v3") //Openstack endpoint
				.credentials(auth.get("email"), auth.get("password"),Identifier.byName("Default")) //Passing credentials
				.scopeToProject( Identifier.byId(auth.get("projectID")))//Project id
				.authenticate();//verify the authentication
		
		
		String script = Base64.getEncoder().encodeToString(("#!/bin/bash\n" + "sudo mkdir /home/ubuntu/temp").getBytes());//encoded with Base64. Creates a temporary directory
		ServerCreate server = Builders.server()//creating a VM server
				.name("Test")//VM or instance name
				.flavor("406352b0-2413-4ea6-b219-1a4218fd7d3b")//flavour id
				.image(auth.get("imageID"))// -image id
				.keypairName(auth.get("keypairName"))//key pair name
				.addSecurityGroup(auth.get("securityID"))	//Security group ID (allow SSH)
				.userData(script)
				.build();//build the VM with above configuration
			
		Server booting=os.compute().servers().boot(server);
		ipAddress = booting.getAccessIPv4();
	}
	
	public Socket connect() {
		if(!active) return null;
		try {
			return new Socket(ipAddress, port);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
}
