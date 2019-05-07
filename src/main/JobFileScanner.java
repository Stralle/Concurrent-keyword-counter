package main;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

public class JobFileScanner implements Job
{
	private ScanType m_jobType;
	private String m_jobPath;
	private static volatile ForkJoinPool fileScannerThreadPool = new ForkJoinPool();
	
	public JobFileScanner(ScanType type, String path)
	{
		this.m_jobPath = path;
		this.m_jobType = type;
	}
	
	public String getPath()
	{
		return this.m_jobPath;
	}

	@Override
	public ScanType getType()
	{
		return this.m_jobType;
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
		File corpus = new File(this.m_jobPath);
		File[] files = corpus.listFiles(new FilenameFilter()
		{
			@Override
			public boolean accept(File current, String name)
			{
	            return name.toLowerCase().endsWith(".txt");
			}
		});
		Future<Map<String, Integer>> result = fileScannerThreadPool.submit(new JobFileTask(files));
		Main.getResultRetriever().getMapOfAllResults().put(this.m_jobPath, result);
		return result;
	}
	
	@Override
	public String toString()
	{
		return "File: " + this.getPath();
	}

	@Override
	public ForkJoinPool getPool()
	{
		return fileScannerThreadPool;
	}
}
