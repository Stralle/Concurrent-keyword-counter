package main;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class JobDispatcher extends Thread
{
	private volatile boolean m_working = true;
	
	public JobDispatcher() { }
	
	@Override
	public void run()
	{
		while (this.m_working)
		{
			try
			{
				Job newJob = Main.jobQueue.take();
				if (newJob.getType() == ScanType.BREAK)
				{
					System.out.println("Stopping threadpool");
					newJob.getPool().shutdown();
					break;
				}
				Future<Map<String, Integer>> result = newJob.initiate();
//				System.out.println("JOB DONE: " + newJob.getPath());
//				for (String k: Main.getKeywords())
//				{
//					try
//					{
//						System.out.println(k + ": " + result.get().get(k));
//					} catch (ExecutionException e)
//					{
//						e.printStackTrace();
//					}
//				}
			} 
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		System.out.println("Job dispatcher stopped!!!");
	}
	
	public void stopIt()
	{
		Main.jobQueue.clear();
		Main.jobQueue.add(new JobFileScanner(ScanType.BREAK, "")); // Insert posion pill to make it stop/quit
		System.out.println("Stopping dispatcher");
		this.m_working = false;
	}
}