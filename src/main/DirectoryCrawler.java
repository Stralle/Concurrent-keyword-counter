package main;

import java.io.File;
import java.io.FilenameFilter;
import java.util.concurrent.CopyOnWriteArrayList;

public class DirectoryCrawler extends Thread
{
	private volatile boolean working = true;
	private CopyOnWriteArrayList<String> directoriesToCrawl;

	public DirectoryCrawler()
	{
		this.setDirectoriesToCrawl(new CopyOnWriteArrayList<>());
	}

	@Override
	public void run()
	{
		System.out.println("Starting DirectoryCrawler RUNNING");
		while (working)
		{
			for (String path: this.directoriesToCrawl)
			{
				searchDirectory(path);
			}
			try
			{
				Thread.sleep(Main.getSleepTime());
//				System.out.println("Odspavao je svoje.");
			} 
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		this.getDirectoriesToCrawl().clear();
		System.out.println("Crawler stopped!!!");
	}

	public void searchDirectory(String path)
	{
//		System.out.println("Searching " + path);
		try 
		{
			File file = new File(path);
			String[] directories = file.list(new FilenameFilter()
			{
				@Override
				public boolean accept(File current, String name)
				{
					return new File(current, name).isDirectory();
				}
			});

			for (String currentDirectory : directories)
			{
//				System.out.println("--------\nCurr dir:" + currentDirectory);
				String newPath = path + "/" + currentDirectory;
				try 
				{
					File newFile = new File(newPath);
					if (newFile.isDirectory())
					{
						if (!currentDirectory.startsWith(Main.getPrefix()))
						{
//							System.out.println("Newfile\'s path: " + newFile.getPath());
							searchDirectory(newPath);
						} 
						else
						{
//							System.out.println("Its a corpus");
							if (!Main.allCorpuss.contains(currentDirectory))
							{
								Main.allCorpuss.add(currentDirectory);
							}
							String[] matchingFiles = newFile.list(new FilenameFilter()
							{
								@Override
								public boolean accept(File current, String name)
								{
									return name.endsWith("txt");
								}
							});
							boolean differentLastModified = false;
							for (String currentFile : matchingFiles)
							{
								File currentMatchingFile = new File(newPath + "/" + currentFile);
								if (!Main.lastModifiedFiles.containsKey(currentFile))
								{
									Main.lastModifiedFiles.put(currentFile, currentMatchingFile.lastModified());
									differentLastModified = true;
								} else
								{
									if (Main.lastModifiedFiles.get(currentFile) != currentMatchingFile.lastModified())
									{
										Main.lastModifiedFiles.replace(currentFile, currentMatchingFile.lastModified());
										differentLastModified = true;
									}
								}
							}
							
							if (differentLastModified)
							{
								assingNewJob(newFile.getPath());
							}
						}
					}
				}
				catch (Exception e)
				{
					System.err.println("Cant open " + newPath);
					continue;
				}

			}
		}
		catch (Exception e)
		{
			System.err.println("Something went wrong with " + path);
			this.stopIt();
		}
		
	}

	public void assingNewJob(String path)
	{
		System.out.println("Adding corpus " + path + " to queue.");
		try
		{
			Main.jobQueue.put(new JobFileScanner(ScanType.FILE, path));
		} 
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
	
	public boolean isWorking()
	{
		return this.working;
	}

	public void stopIt()
	{
		System.out.println("Stopping crawler");
		this.working = false;
	}

	public CopyOnWriteArrayList<String> getDirectoriesToCrawl()
	{
		return directoriesToCrawl;
	}

	public void setDirectoriesToCrawl(CopyOnWriteArrayList<String> directoriesToCrawl)
	{
		this.directoriesToCrawl = directoriesToCrawl;
	}

}
