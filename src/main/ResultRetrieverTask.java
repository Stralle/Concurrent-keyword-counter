package main;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveTask;

public class ResultRetrieverTask extends RecursiveTask<Map<String, Integer>>
{
	private static final long serialVersionUID = 4373360509397828401L;
	private String m_path;

	public ResultRetrieverTask(String path)
	{
		this.setM_path(path);
	}

	@Override
	protected Map<String, Integer> compute()
	{
		System.out.println("Getting results for url " + this.m_path);
		Map<String, Integer> finalResult = new HashMap<>();
		for (String k: Main.getKeywords())
		{
			finalResult.put(k, 0);
		}
		Iterator<Entry<String, Future<Map<String, Integer>>>> it = Main.getResultRetriever().getMapOfAllResults().entrySet().iterator();
		while (it.hasNext()) 
		{
	        Entry<String, Future<Map<String, Integer>>> pair = it.next();
	        
	        if (pair.getKey().contains(this.m_path))
	        {
//	        	System.out.println("It is contained in map of all results!");
	        	if (Main.getResultRetriever().isQuery().get() && !pair.getValue().isDone())
	        	{
//	        		System.out.println("Returning null...");
	        		finalResult.put("-1", 0);
	        		return finalResult;
	        	}
	        	try
				{
	        		Map<String, Integer> res = pair.getValue().get();
	        		res.forEach((k, v) -> finalResult.merge(k, v, Integer::sum));
				} 
	        	catch (InterruptedException | ExecutionException e)
				{
					e.printStackTrace();
				}
	        }
	    }
		return finalResult;
	}

	public String getM_path()
	{
		return m_path;
	}

	public void setM_path(String m_path)
	{
		this.m_path = m_path;
	}

}
