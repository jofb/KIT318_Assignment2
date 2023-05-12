import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;

/*** This thread will be in charge of handling all worker nodes and reading from the work queue ***/

class WorkHandler extends Thread {
	
	Socket serverClient;
	int clientNumber;
	// this should really be a list (temporary)
	WorkerNode w1, w2, w3;
	
	// queue of responses
	Queue<Query> requestQueue;

	
	Queue<WorkUnit> workQueue = new LinkedBlockingQueue<WorkUnit>();

	WorkHandler(Queue<Query> requestQueue) {
		this.requestQueue = requestQueue;
	}
	
	public void run() {
		// lets create a couple of worker nodes
		w1 = new WorkerNode(8886);
		w2 = new WorkerNode(8887);
		w3 = new WorkerNode(8889);

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
				
				// once done, create a QueryResponse object and attach to the query\
				
				// for case 1, the response is the requestId associated with the created request
				// for case 2, the response is the status of the request
				// for case 3, the response is confirmation of the cancellation of request

				QueryResponse response = new QueryResponse("this is a really cool response!");
				query.response = response;
				// then notify the connection thread that this query has a response ready
				synchronized(query)
				{
					query.notify();
				}
			}

			// poll workers and check if there are any results
			
			// add any results to some results queue, which can then be read when requesting to see results
			
			for(WorkUnit work : workQueue)
			{
				// allocate each work unit to available workers
			}
		}
	}
}

/* Class to store information about a requests work unit */
class WorkUnit {
	Integer requestId;
	Integer requestType;
	String data; 
	Integer workId; // TODO might not be necessary
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