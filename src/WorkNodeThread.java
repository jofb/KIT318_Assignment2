import java.util.List;
import java.util.concurrent.locks.Lock;

public class WorkNodeThread extends Thread {
	
	public boolean working;
	private volatile List<Integer> data;
	private volatile int requestType;
	private volatile int requestId;
	
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

			result = null;
			System.out.println("im doing this work!" + data.toString());
			
			// do the work based on data and request type
			
			// 
			
			// result = ...
			
			result = 5;
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
	
	public boolean isFinished() {
		return result != null;
	}
}
