package main;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveTask;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WebFileTask extends RecursiveTask<Map<String, Integer>>
{
	private static final long serialVersionUID = 8629537837791731156L;
	private String jobPath;
	private long hopCount;

	public WebFileTask(String jobPath, long hopCount)
	{
		this.jobPath = jobPath;
		this.hopCount = hopCount;
	}
	
	@Override
	protected Map<String, Integer> compute()
	{
		Map<String, Integer> mapToReturn = new ConcurrentHashMap<>();
		for (String k: Main.getKeywords())
		{
			if (!mapToReturn.containsKey(k))
			{
				mapToReturn.put(k, 0);				
			}
		}
		try
		{
			Document document = Jsoup.connect(jobPath).get();
			
			if (this.hopCount >= 1)
			{
				Elements links = document.select("a[href]");
				for (Element link : links) {
					String url = link.attr("abs:href");
					if (Main.scannedUrls.contains(url))
					{
						continue;
					}
					Main.scannedUrls.add(url);
//					if (url.charAt(url.length() - 1) == '/')
//					{
//						String url1 = url + "#";
//						Main.scannedUrls.add(url1);
//					}
//					else if (url.charAt(url.length() - 1) != '#')
//					{
//						String url1 = url + "#";
//						String url2 = url + "/#";
//						String url3 = url + "/";
//						Main.scannedUrls.add(url1);
//						Main.scannedUrls.add(url2);
//						Main.scannedUrls.add(url3);
//					}
//					System.out.println("Added urls:");
//					for (String u: Main.scannedUrls)
//					{
//						System.out.println("\t" + u);
//					}
					try
					{
						URL uri = new URL(url);
						String host = uri.getHost();
						if (!Main.allDomains.contains(host))
						{
							Main.allDomains.add(host);
							System.out.println("Adding domain: " + host);
						}
					} catch (MalformedURLException e)
					{
						e.printStackTrace();
					}				
					Main.jobQueue.put(new JobWebScanner(ScanType.WEB, url, this.hopCount - 1));	            	
				}				
			}
			
			String webPageText = document.text();
			String tokenz[] = webPageText.split("[\\p{Punct}\\s]+");
			for (String w: tokenz)
			{
				if (mapToReturn.containsKey(w))
				{
					Integer value = mapToReturn.get(w) + 1;
					mapToReturn.put(w, value);
				}
			}
		}
		catch (Exception e)
		{
			System.err.println("Something went wrong while accessing " + this.jobPath);
		}
		return mapToReturn;
	}
	
	public long getHopCount()
	{
		return hopCount;
	}
	
	public void setHopCount(long hopCount)
	{
		this.hopCount = hopCount;
	}
	
	public String getJobPath()
	{
		return jobPath;
	}
	
	public void setJobPath(String jobPath)
	{
		this.jobPath = jobPath;
	}
}
