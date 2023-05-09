import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.UUID;

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
			
			String line;  //line that comes from the server (could just use input.readLine() every time)
			while ((line = input.readLine()) != null) {  //weird code to read what the server says until we hit a line that says stop
				if (line.equals("stop")) {
			        break;  //might change this because very ugly way of doing it
			    }
				System.out.println("Server: " + line);  //prints the line
			}
			
			String selection = "";  //user's menu selection
			while (!(selection.equals("1") || selection.equals("2"))) {  //if it isn't a legitimate response keep asking
				System.out.println("Please enter a number (1 or 2)...");
	            selection = br.readLine();
			}
            
            output.writeBytes(selection+"\n");
            
            if (selection.equals("1")) {  //go into the register menu, print the lines
            	while ((line = input.readLine()) != null) {
    				if (line.equals("stop")) {
    			        break;
    			    }
    				System.out.println("Server: " + line);
    			}
			} else if (selection.equals("2")) {  //go into the login menu
				boolean done = false;  //boolean for if we have found a correct password
				
				while (!done) {  //loops until we've found a correct password
					while ((line = input.readLine()) != null) {  //outputs first chunk of server text about inputting password
	    				if (line.equals("stop")) {
	    			        break;
	    			    }
	    				System.out.println("Server: " + line);
	    			}					
					
					System.out.println("Please enter password...");
		            String attempt = br.readLine();  //reads user's password input
		            output.writeBytes(attempt+"\n");  //sends it to the server (or server thread, technically)
		            
		            while ((line = input.readLine()) != null) {  //outputs whether the password is correct or not
	    				if (line.equals("stop")) {
	    			        break;
	    			    }
	    				if (line.equals("DONE")) {  //means we have a correct password
	    					done = true;  //can break our loop
	    				} else {
	    					System.out.println("Server: " + line);
	    				}
	    			}
				}	            
			}
			
			
			input.close();
			output.close();
			
			socket.close();
		} catch(Exception e){
			System.out.println(e);
		}
	}
}
