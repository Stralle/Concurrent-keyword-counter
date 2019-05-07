package main;

import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

public class JobWebScanner implements Job
{
	
	private ScanType jobType;
	private String jobPath;
	private long hopCount;
	private static volatile ForkJoinPool webScannerThreadPool = new ForkJoinPool();
	
	public JobWebScanner(ScanType type, String path, long hop)
	{
		this.jobPath = path;
		this.jobType = type;
		this.hopCount = hop;
	}
	
	public String getPath()
	{
		return this.jobPath;
	}

	@Override
	public ScanType getType()
	{
		return this.jobType;
	}
	
	public long getHopCount()
	{
		return hopCount;
	}
	
	public void setHopCount(long hopCount)
	{
		this.hopCount = hopCount;
	}

	@Override
	public String getQuery()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<Map<String, Integer>> initiate()
	{	
		Future<Map<String, Integer>> webScannerResult = webScannerThreadPool.submit(new WebFileTask(this.jobPath, this.hopCount));
		Main.getResultRetriever().getMapOfAllResults().put(this.jobPath, webScannerResult);
		return webScannerResult;
	}
	
	@Override
	public String toString()
	{
		return "Web: " + this.getPath();
	}

	@Override
	public ForkJoinPool getPool()
	{
		return webScannerThreadPool;
	}
}
