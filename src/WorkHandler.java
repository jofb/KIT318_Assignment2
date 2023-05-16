import java.io.BufferedReader;
import java.io.DataOutputStream;
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

/*** This thread will be in charge of handling all worker nodes and reading from the work queue ***/

class WorkHandler extends Thread {

	Socket serverClient;
	int clientNumber;
	
	int requestCounter = 0;
	
	WorkerNode[] workerArray = new WorkerNode[5];  //array of our workers, up to our max of 5

	// queue of responses
	Queue<Query> requestQueue;

	// map from requestid to array of results
	// this still isn't perfect, look at finding a better way
	Map<Integer, int[]> results = new HashMap<Integer, int[]>();


	Queue<WorkUnit> workQueue = new LinkedBlockingQueue<WorkUnit>();

	WorkHandler(Queue<Query> requestQueue) {
		this.requestQueue = requestQueue;
	}

	public void run() {
		//puts the always on worker nodes into our array of 5
		for(int i = 0; i < 3; i++) {
			   workerArray[i] = new WorkerNode(8080+i); //different port for each worker
			}

		while(true)
		{
			// feel free to get rid of this
			System.out.println("Waiting for Requests...");
			// wait for updates to request queue
			synchronized(requestQueue)
			{
				try {
					// this will wait until requestQueue is notified, OR 1 minute TODO change to 5 (or w/e is appropriate)
					requestQueue.wait(60000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}


			for(Query query : requestQueue)
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
				switch(query.queryType)
				{
				case CREATE:
					createRequest(params);
					break;
				case VIEW:
					id = Integer.parseInt(params.get("requestId"));
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
					results.remove(id);
					break;
				}
				// once done, create a QueryResponse object and attach to the query\

				// for case 1, the response is the requestId associated with the created request
				// for case 2, the response is the status of the request
				// for case 3, the response is confirmation of the cancellation of request
				String r = query.queryParams.toString();
				QueryResponse response = new QueryResponse("Your query: " + r);
				System.out.println("I'm handling this query: " + response.responseBody);
				query.response = response;
				// then notify the connection thread that this query has a response ready
				synchronized(query)
				{
					query.notify();
				}
			}

			// poll workers and check if there are any results

			/* for worker in workers
			 *   if !worker.running
			 *      break
			 *
			 *   (black box) check if worker is done, get back some workResults + request id
			 *   results.put(requestid, workResults)
			 *
			 */

			// add any results to some results queue, which can then be read when requesting to see results

			for(WorkUnit work : workQueue)
			{
				//i feel like we might run into the the problem of the data.txt file being written over as the
				//work node doesn't get to it in time. But, i could be wrong
//				try {
//					String homeDir = System.getProperty("user.home"); // get the home directory of the current user on the VM
//				    String filePath = homeDir + "/data.txt"; // sets the file path for the doc
//				    FileWriter writer = new FileWriter(filePath);
//
//				    //writes the data to the file. Should be noted that each line of the data needs to be
//				    //separated by something, and that can't be commas as we're already using that to separate
//				    //elements in the line itself. Use \n, imo
//					writer.write(work.data);
//		            writer.flush();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
			}
		}
	}
	private void createRequest(Map<String, String> params)
	{
		// get the request type (to be used in a switch statement)
		int requestType = Integer.parseInt(params.get("requestType"));

		// for niceness lets assume requestType == 1
		// but this should really be a switch, unless we can combine the conditions into one piece of functionality

		// avg monthly
		String stationId = params.get("stationId");
		String year = params.get("year");
		int minMax = Integer.parseInt(params.get("minMax"));

		int requestId = requestCounter;
		requestCounter++;

		Map<String, List<Integer>> work = new HashMap<String, List<Integer>>();

		// TODO iterate over data, create new mapping for each station-month

		/* some pesudo code
		 *
		 * for data in dataset:
		 *   if year == data.year and stationid == data.stationId and minMax == data.minMax
		 *     if work.get('stationid-month') doesn't exist:
		 *       work.add('stationid-month', new array list)
		 *
		 *     work.get('stationid-month').add(temp)
 		 *
		 *
		 */

		// create a new results array the size of the array of work units
		results.put(requestId, new int[work.entrySet().size()]);

		for (Map.Entry<String, List<Integer>> entry : work.entrySet())
		{
			// use these to write to file
			String key = entry.getKey();
			List<Integer> values = entry.getValue();

			String filename = requestId + "_" + key;
			// TODO write to file
			// ...

			// create new work unit, pass in file name, then add to queue
			WorkUnit w = new WorkUnit();
			w.requestId = requestId;
			w.requestType = requestType;
			w.data = filename;
			workQueue.add(w);
		}
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

	WorkerNode(int p) {
		port = p;
		running = false;
	}

	public void initVM() {
		// TODO
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
