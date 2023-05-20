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
	// reads multiple lines from server, exiting on an empty line
	private static void menuPrint(BufferedReader input) throws IOException {
		String line;
		while ((line = input.readLine()) != null) { 
			if(line.length() == 0) break;
			// print the line
			System.out.println("Server: " + line);
		}
	}
	
	public void run()  {
		try {
			// input and output streams
			BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());
			
			boolean p = false;
			menuPrint(input);
			output.writeBytes("2\n"); // password
			System.out.println(input.readLine());
			output.writeBytes("password\n");
			p = input.read() != 0;
			System.out.println(input.readLine());
			menuPrint(input);
			output.writeBytes("2\n");
			p = input.read() != 0;
			System.out.println(input.readLine());
			menuPrint(input);
			output.writeBytes("1\n");
			p = input.read() != 0;
			
//			request type 1
			System.out.println(input.readLine());
			output.writeBytes("ITE00100550\n");
			System.out.println(input.readLine());
			output.writeBytes("1863\n");
			System.out.println(input.readLine());
			output.writeBytes("1\n");
			
//			request type 2
//			System.out.println(input.readLine());
//			output.writeBytes("1863\n");
//			System.out.println(input.readLine());
//			output.writeBytes("1\n");
//			
			System.out.println(input.readLine());
			
			input.close();
			output.close();
			clientSocket.close();

		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}
