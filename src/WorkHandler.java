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
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Base64;
import java.util.Collections;

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
	
	public boolean runningWorkHandler = true;

	WorkHandler(Queue<Query> requestQueue) {
		this.queryQueue = requestQueue;
	}

	public void run() {
		// ensure that the output folder exists
		File requestDir = new File(REQUESTS_DIR);
		requestDir.mkdir();
		
		/* AUTHENTICATION INFORMATION FOR NECTAR ACCOUNTS */
		String[] auth1 = {
				"jtwylde@utas.edu.au", 
				"M2I1YzA4NWY4MmFhMmRk",
				"1a58f808e7c34eab90db080bb6fe67fa",
				"cd5e6ad6-46af-48a1-bdbc-f12c0db1a69a",
				"kit318_assignment_ssh",
				"216ad4cd-52a3-4718-94ab-bae4bddcc043"
		};
		String[] auth2 = {
				"aflood@utas.edu.au", 
				"MWJmNDFmMTkwZTk0M2Fk",
				"3aea2efef75046f98f78cb3961388169",
				"d76daf9e-1150-4e89-90b7-d6d98e7d7b21",
				"tut7",
				"a37dc379-f34f-46b5-8633-ce3cc6a5e473"
		};
		String[] auth3 = {
				"email", 
				"password",
				"projectID",
				"imageID",
				"keypairname",
				"securitygroupID"
		};
		
		/* INITIALIZE ALL AUTH INFORMATION FOR WORKER NODES*/
		WorkerNode w1 = new WorkerNode(false, auth1); // TODO make w1 true
		WorkerNode w2 = new WorkerNode(true, auth2);
		WorkerNode w3 = new WorkerNode(true, auth2);
		WorkerNode w4 = new WorkerNode(false, auth3);
		WorkerNode w5 = new WorkerNode(false, auth3);

		workers = new ArrayList<WorkerNode>(Arrays.asList(w1, w2, w3, w4, w5));
		// initialize workers
		System.out.println("Initializing workers...");
		for(WorkerNode worker : workers)
		{
			if (!worker.active) continue;
			worker.initVM();
		}
		// wait for each IP to be assigned
		for(WorkerNode worker : workers)
		{
			if (!worker.active) continue;
			worker.assignIP();
			System.out.println("Worker initialized with IP " + worker.ipAddress);
		}

		// wait an additional 10 seconds
		try {
			Thread.sleep(40000); 
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("All workers initialized!");
		// and tell the main thread to start up
		synchronized(this)
		{
			notify();
		}
		// ITE00100550
		while(true)
		{
			// wait for updates to request queue
			synchronized(queryQueue)
			{
				try {
					boolean waitTime = !workQueue.isEmpty();
					// if there is no work in the queue, and no results expected, just wait for query queue
					for(WorkerNode worker : workers)
					{
						// has work, need results
						if(worker.workingOn != null)
						{
							waitTime = true;
							break;
						}
					}
					// if needed, update every 10s, otherwise only cycle when there are queries
					if(waitTime) queryQueue.wait(10000);
					else queryQueue.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			if(!runningWorkHandler) break;
			
			for(Query query : queryQueue)
			{
				System.out.println("Processing query " + query.queryId + "(" + query.queryType.toString() + ")");
				Map<String, String> params = query.queryParams;
				int id;
				String response = "";
				
				// parse each query and handle
				try {

					switch(query.queryType)
					{
					case CREATE:
						id = createRequest(params);
						response = String.format("A new request has now been created, with request ID [%d]", id);
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
				} catch(Exception e)
				{
					response = "There was an error while processing your query!";
				}

				// once done, create a QueryResponse object and attach to the query
				query.response = new QueryResponse(response);

				// notify the connection thread that this query has a response ready
				synchronized(query)
				{
					query.notify();
				}
			}

			//poll workers for results
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

						// if its null, the request has been deleted, discard the results
						if(currentRequest != null)
						{
							// get back the filename as well, set that to the key
							String key = input.readLine();
							// formats it nicely
							key = key.split("/")[1].split(".txt")[0];
							key = key.substring(key.indexOf("_") + 1);
							System.out.println(String.format("Received [%s] %s result", requestid, key));
							int result = input.read();
							currentRequest.results.put(key, result);
						}
						
						// set worker to not working
						worker.available = true;
					}
					
					s.close();					
				} // TODO should wrap these in functions
				catch (UnknownHostException e) {
					//e.printStackTrace();
					System.out.println("Connection refused! Restarting worker " + worker.ipAddress + "...");
					// reassign the work to different worker
					WorkUnit w = worker.workingOn;
					if(w != null) workQueue.add(w);
					worker.available = false;
					worker.active = false;
					// TODO restart the vm?
					worker.shutDownServer();
				} catch (IOException e) {
					//e.printStackTrace();
					System.out.println("Connection refused! Restarting worker " + worker.ipAddress + "...");
					// reassign the work to different worker
					WorkUnit w = worker.workingOn;
					if(w != null) workQueue.add(w);
					worker.available = false;
					worker.active = false;
					// TODO restart the vm?
					worker.shutDownServer();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			if(workQueue.isEmpty()) continue;
			
			for(WorkerNode worker : workers)
			{
				if(!worker.active && !worker.available && !workQueue.isEmpty())
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
				worker.workingOn = work;
				if(work == null) break;
				
				// connect to worker
				try {
					Socket s = worker.connect();
					DataOutputStream output = new DataOutputStream(s.getOutputStream());
					
					output.write(2);
					
					output.write(work.requestType);
					output.write(work.requestId);
					output.writeBytes(work.metadata + "\n");
					output.writeBytes(work.data + "\n");
					
					s.close();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				worker.available = false;
			}
			// check workqueue size compared to workers active
			// if larger then start new workers
			// else delte workers 
		}
		
		shutdownWorkers();
	}
	
	private void shutdownWorkers()
	{
		for(WorkerNode worker : workers)
		{
			if(!worker.active) break;
			System.out.println("Shutting down worker with IP " + worker.ipAddress);
			worker.shutDownServer();
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
		String metadata = "";
		List<String> station;
		
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
			option = "TMAX";
			int highLow = Integer.parseInt(params.get("minMax")); // this needs to be put on the request object

			metadata = Integer.toString(highLow);
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
		
		Request r = new Request(requestId, work.entrySet().size());
		if(requestType == 3) r.expectedResults = 1; // hacky fix 
		
		r.requestType = requestType;
		r.params = params;
		r.results = new HashMap<String, Integer>();
		requests.put(requestId, r);
		
		System.out.println(String.format("Generated %d work units", work.entrySet().size()));

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
			w.metadata = metadata;
			
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
	int expectedResults;
	int requestType;
	Map<String, String> params;
	Map<String, Integer> results;
	
	long time;
	String start_date;
	String end_date;
	DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	
	Request(int id, int expectedResults) 
	{
		this.id = id;
		this.expectedResults = expectedResults;
		time = System.currentTimeMillis();
		
		LocalDateTime now = LocalDateTime.now();
		start_date = dtf.format(now);
	}
	
	String formattedRequestType()
	{
		String y = params.get("year");
		String id = params.get("stationId");
		String minMax = params.get("minMax");
		
		String[] requests = {
				String.format("Average monthly %s temperature for station %s in %s", (minMax == "0"? "min" : "max"), id, y),
				String.format("Average yearly %s temperature in %s", (minMax == "0"? "min" : "max"), y),
				String.format("Month with %s temperature for %s in %s", (minMax == "0"? "lowest" : "highest"), id, y)
		};

		return requests[requestType - 1];
	}

	//added a new function, in case you want to use the other one for different things
	String checkStatus() {
		int status = (results.size() * 100) / expectedResults;
		
		// if status is complete
		if (status == 100) {
			if(requestType == 3)
			{
				// quickly aggregate results
				// very hacky
				// find max or min
				int minMax = Integer.parseInt(params.get("minMax"));
				String k = "";
				int v;
				if(minMax == 0) v = Integer.MAX_VALUE;
				else v = Integer.MIN_VALUE;
				for(Entry<String, Integer> entry : results.entrySet())
				{
					String key = entry.getKey();
					Integer value = entry.getValue();
					
					if((minMax == 0 && value < v) || (minMax == 1 && value > v))
					{
						v = value;
						k = key;
					}
				}
				// clear results and only grab the one that counts
				results.clear();
				results.put(k, v);
			}
			time = System.currentTimeMillis() - time;
			time = time / 1000;  //to seconds
			time = time / 60;  //to minutes (not going to bother with hours)
			end_date = dtf.format(LocalDateTime.now());
			// TODO format the results nicer
			Map<String, Integer> tree = new TreeMap<String, Integer>(results);
			String resultsString = tree.toString();
			resultsString = resultsString.replace("=", ": ").replace("{", "[ ").replace("}", " ]");

			// would be cool if we could give info about their query here but don't think it's possible?
			String statement = String.format("Request: " + formattedRequestType() + "\n" +
					"Results: %s\n" +
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
	String metadata;
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
	String serverId;
	int port = 9000;
	
	// TODO pass in the auth information
	WorkerNode(boolean start, String[] _auth) {
		// email, password, projectID, imageID, keyPairName, securityID
		auth = new HashMap<String, String>();
		auth.put("email", _auth[0]);
		auth.put("password", _auth[1]);
		auth.put("projectID", _auth[2]);
		auth.put("imageID", _auth[3]);
		auth.put("keypairName", _auth[4]);
		auth.put("securityID", _auth[5]);
		
		active = start;
		available = start;
	}
	
	// image id
	// key pair name
	// security group id
	public void initVM() {
		// authenticate openstack builder
		try {
			os = OSFactory.builderV3()//Setting up openstack client with  -OpenStack factory
					.endpoint("https://keystone.rc.nectar.org.au:5000/v3") //Openstack endpoint
					.credentials(auth.get("email"), auth.get("password"),Identifier.byName("Default")) //Passing credentials
					.scopeToProject( Identifier.byId(auth.get("projectID")))//Project id
					.authenticate();//verify the authentication
		} catch(Exception e)
		{
			// supress the logger warning
		}

		// initializes work node, compiles and runs
		String script = Base64.getEncoder().encodeToString((
				"#!/bin/bash\n" + 
				"sudo mkdir /home/ubuntu/temp\n" + 
				"sudo apt-get update\n" + 
				"sudo apt-get upgrade\n" +
				"cd /home/ubuntu\n" +
				"./run_worker.sh").getBytes());// encoded with Base64
		ServerCreate server = Builders.server()//creating a VM server
				.name("KIT318-Worker-Node")//VM or instance name
				.flavor("406352b0-2413-4ea6-b219-1a4218fd7d3b")//flavour id
				.image(auth.get("imageID"))// -image id
				.keypairName(auth.get("keypairName"))//key pair name
				.addSecurityGroup(auth.get("securityID"))	//Security group ID (allow SSH)
				.userData(script)
				.build();//build the VM with above configuration
			
		Server booting=os.compute().servers().boot(server);
		serverId = booting.getId();
	}
	
	public void shutDownServer()
	{
		os.compute().servers().delete(serverId);
	}
	
	public void restartWorker()
	{
		shutDownServer();
		// wait for shutdown
	}

	public void assignIP()
	{
		String ip = os.compute().servers().get(serverId).getAccessIPv4();
		// wait for IP to be assigned
		while(ip==null||ip.length()==0) {
			try {
				Thread.sleep(1000);
				//System.out.println("Waiting");
			} catch(InterruptedException e) {e.printStackTrace();}
			ip=os.compute().servers().get(serverId).getAccessIPv4();
		}
		ipAddress = ip;
	}
	
	public Socket connect() throws UnknownHostException, IOException {
		if(!active) return null;
		return new Socket(ipAddress, port);
		//return null;
	}
	
}
