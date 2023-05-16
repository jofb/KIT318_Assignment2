import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Scanner;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/* WorkNode will continually wait for connection, and then complete the given work when it receives one */
public class WorkNode {

	// reads multiple lines from server and returns as a List of strings
	private static List<String> multiLineRead(BufferedReader input) throws IOException {
		List<String> lines = new ArrayList<String>();
		List<Integer> d = new ArrayList<Integer>();
		String line;
		while ((line = input.readLine()) != null) { 
			if(line.length() == 0) break;
			// print the line
			lines.add(line);
		}
		return lines;
	}
	
	public static void main(String[] args) throws Exception
	{
		// the worker node is a server in of itself, it waits for connections from the weather server
		ServerSocket worker = new ServerSocket(8888);
		
		Socket weatherServer;
		BufferedReader input;
		DataOutputStream output;
		
		downloadFile();
		
		// TODO this needs some sort of exit condition
		while(true)
		{
			// wait for connection from server
			weatherServer = worker.accept();
			
			// input and output streams
			input = new BufferedReader(new InputStreamReader(weatherServer.getInputStream()));
			output = new DataOutputStream(weatherServer.getOutputStream());
			
			// parse the request type
			// for now lets assume the first response is the request type
			int requestType = input.read();
			
			/************************** TODO DOWNLOADING FILE **********************************/
			/* all of the input here can be changed to reflect the address of the file to download + name */
			
			String homeDir = System.getProperty("user.home"); // get the home directory of the current user on the VM
			Scanner sc = new Scanner(new File(homeDir + "/transferred_data.txt"));  
			List<String> allData = new ArrayList<String>();  //list of all our data
			
			//should put all our data into a list. note that at this point i am assuming the data is in the format of (id,date,value type,temp)
			while (sc.hasNextLine()) {
			    String data = sc.nextLine();
			    allData.add(data);
			}
			
			// once we have our input, close the socket
			weatherServer.close();
			
//			List<Integer> numbers = new ArrayList<Integer>();
//			for(String s : numberStrings) {
//				numbers.add(Integer.parseInt(s));
//			}

			int result = 0;
			// TODO should change this
			// 0 for avg, 1 for max, 2 for min
			switch(requestType)
			{
			case 0:
//				int sum = 0;
//				for(int n : numbers) {
//					sum += n;
//				}
//				int avg = sum / numbers.size(); // TODO get this output out of the switch scope
//				result = avg;
				break;
			case 1:
				break;
			case 2:
				break;
			}
			
			// the work is now complete, wait for a request from server again to respond with results
			weatherServer = worker.accept();
			
			output.writeBytes(Integer.toString(result) + "\n");
			
			//perhaps here, before we close the server again, check if we need to keep checking?
			weatherServer.close();
		}	
	}
	
	public static void downloadFile() {

//		try {
			String homeDir = System.getProperty("user.home"); // get the home directory of the current user on the VM
			String host = "203.101.228.83";  //ip of our weather server. CHANGE THIS?
			String user = "ubuntu";
			System.out.println(homeDir);
			String privateKey = homeDir + "/Downloads/tut7.pem"; //this is the bugged line. Probably a .pem
			JSch jsch = new JSch();
//			Session session = jsch.getSession(user, host, 22);
//			Properties config = new Properties();
//			jsch.addIdentity(privateKey);
//			System.out.println("identity added ");
//			config.put("StrictHostKeyChecking", "no");
//			session.setConfig(config);
//			session.connect();
//
//			Channel channel = session.openChannel("sftp");
//			channel.connect();
//			ChannelSftp sftpChannel = (ChannelSftp) channel;
//			sftpChannel.get("/home/ubuntu/data.txt", homeDir + "/transferred_data.txt");
//
//			sftpChannel.exit();
//			session.disconnect();
//		} catch (JSchException e) {
//			e.printStackTrace();
//		} catch (SftpException e) {
//			e.printStackTrace();
//		} catch (Exception e) {
//			System.out.println(e);
//		}
	}
}
