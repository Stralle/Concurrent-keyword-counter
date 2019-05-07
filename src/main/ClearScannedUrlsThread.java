package main;

public class ClearScannedUrlsThread extends Thread
{

	private volatile boolean m_working = true;
	
	public ClearScannedUrlsThread()
	{
		this.m_working = true;
	}
	
	@Override
	public void run()
	{
		while (m_working)
		{
			try
			{
				Thread.sleep(Main.getUrlRefreshTime());
				Main.scannedUrls.clear();
				System.out.println("Scanned urls are cleared now");
			} catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void stopIt()
	{
		this.m_working = false;
	}
}
