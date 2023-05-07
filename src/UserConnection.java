import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;

/*** Single client, takes in user input from console ***/
public class UserConnection {
	
	Socket clientSocket;
	int clientNumber;
	
	public UserConnection(Socket clientSocket, int clientNumber) {
		this.clientSocket = clientSocket;
		this.clientNumber = clientNumber;
	}
	
	public static void main(String[] args) throws Exception {
		try {
			// change to match destination address
			Socket socket = new Socket("127.0.0.1", 8888);
			
			// TODO this should be a loop similar to on server side, only breaking when user exits
			
			// input and output streams
			BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			DataOutputStream output = new DataOutputStream(socket.getOutputStream());
			// inputstream for user input
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			
			String line = input.readLine();
			System.out.println("pc responded with: " + line);
			
			input.close();
			output.close();
			
			socket.close();
		} catch(Exception e){
			System.out.println(e);
		}
	}
}
