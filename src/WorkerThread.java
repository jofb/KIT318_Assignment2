import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*** This thread will be in charge of handling all worker nodes and reading from the work queue ***/
class WorkerThread extends Thread {
	
	Socket serverClient;
	int clientNumber;

	WorkerThread(Socket inSocket, int counter) {
		serverClient = inSocket;
		clientNumber = counter;
	}
	
	public void run() {
		// TODO what we doing in here?
	}

}