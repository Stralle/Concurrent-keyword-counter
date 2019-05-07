package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class Main
{
	private static BufferedReader bufferedReader;
	private static String keywords[];
	private static String prefix;
	private static long sleepTime;
	private static long scanningSizeLimit;
	private static long hopCount;
	private static long urlRefreshTime;
	
	public static Map<String, Long> lastModifiedFiles = new HashMap<>();
	public static volatile BlockingQueue<Job> jobQueue = new LinkedBlockingQueue<>();
	public static volatile ResultRetriever resultRetriever = new ResultRetriever();
	public static volatile CopyOnWriteArrayList<String> scannedUrls = new CopyOnWriteArrayList<>();
	public static volatile CopyOnWriteArrayList<String> enteredUrls = new CopyOnWriteArrayList<>();
	public static volatile CopyOnWriteArrayList<String> allDomains = new CopyOnWriteArrayList<>();
	public static volatile CopyOnWriteArrayList<String> allCorpuss = new CopyOnWriteArrayList<>();
	
	public static void main(String[] args)
	{
		readConfig();
		
		System.out.println("Welcome to keyword counter. Use next commands:\nad directory_name;\naw web_url;\nget get_command - to get result (blocking);\nquery query_command - to get result (non-blocking);\ncws/cfs - clear web/file summary;\nstop - this is obvious;\n");
		Scanner scanner = new Scanner(System.in);
		DirectoryCrawler directoryCrawler = new DirectoryCrawler();				
		directoryCrawler.start();
		JobDispatcher jobDispatcher = null;
		jobDispatcher = new JobDispatcher();
		jobDispatcher.start();
		
		while (true)
		{
			String line = scanner.nextLine();
			if (line.startsWith("ad")) 
			{
				String path = line.substring("ad ".length());
				System.out.println("Adding directory -> " + path);
				if (!directoryCrawler.getDirectoriesToCrawl().contains(path))
				{
					directoryCrawler.getDirectoriesToCrawl().add(path);
				}
			}
			else if (line.startsWith("aw"))
			{
				String url = line.substring("aw ".length());
				if (!url.contains("http"))
				{
					String temp = "https://";
					url = temp + url;
				}
				
				try
				{
					Main.jobQueue.put(new JobWebScanner(ScanType.WEB, url, Main.hopCount));
					Main.enteredUrls.add(url);
					System.out.println("Adding web -> " + url);
//					clearScannedUrls();
				} 
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
			else if (line.startsWith("get"))
			{
				Main.getResultRetriever().isQuery().set(false);
				String tokens[] = line.split(" ");
				if (tokens[1].contains("file") && tokens[1].contains("FILE") && !tokens[1].contains(Main.getPrefix()))
				{
					System.err.println("Not a corpus!");
				}
				else if((tokens[1].contains("FILE") || tokens[1].contains("file") || tokens[1].contains("web") || tokens[1].contains("WEB")) && tokens[1].contains("|"))
				{
					System.out.println("processing... GET " + tokens[1]);
					Main.getResultRetriever().getResult(tokens[1]);
				}
				else
					System.err.println("Invalid number of arguments!");
			}
			else if (line.startsWith("SUMMARY") || line.startsWith("summary"))
			{
				Main.getResultRetriever().isQuery().set(false);
				if (line.contains("WEB") || line.contains("web"))
				{
					Main.getResultRetriever().getSummary(ScanType.WEB);
				}
				else if (line.contains("FILE") || line.contains("file"))
				{
					Main.getResultRetriever().getSummary(ScanType.FILE);					
				}
				else
				{
					System.err.println("Wrong command! Try SUMMARY WEB or SUMMARY FILE");
				}
			}
			else if (line.startsWith("query get") || line.startsWith("QUERY GET"))
			{
				Main.getResultRetriever().isQuery().set(true);
				String tokens[] = line.split(" ");
				if ((tokens[2].contains("file") || tokens[2].contains("FILE")) && !tokens[2].contains(Main.getPrefix()))
				{
					System.err.println("Not a corpus!");
				}
				else if((tokens[2].contains("FILE") || tokens[2].contains("file") || tokens[2].contains("web") || tokens[2].contains("WEB")) && tokens[2].contains("|"))
				{
					System.out.println("processing... GET " + tokens[2]);
					Main.getResultRetriever().queryResult(tokens[2]);
				}
				else
					System.err.println("Invalid number of arguments!");
//				System.out.println("Getting query result.");
			}
			else if (line.startsWith("query summary") || line.startsWith("QUERY SUMMARY"))
			{
				Main.getResultRetriever().isQuery().set(true);
				if (line.contains("WEB") || line.contains("web"))
				{
					Main.getResultRetriever().querySummary(ScanType.WEB);
				}
				else if (line.contains("FILE") || line.contains("file"))
				{
					Main.getResultRetriever().querySummary(ScanType.FILE);					
				}
				else
				{
					System.err.println("Wrong command! Try QUERY SUMMARY WEB or QUERY SUMMARY FILE");
				}
			}
			else if (line.startsWith("cws"))
			{
				System.out.println("Clearing web summary.");
				Main.getResultRetriever().clearSummary(ScanType.WEB);
			}
			else if (line.startsWith("cfs"))
			{
				System.out.println("Clearing file summary");
				Main.getResultRetriever().clearSummary(ScanType.FILE);
			}
			else if (line.startsWith("stop"))
			{
				System.out.println("Stopping application.");
				// TODO: stop all threads.
				directoryCrawler.stopIt();
				jobDispatcher.stopIt();
				scanner.close();
				return;
			}
			else 
			{
				System.out.println("Wrong command entered!");
			}
		}
	}
		
	public static void readConfig()
	{
		File configFile = new File("app.properties");
		String command = "";
		keywords = new String[]{};
		prefix = "";
		sleepTime = 0;
		scanningSizeLimit = 0;
		hopCount = 0;
		urlRefreshTime = 0;
		try
		{
			bufferedReader = new BufferedReader(new FileReader(configFile));
			while ((command = bufferedReader.readLine()) != null)
			{
				if (command.startsWith("keywords="))
				{
					keywords = command.substring("keywords=".length()).split(",");
				}
				else if (command.startsWith("file_corpus_prefix="))
				{
					prefix = command.substring("file_corpus_prefix=".length());
				}
				else if (command.startsWith("dir_crawler_sleep_time="))
				{
					sleepTime = Long.parseLong(command.substring("dir_crawler_sleep_time=".length()));
				}
				else if (command.startsWith("file_scanning_size_limit="))
				{
					scanningSizeLimit = Long.parseLong(command.substring("file_scanning_size_limit=".length()));
				}
				else if (command.startsWith("hop_count=")) 
				{
					hopCount = Long.parseLong(command.substring("hop_count=".length()));
				}
				else if (command.startsWith("url_refresh_time="))
				{
					urlRefreshTime = Long.parseLong(command.substring("url_refresh_time=".length()));
				}
				else 
				{
					System.out.println("There\'s an error in your config file.");
					return;
				}
			}
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	static void clearScannedUrls()
	{
		try
		{
			System.out.println("Sleeping for " + Main.getUrlRefreshTime());
			Thread t = new Thread();
			t.sleep(Main.getUrlRefreshTime());
		} catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		Main.scannedUrls.clear();
		System.out.println("Scanned urls are cleared now");
	}
	
	public static String[] getKeywords()
	{
		return keywords;
	}

	public static void setKeywords(String[] keywords)
	{
		Main.keywords = keywords;
	}

	public static String getPrefix()
	{
		return prefix;
	}

	public static void setPrefix(String prefix)
	{
		Main.prefix = prefix;
	}

	public static long getSleepTime()
	{
		return sleepTime;
	}

	public static void setSleepTime(long sleepTime)
	{
		Main.sleepTime = sleepTime;
	}

	public static long getHopCount()
	{
		return hopCount;
	}

	public static void setHopCount(long hopCount)
	{
		Main.hopCount = hopCount;
	}

	public static long getUrlRefreshTime()
	{
		return urlRefreshTime;
	}

	public static void setUrlRefreshTime(long urlRefreshTime)
	{
		Main.urlRefreshTime = urlRefreshTime;
	}

	public static long getScanningSizeLimit()
	{
		return scanningSizeLimit;
	}

	public static void setScanningSizeLimit(long fileScanningSize)
	{
		Main.scanningSizeLimit = fileScanningSize;
	}
	
	public static ResultRetriever getResultRetriever()
	{
		return resultRetriever;
	}

}
