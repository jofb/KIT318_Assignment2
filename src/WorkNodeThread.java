import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class WorkNodeThread extends Thread {
	
	private static final String WORK_DATA_PATH = "work_data.txt";
	
	private volatile List<Integer> data;
	private volatile int requestType;
	private volatile int requestId;
	private volatile String options;
	
	private volatile String serverIP;
	private volatile String filename;
	
	private Lock lock;
	
	private volatile Integer result;
	
	public WorkNodeThread(Lock _lock)
	{
		lock = _lock;
	}

	public void run()
	{
		// this will wait for work
		while(true)
		{
			synchronized(lock)
			{
				try {
					lock.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			// download the file
			downloadFile(serverIP, filename);
			
			System.out.println("Downloading file: " + filename + "...");
			
			// next, read from the downloaded file
			String homeDir = "/home/ubuntu"; 
			Scanner sc;
			try {
				sc = new Scanner(new File(homeDir + "/" + WORK_DATA_PATH));

				List<String> lines = new ArrayList<String>();
				
				while(sc.hasNextLine())
				{
					// splitting by comma
					String line = sc.nextLine();
					lines.addAll(Arrays.asList(line.split(",")));
				}
				// map to list of integers
				data = lines.stream().map(Integer::parseInt).collect(Collectors.toList());
				result = null;
				
				switch(requestType)
				{
				// avg 
				case 1:
				case 2:
					int avg = data.stream().reduce(0, Integer::sum) / data.size();
					result = avg;
					break;
					
				case 3:
					// compute min/max based on options
					int m = Integer.parseInt(options);

					// pretty bad but will work
					switch(m)
					{
					case 0:
						int min = Integer.MAX_VALUE;
						for(int d : data)
						{
							if (d < min) min = d;
						}
						result = min;
						break;
					case 1:
						int max = Integer.MIN_VALUE;
						for(int d : data)
						{
							if (d > max) max = d;
						}
						result = max;
						break;
					}
					break;
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void setData(int _requestType, int _requestId, String _options)
	{
		requestId = _requestId;
		requestType = _requestType;
		options = _options;
	}
	
	public void setDownloadParams(String ip, String path)
	{
		serverIP = ip;
		filename = path;
	}
	
	public int getRequestId()
	{
		return requestId;
	}
	
	public int getResult()
	{
		return result;
	}
	
	public void setResult(Integer r)
	{
		result = r;
	}
	
	public String getKey()
	{
		return filename;
	}
	
	public boolean isFinished() {
		return result != null;
	}

	public static void downloadFile(String ip, String path) {

		try {
			// TODO work node needs to know the host IP, plus have a private key on them
			String homeDir = "/home/ubuntu"; // get the home directory of the current user on the VM
			String host = ip.substring(1);  //ip of the weather server to download from
			String user = "ubuntu";
			String privateKey = homeDir + "/kit318_assignment_ssh.pem"; //this is the bugged line. Probably a .pem
			JSch jsch = new JSch();
			Session session = jsch.getSession(user, host, 22);
			Properties config = new Properties();
			jsch.addIdentity(privateKey);
			jsch.setKnownHosts(".ssh/known_hosts");
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
