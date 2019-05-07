package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

public class JobFileTask extends RecursiveTask<Map<String, Integer>>
{
	private static final long serialVersionUID = -632999003173332511L;
	private File[] files;
	
	public JobFileTask (File[] f)
	{
		this.files = f;
	}

	@Override
	protected Map<String, Integer> compute()
	{
		// Mozda bi ovo trebalo da bude Future? I da ima mapiranje na direktorijum u kojem se nalazi?
		System.out.println("Computing... ");
		Map<String, Integer> mapToReturn = new ConcurrentHashMap<>();
		for (String k: Main.getKeywords())
		{
			if (!mapToReturn.containsKey(k))
			{
				mapToReturn.put(k, 0);
			}
		}
		long currentSize = 0;
		ArrayList<File> newFiles = new ArrayList<>();
		ArrayList<File> otherFiles = new ArrayList<>();
		
		if (files.length == 0)
		{
			return mapToReturn;
		}
		
		for (File f: files)
		{
			currentSize += f.length();
			if (currentSize <= Main.getScanningSizeLimit())
			{
				newFiles.add(f);
				Map<String, Integer> temp = readFile(f);
				Iterator<Entry<String, Integer>> it = temp.entrySet().iterator();
			    while (it.hasNext()) {
			        Map.Entry<String, Integer> pair = it.next();
			        mapToReturn.put((String) pair.getKey(), mapToReturn.get((String) pair.getKey()) + (Integer) pair.getValue());
			        it.remove(); // avoids a ConcurrentModificationException
			    }
			}
			else
			{
				ForkJoinTask<Map<String, Integer>> forkTask = new JobFileTask((File[]) newFiles.toArray(new File[newFiles.size()]));
				
				forkTask.fork();
			
				for (File fs: files)
				{
					if (!newFiles.contains(fs))
					{
						otherFiles.add(fs);
					}
				}
				
				JobFileTask callTask = new JobFileTask((File[]) otherFiles.toArray(new File[otherFiles.size()]));
				
				Map<String, Integer> forkResult = callTask.compute();
				
				Map<String, Integer> callResult = forkTask.join();
				
				mapToReturn.putAll(forkResult);
				Iterator<Entry<String, Integer>> it = callResult.entrySet().iterator();
			    while (it.hasNext()) {
			        Map.Entry<String, Integer> pair = it.next();
			        mapToReturn.put((String) pair.getKey(), mapToReturn.get((String) pair.getKey()) + (Integer) pair.getValue());
			        it.remove(); // avoids a ConcurrentModificationException
			    }
//				mapToReturn.putAll(callResult);
				break;
			}
		}
		return mapToReturn;
	}
	
	public Map<String, Integer> readFile(File f)
	{
		Map<String, Integer> toReturn = new ConcurrentHashMap<>();
		for (String k: Main.getKeywords())
		{
			if (!toReturn.containsKey(k))
			{
				toReturn.put(k, 0);				
			}
		}
		try
		{
			String line = "";
			BufferedReader bufferedReader = new BufferedReader(new FileReader(f));
			while ((line = bufferedReader.readLine()) != null)
			{
				for (String k: Main.getKeywords())
				{
					if (line.contains(k))
					{
						String tokens[] = line.split("[^a-zA-Z]+");
						for (String t: tokens)
						{
							if (t.equals(k))
							{
								toReturn.put(k, toReturn.get(k)+1);
							}
						}
					}
				}
			}
			bufferedReader.close();
		} 
		catch (Exception e)
		{
			System.err.println("Something went wrong while reading a file " + f.getName());
//			e.printStackTrace();
		}
		return toReturn;
	}

}
