import java.util.List;

public class WorkNodeThread extends Thread {
	
	public boolean working;
	private volatile List<Integer> data;
	private volatile int requestType;
	private volatile int requestId;
	
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
	
	public void setData(int _requestType, int _requestId, List<Integer> _data)
	{
		data = _data;
		requestId = _requestId;
		requestType = _requestType;
	}
	
	public int getRequestId()
	{
		return requestId;
	}
	
	public int getResult()
	{
		return result;
	}
	
	public int isFinished() {
		//TODO write the code to actually check this
		int finished = 0;  //0 not finished, 1 finished
		
		//if statement here
		
		return  finished;
	}
}
