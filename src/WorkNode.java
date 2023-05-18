import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Scanner;
import java.util.stream.Collectors;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/* WorkNode will continually wait for connection, and then complete the given work when it receives one */
public class WorkNode {
	
	private static final String WORK_DATA_PATH = "work_data.txt";
	
	public static volatile Object result = null;
	private static volatile boolean working = false;

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
		
		WorkNodeThread thread = new WorkNodeThread();
		
		while(true)
		{
			// wait for connection from server
			weatherServer = worker.accept();
			
			// input and output streams
			input = new BufferedReader(new InputStreamReader(weatherServer.getInputStream()));
			output = new DataOutputStream(weatherServer.getOutputStream());
			
			// the server has connected, what does it want?
			int request = input.read();
			
			// it wants to see if we have some results, do we?
			if(request == 1)
			{
				if(result != null)
				{
					// give the result back to the server
				} else
				{
					// tell the server it got nothin
				}
			}
			// it wants to pass in some work, whats it got?
			else if (request == 2)
			{
				int requestType = input.read();
				int requestId = Integer.parseInt(input.readLine());
				String filename = input.readLine();

				// TODO downloading should be moved into the thread
				downloadFile(filename);
				
				// next, read from the downloaded file
				String homeDir = System.getProperty("user.home"); 
				Scanner sc = new Scanner(new File(homeDir + "/" + WORK_DATA_PATH));
				
				List<String> lines = new ArrayList<String>();
				
				while(sc.hasNextLine())
				{
					// splitting by comma
					String line = sc.nextLine();
					lines.addAll(Arrays.asList(line.split(",")));
				}
				// map to list of integers
				List<Integer> data = lines.stream().map(Integer::parseInt).collect(Collectors.toList());
				
				// tell the worknode thread to do some work (pass in the data, and then notify)
				thread.setData(requestType, data);
				data.notify();
			}
			weatherServer.close();
		}
	}
	
	public static void downloadFile(String path) {

		try {
			// TODO work node needs to know the host IP, plus have a private key on them
			String homeDir = System.getProperty("user.home"); // get the home directory of the current user on the VM
			String host = "203.101.225.57";  //ip of our weather server. CHANGE THIS?
			String user = "ubuntu";
			String privateKey = homeDir + "/Downloads/kit318-assignment-ssh.pem"; //this is the bugged line. Probably a .pem
			JSch jsch = new JSch();
			Session session = jsch.getSession(user, host, 22);
			Properties config = new Properties();
			jsch.addIdentity(privateKey);
			System.out.println("identity added ");
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.connect();

			Channel channel = session.openChannel("sftp");
			channel.connect();
			ChannelSftp sftpChannel = (ChannelSftp) channel;
			sftpChannel.get(path, homeDir + "/" + WORK_DATA_PATH);

			sftpChannel.exit();
			session.disconnect();
		} catch (JSchException e) {
			e.printStackTrace();
		} catch (SftpException e) {
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println(e);
		}
	}
}
