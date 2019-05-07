package main;

import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

public interface Job
{
	ScanType getType();
	
	String getQuery();
	
	String getPath();
	
	Future<Map<String, Integer>> initiate();

	ForkJoinPool getPool();
}