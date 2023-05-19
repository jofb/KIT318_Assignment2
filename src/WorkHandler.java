import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	
	List<WorkerNode> workers = new ArrayList<WorkerNode>();

	// queue of queries
	Queue<Query> queryQueue;
	
	// queue of requests
	Queue<Request> requestQueue;

	// map from requestid to request object
	Map<Integer, Request> requests = new HashMap<Integer, Request>();


	Queue<WorkUnit> workQueue = new LinkedBlockingQueue<WorkUnit>();

	WorkHandler(Queue<Query> requestQueue) {
		this.queryQueue = requestQueue;
	}

	public void run() {
		//puts the always on worker nodes into our array of 5
//		for(int i = 0; i < 3; i++) 
//		{
//		   workerArray[i] = new WorkerNode(8080+i); //different port for each worker
//		}
		
//		workers.add(new WorkerNode("email", "pass", "projectid"));
//		workers.get(0).initVM("imageid", "keypair", "security");
//		
		// ensure that the output folder exists
		File requestDir = new File(REQUESTS_DIR);
		requestDir.mkdir();
		
//		Map<String, String> pp = new HashMap<String, String>();
//		pp.put("requestType", "2");
//		pp.put("year", "1863");
//		pp.put("minMax", "1");
////		pp.put("stationId", "ITE00100550");
//		createRequest(pp);

		while(true)
		{
			// feel free to get rid of this
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

				// cases:
				// 1. create a request
				//		based on request type, split up data into work units
				//		then put work units into work queue

				// 2. view request
				//		check status of request somehow (perhaps checking work queue)

				// 3. cancel request
				//		remove all work units associated with request id from queue
				Map<String, String> params = query.queryParams;
				int id;
				String response = "";
				switch(query.queryType)
				{
				case CREATE:
					createRequest(params);
					//response to be output, which reminds the user of the request ID
					response = "your request is being handled i swear!";
//					response = "A new query has now been created, and is associated with request ID " + Integer.parseInt(params.get("requestId"));
					break;
				case VIEW:
					id = Integer.parseInt(params.get("requestId"));
					String currentStatus = requests.get(id).returnStatus();
					response = "Your query has processed " + currentStatus + " items";
					// find out status of the id
					// (i.e compare the number of results to the total of expected results)
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
					// would also need to remove it from whatever results mapping we have
					response = "The query associated with request ID " + Integer.parseInt(params.get("requestId")) + " has been deleted";
					requestQueue.remove(id);
					break;
				}
				// once done, create a QueryResponse object and attach to the query\
				query.response = new QueryResponse(response);

				// for case 1, the response is the requestId associated with the created request
				// for case 2, the response is the status of the request
				// for case 3, the response is confirmation of the cancellation of request
//				String r = query.queryParams.toString();
//				QueryResponse response = new QueryResponse("Your query: " + r);
//				System.out.println("I'm handling this query: " + response.responseBody);
//				query.response = response;
				// then notify the connection thread that this query has a response ready
				synchronized(query)
				{
					query.notify();
				}
			}

			// poll workers and check if there are any results
			
			// iterate over workers and connect to them using connect
			
			// step 1. send 1 using 
			
			// receieve a boolean
			
			// if the booleans true, weve got results
			
			// receieve results

			/* for worker in workers
			 *   if !worker.running
			 *      break
			 *
			 *   (black box) check if worker is done, get back some workResults + request id
			 *   results.put(requestid, workResults)
			 *   
			 *   if its a 1, then read in the results the worknodes about to send
			 *
			 */
			
			// try to connect to worker node
			try {
				Socket s = new Socket("131.217.174.74", 9000);
				// give some work to it
				// input and output streams
				BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));
				DataOutputStream output = new DataOutputStream(s.getOutputStream());
				
				// to download a file
				output.write(2);
				output.write(1);
				WorkUnit w = workQueue.poll();
				output.writeBytes(w.requestId + "\n");
				
				output.writeBytes(w.data + "\n");
				
				s.close();
				
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
						
			for (WorkerNode worker : workers) {
				if (!worker.running) {
					break;
				}
				
			}

			// add any results to some results queue, which can then be read when requesting to see results

			for(WorkUnit work : workQueue)
			{
				// iterate over work and allocate to workers
				
				// send 2 : create job
				
				// send over whatever data u want for the work
			}
		}
	}
	private void createRequest(Map<String, String> params)
	{
		// get the request type (to be used in a switch statement)
		int requestType = Integer.parseInt(params.get("requestType"));
		
		int requestId = requestCounter;
		requestCounter++;
		
		params.put("requestId", Integer.toString(requestId));

		Map<String, List<Integer>> work = new HashMap<String, List<Integer>>();
		
		String option;
		String id;
		int steps = 1;
		
		switch(requestType)
		{
		// avg monthly temp by id-year
		case 1:
			id = params.get("stationId");

			option = (params.get("minMax") == "0")? "TMIN" : "TMAX";
			
			System.out.println(params.get("year"));

			// grab the dataset by station id
			List<String> station = WeatherServer.dataByID.get(id);
			
			System.out.println(station.size());
			
			// iterate over station
			for(String data : station)
			{
				String[] line = data.split(",");

				if(!line[2].equals(option) && !line[1].substring(0, 4).equals(params.get("year"))) continue;
				// get parts of the line
				String date = line[1];
				String month = date.substring(4, 6);

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
			
			List<String> year = WeatherServer.dataByYear.get(params.get("year"));

			for(String data : year)
			{
				String[] line = data.split(",");
				if(line[2].equals(option)) continue;
				
				String stationId = line[0];
				
				int temp = Integer.parseInt(line[3]);
				
				String key = stationId;
				
				if(!work.containsKey(key))
				{
					work.put(key, new ArrayList<Integer>());
				}
				work.get(key).add(temp);
			}
			break;
			// month which has highest/lowest max temperature in given year and station
		case 3:
			// TODO not done yet
			id = params.get("stationId");
			steps = 2;
			option = "TMAX";
			int highLow = Integer.parseInt(params.get("minMax"));
			
			List<String> st = WeatherServer.dataByID.get(params.get(id));
			
			for(String data : st)
			{
				String[] line = data.split(",");
				
				if(!line[2].equals(option) && !line[1].substring(0, 4).equals(params.get("year"))) continue;
				
				String date = line[1];
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
				
				String line = values.toString();
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
			System.out.println(w.requestId);
			workQueue.add(w);
		}
	}
}

// go ahead with creating a request object

// have a steps variable
// set the bad cases one to TWO steps
// and then when the list of results is filled up
// create a NEW work unit from the results
// pass into queue with the request id

class Request {
	int id;
	int step;
	int totalSteps;
	int expectedResults;
	List<Integer> results;
	
	long time;
	
	Request(int id, int steps, int expectedResults) 
	{
		this.id = id;
		this.totalSteps = steps;
		this.expectedResults = expectedResults;
		time = System.currentTimeMillis();
		
		this.step = 1;
	}
	
	// not implemented
	void checkStatus()
	{
		// compare step to total steps
		double mod = step / totalSteps;
		
		// then compare expected results to size of results
		double r = results.size() / expectedResults;
	}
		
	//added a new function, in case you want to use the other one for different things
	String returnStatus() {
		String status = results.size() + "/" + expectedResults;
		
		// if status is compelte
		
		// when its complete
		time = System.currentTimeMillis() - time;
		
		// then also return bill (results + price + time)
		
		return status;
	}
}

/* Class to store information about a requests work unit */
class WorkUnit {
	Integer requestId;
	Integer requestType;
	String data;
}


// TODO in here we could also create a method to create the VM
// TODO rather than port it should be storing an ip address since we will be working with VMs
// using ports for now on local machines since all on same machine
class WorkerNode {
	int port;
	boolean running;
	OSClientV3 os = null;
	String ipAddress;
	
	// TODO pass in the auth information
	WorkerNode(String email, String password, String projectID) {
		running = false;
		// commented out while testing
		os = OSFactory.builderV3()//Setting up openstack client with  -OpenStack factory
				.endpoint("https://keystone.rc.nectar.org.au:5000/v3")//Openstack endpoint
				.credentials(email, password,Identifier.byName("Default"))//Passing credentials
				.scopeToProject( Identifier.byId(projectID))//Project id
				.authenticate();//verify the authentication
	}
	
	// image id
	// key pair name
	// security group id
	public void initVM(String imageID, String keyPairName, String securityID) {
		String script = Base64.getEncoder().encodeToString(("#!/bin/bash\n" + "sudo mkdir /home/ubuntu/temp").getBytes());//encoded with Base64. Creates a temporary directory
		ServerCreate server = Builders.server()//creating a VM server
				.name("Test")//VM or instance name
				.flavor("406352b0-2413-4ea6-b219-1a4218fd7d3b")//flavour id
				.image(imageID)// -image id
				.keypairName(keyPairName)//key pair name
				.addSecurityGroup(securityID)	//Security group ID (allow SSH)
				.userData(script)
				.build();//build the VM with above configuration
			
		Server booting=os.compute().servers().boot(server);
		ipAddress=booting.getAccessIPv4();
	}
	
	public Socket connect() {
		if(!running) return null;
		try {
			return new Socket("127.0.0.1", port);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
}
