package main;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class ResultRetriever implements InterfaceResultRetriever
{
	public static ForkJoinPool resultRetrieverThreadPool = new ForkJoinPool();
	public static volatile Map<String, Future<Map<String, Integer>>> mapOfAllResults = new ConcurrentHashMap<>();
	private AtomicBoolean isQuery = new AtomicBoolean(false);
	private volatile Map<String, Map<String, Integer>> cacheMap = new ConcurrentHashMap<>();
	private volatile Map<String, Map<String, Integer>> webSummary = new ConcurrentHashMap<>();
	private volatile Map<String, Map<String, Integer>> fileSummary = new ConcurrentHashMap<>();
	
	public ResultRetriever()
	{
		ResultRetriever.resultRetrieverThreadPool = new ForkJoinPool();
		ResultRetriever.mapOfAllResults = new ConcurrentHashMap<>();
	}

	@Override
	public Map<String, Integer> getResult(String query)
	{
		String tokens[] = query.split("\\|");
		String type = tokens[0];
		String path = tokens[1];
//		System.out.println("I got path: " + path + " " + type);
		
		Iterator<Entry<String, Future<Map<String, Integer>>>> it = mapOfAllResults.entrySet().iterator();			
		
		if (type.equalsIgnoreCase("web"))
		{
			if (Main.getResultRetriever().getCacheMap().containsKey(path))
			{
				System.out.println("It\'s in a cache woohoo");
	    		for (String k: Main.getKeywords())
	    		{
	    			System.out.println(k + ": " + this.getCacheMap().get(path).get(k));
	    		}
	    		return this.getCacheMap().get(path);
			}
			Future<Map<String, Integer>> finalResult = resultRetrieverThreadPool.submit(new ResultRetrieverTask(path));

			try
			{
				if (finalResult.get().containsKey("-1"))
				{
					System.out.println("NOT DONE YET " + path);
					return null;
				}
				else if (finalResult.get().isEmpty())
				{
					System.err.println("There\'s no such url");
					return null;
				}
				else
				{
					try
					{
						for (String k: Main.getKeywords())
						{
							System.out.println(k + ": " + finalResult.get().get(k));
						}
						this.getCacheMap().put(path, finalResult.get());
						return finalResult.get();
					}
					catch (Exception e)
					{
						System.err.println("Whoops something went wrong...");
						return null;
					}
				}
			} 
			catch (InterruptedException | ExecutionException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if (type.equalsIgnoreCase("file"))
		{
			System.out.println("Getting results for files in directory " + path);
			while (it.hasNext()) 
			{
		        Entry<String, Future<Map<String, Integer>>> pair = it.next();
		        if (pair.getKey().contains(path))
		        {
		        	if (Main.getResultRetriever().isQuery().get() && !pair.getValue().isDone())
		        	{
		        		System.out.println("NOT DONE YET " + path);
		        		continue;
		        	}
		        	try
					{
		        		Map<String, Integer> res = pair.getValue().get();
		        		for (String k: Main.getKeywords())
		        		{
		        			System.out.println(k + ": " + res.get(k));
		        		}
						return pair.getValue().get();
					} 
		        	catch (InterruptedException | ExecutionException e)
					{
						e.printStackTrace();
					}
		        }
		    }
			System.err.println("There\'s no such a directory");
		}
	    
		System.err.println("Something went wrong. Maybe wrong path? ^.-");
		return null;
	}

	@Override
	public Map<String, Integer> queryResult(String query)
	{
		this.isQuery().set(true);
		System.out.println("Query result...");
		return getResult(query);
	}

	@Override
	public void clearSummary(ScanType summaryType)
	{
		if (summaryType == ScanType.WEB)
		{
			this.webSummary.clear();
		}
		else if (summaryType == ScanType.FILE)
		{
			this.fileSummary.clear();
		}
	}

	@Override
	public Map<String, Map<String, Integer>> getSummary(ScanType summaryType)
	{
		Map<String, Map<String, Integer>> finalResult = new ConcurrentHashMap<>();
		if (summaryType == ScanType.WEB)
		{
			System.out.println("All domains:");
			for (String path: Main.allDomains)
			{
				if (Main.getResultRetriever().webSummary.containsKey(path))
				{
					System.out.println("CACHED Domain: " + path);
					for (String k: Main.getKeywords())
					{
						System.out.println("\t" + k + ": " + Main.getResultRetriever().webSummary.get(path).get(k));
					}
					continue;
				}
				finalResult.put(path, new ConcurrentHashMap<>());
				Iterator<Entry<String, Future<Map<String, Integer>>>> it = mapOfAllResults.entrySet().iterator();			
				
				System.out.println("Getting results for links on domain " + path);
				boolean notDoneYet = false;
				while (it.hasNext()) 
				{
			        Entry<String, Future<Map<String, Integer>>> pair = it.next();
			        if (pair.getKey().contains(path))
			        {
			        	if (Main.getResultRetriever().isQuery().get() && !pair.getValue().isDone())
			        	{
			        		notDoneYet = true;
			        		break;
			        	}
			        	try
						{
			        		Map<String, Integer> res = pair.getValue().get();
			        		res.forEach((k, v) -> finalResult.get(path).merge(k, v, Integer::sum));
						} 
			        	catch (InterruptedException | ExecutionException e)
						{
							e.printStackTrace();
						}
			        }
			    }
				if (notDoneYet)
				{
					System.out.println("NOT DONE YET " + path);
				}
				else
				{
					System.out.println("Domain: " + path);
					for (String k: Main.getKeywords())
					{
						System.out.println("\t" + k + ": " + finalResult.get(path).get(k));
					}
					Main.getResultRetriever().webSummary.put(path, finalResult.get(path));
				}
			}
		}
		else
		{
			System.out.println("All corpus:");
			for (String path: Main.allCorpuss)
			{				
				if (Main.getResultRetriever().fileSummary.containsKey(path))
				{
					System.out.println("CACHED Corpus: " + path);
					for (String k: Main.getKeywords())
					{
						System.out.println("\t" + k + ": " + Main.getResultRetriever().fileSummary.get(path).get(k));
					}
					continue;
				}
				finalResult.put(path, new ConcurrentHashMap<>());
				Iterator<Entry<String, Future<Map<String, Integer>>>> it = mapOfAllResults.entrySet().iterator();			
				
				System.out.println("Getting results for files in folder " + path);
				boolean notDoneYet = false;
				while (it.hasNext()) 
				{
			        Entry<String, Future<Map<String, Integer>>> pair = it.next();
			        if (pair.getKey().contains(path))
			        {
			        	if (Main.getResultRetriever().isQuery().get() && !pair.getValue().isDone())
			        	{
			        		notDoneYet = true;
			        		break;
			        	}
			        	try
						{
			        		Map<String, Integer> res = pair.getValue().get();
			        		res.forEach((k, v) -> finalResult.get(path).merge(k, v, Integer::sum));
						} 
			        	catch (InterruptedException | ExecutionException e)
						{
							e.printStackTrace();
						}
			        }
			    }
				if (notDoneYet)
				{
					System.out.println("NOT DONE YET " + path);
				}
				else
				{
					System.out.println("Corpus: " + path);
					for (String k: Main.getKeywords())
					{
						System.out.println("\t" + k + ": " + finalResult.get(path).get(k));
					}
					Main.getResultRetriever().fileSummary.put(path, finalResult.get(path));
				}
			}
			
		}
		return finalResult;
	}

	@Override
	public Map<String, Map<String, Integer>> querySummary(ScanType summaryType)
	{
		this.isQuery().set(true);
		return getSummary(summaryType);
	}

	@Override
	public void addCorpusResult(String corpusName, Future<Map<String, Integer>> corpusResult)
	{
		// TODO Auto-generated method stub
		
	}

	public Map<String, Future<Map<String, Integer>>> getMapOfAllResults()
	{
		return mapOfAllResults;
	}

	public static void setMapOfAllResults(Map<String, Future<Map<String, Integer>>> mapOfAllResults)
	{
		ResultRetriever.mapOfAllResults = mapOfAllResults;
	}
	
	public static void stop()
	{
		System.out.println("Stopping result retriever");
		resultRetrieverThreadPool.shutdown();
	}

	public Map<String, Map<String, Integer>> getCacheMap()
	{
		return cacheMap;
	}

	public void setCacheMap(Map<String, Map<String, Integer>> cacheMap)
	{
		this.cacheMap = cacheMap;
	}

	public AtomicBoolean isQuery()
	{
		return isQuery;
	}

	public void setQuery(AtomicBoolean isQuery)
	{
		this.isQuery = isQuery;
	}
	
}