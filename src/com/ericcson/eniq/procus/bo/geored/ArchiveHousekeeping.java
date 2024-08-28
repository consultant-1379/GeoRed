package com.ericcson.eniq.procus.bo.geored;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ArchiveHousekeeping {
	


	private LogFile logger = LogFile.getInstance();
	private int retention;
	private SimpleDateFormat dateformat = new SimpleDateFormat("yyyyMMdd");
	
	public ArchiveHousekeeping(int retention) {
		this.retention = retention;
	}
	
	public void performHousekeeping(File archiveDir){
		try { 
			logger.writeToLog("Performnig housekeeping");
			
			Calendar retentionDate = Calendar.getInstance();
			retentionDate.set(Calendar.DATE, retentionDate.get(Calendar.DATE) - retention);
			logger.writeToLog("Cut off date is " + dateformat.format(retentionDate.getTime()));
			
			for (File file : archiveDir.listFiles()) {
				if (file.isFile()) {

					FileTime creationtime = Files.readAttributes(file.toPath(), BasicFileAttributes.class)
							.creationTime();

					Calendar time = Calendar.getInstance();
					time.setTimeInMillis(creationtime.toMillis());

					if (time.before(retentionDate)) {
						logger.writeToLog("Archive past retention period. FILE NAME: " + file.getName() + ", CREATED: "
								+ dateformat.format(time.getTime()));
						file.delete();
						logger.writeToLog(file.getAbsolutePath() + " deleted");
					}
				}

			}
		
		}catch(IOException e) {
			logger.writeToLog("Unable to perform housekeeping. " + e);
		}
		
		
	}
	
	public void cleanupTraceLogs(File archiveDir){
		try { 
			logger.writeToLog("Performnig housekeeping");
			
			Calendar retentionDate = Calendar.getInstance();
			retentionDate.set(Calendar.DATE, retentionDate.get(Calendar.DATE) - retention);
			logger.writeToLog("Cut off date is " + dateformat.format(retentionDate.getTime()));
			
			for (File file : archiveDir.listFiles()) {
				if (file.isFile()) {
					if(file.getName().startsWith("TraceLog")) {

						FileTime creationtime = Files.readAttributes(file.toPath(), BasicFileAttributes.class)
								.creationTime();
	
						Calendar time = Calendar.getInstance();
						time.setTimeInMillis(creationtime.toMillis());
	
						if (time.before(retentionDate)) {
							logger.writeToLog("Archive past retention period. FILE NAME: " + file.getName() + ", CREATED: "
									+ dateformat.format(time.getTime()));
							file.delete();
							logger.writeToLog(file.getAbsolutePath() + " deleted");
						}
					}
				}

			}
		
		}catch(IOException e) {
			logger.writeToLog("Unable to perform housekeeping. " + e);
		}
		
		
	}
	
}
