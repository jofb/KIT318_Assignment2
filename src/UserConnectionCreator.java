import java.net.*;

/*** Creates workers equal to number of threads and then connects to server ***/
/*** This is purely for testing purposes ***/
import java.io.*;
public class UserConnectionCreator {
	
	public static void main(String[] args) throws Exception {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			//int totalclients = 4;
			System.out.println("Enter number of clients :");
			
			int totalclients = Integer.parseInt(br.readLine());
			int clientnumber = 0;
			
			while(clientnumber < totalclients){
				Socket clientSocket=new Socket("127.0.0.1", 8888);
				UserConnectionThread wt = new UserConnectionThread(clientSocket,clientnumber);
				wt.start();
				clientnumber++;
			}
		} catch(Exception e){
			System.out.println(e);
		}
	}
}
