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
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;

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

	int requestCounter = 0;
	
	List<WorkerNode> workers;

	// queue of queries
	Queue<Query> queryQueue;
	
	// map from requestid to request object
	Map<Integer, Request> requests = new HashMap<Integer, Request>();

	Queue<WorkUnit> workQueue;
	
	public boolean runningWorkHandler = true;

	WorkHandler(boolean priority, Queue<Query> requestQueue) {
		this.queryQueue = requestQueue;
		
		if(priority)
		{
			workQueue = new PriorityQueue<WorkUnit>(new WorkComparator());
		}
		else workQueue = new LinkedBlockingQueue<WorkUnit>();
	}

	public void run() {
		// ensure that the output folder exists
		File requestDir = new File(REQUESTS_DIR);
		requestDir.mkdir();
		
		/* AUTHENTICATION INFORMATION FOR NECTAR ACCOUNTS */
		String[] auth1 = {
				"jtwylde@utas.edu.au", // email
				"M2I1YzA4NWY4MmFhMmRk", // password
				"1a58f808e7c34eab90db080bb6fe67fa", //project id
				"065fb181-3a13-4b66-b6fb-5c47ecd86fe2", // image id
				"kit318_assignment_ssh", // keypair
				"216ad4cd-52a3-4718-94ab-bae4bddcc043" // security id
		};
		String[] auth2 = {
				"aflood@utas.edu.au", 
				"MWJmNDFmMTkwZTk0M2Fk",
				"3aea2efef75046f98f78cb3961388169",
				"aff7d076-c640-4fc9-a1f2-171dc389c4c5",
				"tut7",
				"a37dc379-f34f-46b5-8633-ce3cc6a5e473"
		};
		String[] auth3 = {
			"vpcleng@utas.edu.au", 
			"NTE2NWU3OGIwZmZjNGRl",
			"9a115bd605554e74a34b0339e4bb850e",
			"ef7538ea-0696-4036-a4e8-b6b3042db922",
			"kit318",
			"3bb7cb71-1985-42d5-abdf-acf1349be007"
		};
		
		/* INITIALIZE ALL AUTH INFORMATION FOR WORKER NODES*/
		WorkerNode w1 = new WorkerNode(true, auth1); 
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

		// wait an additional 60 seconds
		try {
			Thread.sleep(60000); 
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("All workers initialized!");
		// and tell the main thread to start up
		synchronized(this)
		{
			notify();
		}
		// ITE00100550, AE000041196
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
					// if needed, update every 5s, otherwise only cycle when there are queries
					if(waitTime) queryQueue.wait(5000);
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
						workQueue.removeIf(w -> w.requestId == id);
						response = String.format("Request [%d] has been deleted", id);
						requests.remove(id);
						break;
					}
				} catch(Exception e)
				{
					response = "There was an error while processing your query!";
					e.printStackTrace();
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
							String file = input.readLine();
							// formats it nicely
							String key = file.split("/")[1].split(".txt")[0];
							key = key.substring(key.indexOf("_") + 1);
							int result = Integer.parseInt(input.readLine());
							System.out.println(String.format("Received [%s] %s result: %d", requestid, key, result));
							currentRequest.results.put(key, result);
							
							// delete file
							File f = new File(file);
							f.delete();
						}
						
						// set worker to not working
						worker.available = true;
					}
					
					s.close();					
				} 
				catch (UnknownHostException e) {
					//e.printStackTrace();
					System.out.println("Connection refused! Restarting worker " + worker.ipAddress + "...");
					// reassign the work to different worker
					WorkUnit w = worker.workingOn;
					if(w != null) workQueue.add(w);
					worker.available = false;
					worker.active = false;
					worker.shutDownServer();
				} catch (IOException e) {
					//e.printStackTrace();
					System.out.println("Connection refused! Restarting worker " + worker.ipAddress + "...");
					// reassign the work to different worker
					WorkUnit w = worker.workingOn;
					if(w != null) workQueue.add(w);
					worker.available = false;
					worker.active = false;
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
				// poll workqueue
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
					e.printStackTrace();
				}
				worker.available = false;
			}
			
			for(WorkerNode w : workers) 
			{
				if (w.starting)
				{
					// check how long its been since they started
					long enough = System.currentTimeMillis() - w.startTime;
					if(enough >= 240000)
					{
						w.active = true;
						w.available = true;
						w.starting = false;
						w.assignIP();
					}
				}
			}
			// it takes roughly 250 seconds to start up a vm, we poll every 5
			// therefore each worker does roughly 50 requests in the time it takes for a vm to start
			// if there are more requests than we can handle, start up new vms to match it
			// find number of active workers
			
			int activeWorkers = 0;
			for(WorkerNode w : workers) { if (w.active || w.starting) activeWorkers++; }
			
			System.out.println("Active workers: " + activeWorkers + " | Work in Queue: " + workQueue.size());

			if(activeWorkers > 3 && workQueue.size() < (50 * activeWorkers))
			{
				if(!workers.get(activeWorkers - 1).starting) break;
				workers.get(activeWorkers - 1).active = false;
				workers.get(activeWorkers - 1).shutDownServer();
			}
			if(activeWorkers == workers.size()) {

				continue;
			}
			if(workQueue.size() > (50 * activeWorkers))
			{
				if(activeWorkers >= workers.size()) break;
				activeWorkers++; 
				System.out.println("Starting new worker from " + workers.get(activeWorkers - 1).auth.get("email"));
				workers.get(activeWorkers - 1).initVM(); 
				workers.get(activeWorkers - 1).starting = true;
			}
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
			station = WeatherServer.getStationData(params.get("year"), id);
			//station = WeatherServer.dataByYearID.get(params.get("year")).get(id);

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
			

			//Map<String, List<String>> year = WeatherServer.dataByYearID.get(params.get("year"));
			
			// iterate over each station id in the year
			List<File> yearFiles = WeatherServer.getAllStations(params.get("year"));
			
			for(File y : yearFiles)
			{
				station = WeatherServer.getStationData(y);
				for(String data : station)
				{
					String stationId = data.split(",")[0];
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
			id = params.get("stationId");
			option = "TMAX";
			int highLow = Integer.parseInt(params.get("minMax")); // this needs to be put on the request object

			metadata = Integer.toString(highLow);
//			station = WeatherServer.dataByYearID.get(params.get("year")).get(id);
			station = WeatherServer.getStationData(params.get("year"), id);
			
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
		
		r.requestType = requestType;
		r.params = params;
		r.results = new HashMap<String, Integer>();
		requests.put(requestId, r);
		
		System.out.println(String.format("Generated %d work units", work.entrySet().size()));

		int count = 0;
		for (Map.Entry<String, List<Integer>> entry : work.entrySet())
		{
			count++;
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
			// ones that are further along
			switch(requestType)
			{
			case 1:
			case 3:
				w.priority = 1 + (count / r.expectedResults);
				break;
			case 2:
				w.priority = 0.75f + (count / r.expectedResults);
				break;
			}
			workQueue.offer(w);
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
				String.format("Average monthly %s temperature for station %s in %s", (minMax.equals("0")? "min" : "max"), id, y),
				String.format("Average yearly %s temperature in %s", (minMax.equals("0")? "min" : "max"), y),
				String.format("Month with %s temperature for %s in %s", (minMax.equals("0")? "lowest" : "highest"), id, y)
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

			Map<String, Integer> tree = new TreeMap<String, Integer>(results);
			String resultsString = tree.toString();
			resultsString = resultsString.replace("=", ": ").replace("{", "[ ").replace("}", " ]");

			String statement = String.format("Request: " + formattedRequestType() + "\n" +
					"Results: %s\n" +
					"Start Date: %s, End Date: %s, Time taken (minutes): %d\n" +
					"Total Cost ($3/minute): $%d", resultsString, start_date, end_date, time, 3 * time);			
			return statement;
		} else {
			String statement = "Your request is still being processed. It is currently " + status +
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
	float priority = 1;
}

class WorkComparator implements Comparator<WorkUnit> {
    @Override
    public int compare(WorkUnit w1, WorkUnit w2) {
        return Float.compare(w2.priority, w1.priority);
    }
}


class WorkerNode {

	boolean active;
	boolean available;
	boolean starting = false;
	WorkUnit workingOn;
	
	long startTime;
	
	Map<String, String> auth;
	
	OSClientV3 os = null;
	
	String ipAddress;
	String serverId;
	int port = 9000;

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
	
	public void initVM() {
		startTime = System.currentTimeMillis();
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
			e.printStackTrace();
		}

		// initializes work node, compiles and runs
		String script = Base64.getEncoder().encodeToString((
				"#!/bin/bash\n" + 
				"sudo mkdir /home/ubuntu/temp\n" + 
				"sudo apt-get update\n" + 
				"sudo apt-get upgrade\n" +
				"cd /home/ubuntu\n" +
				"sudo chmod 764 run_worker.sh\n" +
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
		System.out.println("Booting worker with ID " + serverId + "...");
	}
	
	public void shutDownServer()
	{
		os.compute().servers().delete(serverId);
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
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
