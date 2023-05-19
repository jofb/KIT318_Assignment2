import java.util.List;

public class WorkNodeThread extends Thread {
	
	public boolean working;
	private volatile List<Integer> data;
	private volatile int requestType;
	
	private volatile int result;
	
	public WorkNodeThread()
	{
		
	}

	public void run()
	{
		// this will wait for work
		while(true)
		{
			try {
				data.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// do the work based on data and request type
			
			// 
			
			// result = ...
		}
	}
	
	public void setData(int _requestType, List<Integer> _data)
	{
		data = _data;
		requestType = _requestType;
	}
	
	public int getResult()
	{
		return result;
	}
}
