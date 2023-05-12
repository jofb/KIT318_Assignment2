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
import java.util.concurrent.SynchronousQueue;

/*** This thread will be in charge of handling all worker nodes and reading from the work queue ***/

class WorkHandler extends Thread {
	
	Socket serverClient;
	int clientNumber;
	// this should really be a list
	WorkerNode w1, w2, w3;
	
	Queue<WorkUnit> workQueue;

	WorkHandler(Queue<WorkUnit> workQueue) {
		this.workQueue = workQueue;
	}
	
	public void run() {
		// lets create a couple of worker nodes
		w1 = new WorkerNode(8886);
		w2 = new WorkerNode(8887);
		w3 = new WorkerNode(8889);
		
		// i believe this pauses the thread until the work queue is notified of change 
		// which can be handled server side with workQueue.notifyAll();
		try {
			workQueue.wait();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// to allocate a task, we grab a workunit from the queue
		// TODO this is a naive removal, could be improved by searching the queue and checking priority
		// TODO add priority variable to work units? based on request type
		WorkUnit work = workQueue.poll();
		
		// and once we have the work we can connect to an available worker
		
		// and do the deed
		
		// the deed involves first sending the request type as an int (0, 1, 2)
		
		// then sending the list of ints within the work as one string
		
		// TODO additionally, we should be polling every few minutes to check if a work node has completed work
	}
	
	

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