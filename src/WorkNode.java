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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/* WorkNode will continually wait for connection, and then complete the given work when it receives one */
public class WorkNode {

	public static void main(String[] args) throws Exception
	{
		// the worker node is a server in of itself, it waits for connections from the weather server
		ServerSocket worker = new ServerSocket(9000); 
		
		Socket weatherServer;
		BufferedReader input;
		DataOutputStream output;
		
		Lock lock = new ReentrantLock();
		
		WorkNodeThread thread = new WorkNodeThread(lock);
		thread.start();
		System.out.println("Waiting for work...");
		while(true)
		{
			
			// wait for connection from server
			weatherServer = worker.accept();
			
			// input and output streams
			input = new BufferedReader(new InputStreamReader(weatherServer.getInputStream()));
			output = new DataOutputStream(weatherServer.getOutputStream());
			
			// the server has connected, what does it want?
			int request = input.read();
			
			// 1 = checking results, and if theyre done, get the results
			// 2 = create a job
						
			// it wants to see if we have some results, do we?
			if(request == 1)
			{
				boolean finished = thread.isFinished();
				output.writeBoolean(finished);
				if (finished) {  
					output.write(thread.getRequestId());
					output.writeBytes(thread.getKey() + "\n");
					int r = thread.getResult();
					output.writeBytes(r + "\n");
					
					System.out.println("Returning results: " + r);
					thread.setResult(null);
				}
			}
			// it wants to pass in some work, whats it got?
			else if (request == 2)
			{
				int requestType = input.read();
				int requestId = input.read();
				String options = input.readLine();
				String filename = input.readLine();

				// give the thread the file to download, then wait for it to complete work
				thread.setDownloadParams(weatherServer.getInetAddress().toString(), filename);
				thread.setData(requestType, requestId, options);
				synchronized(lock)
				{
					lock.notify();
				}

			}
			weatherServer.close();
		}
	}
}
