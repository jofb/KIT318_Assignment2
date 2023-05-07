import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/*** Automatic client creation thread, for automatic input ***/
public class UserConnectionThread extends Thread {
	
	Socket clientSocket;
	int clientNumber;
	
	public UserConnectionThread(Socket clientSocket, int clientNumber) {
		this.clientSocket = clientSocket;
		this.clientNumber = clientNumber;
	}
	
	public void run()  {
		try {
			// input and output streams
			BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());
			
			String line = input.readLine();
			System.out.println("pc responded with: " + line);
			
			input.close();
			output.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}
