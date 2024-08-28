package com.ericcson.eniq.procus.bo.geored;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Properties;

public class BIS_Geo_Red {
	

	
	private static String workingDir = "C:/eniq_procus_geored";
	private LogFile logger = LogFile.getInstance();
	int retention = 10;
	public void startup(String type) {
		try {
			//Create the log file
			logger.createLogFile(workingDir + "/logs");
			
			//Load properties if available. 
			File properties = new File(workingDir + "/geo_red.properties");
			Properties props = null;
			if(properties.exists()) {
				InputStream input = new FileInputStream(properties);
				props = new Properties();
				props.load(input);
			}
			
			//Create the directory to store the biar files from the exports
			File biardir = new File(workingDir, "biar");
			biardir.mkdirs();
			
			sync(props, biardir, type);


		} catch (FileNotFoundException e) {
			System.err.println("Unable to create log file. " + e);
		} catch (IOException e) {
			logger.writeToLog("Unable to load properties file. " + e);
		}finally {
			logger.closeLog();
		}

	}
	
	public void sync(Properties props, File biardir, String type) {
		if(props != null) {
			String currentDate = getCurrentDate();
			
			boolean performHousekeeping = true;
			
			BIS_Sync sync = new BIS_Sync(props, biardir, currentDate);
			if(type.equalsIgnoreCase("BIAR")) {
				logger.writeToLog("Starting BIAR Sync");
				sync.performSync();
			}else if(type.equalsIgnoreCase("LCM")) {
				logger.writeToLog("Starting LCM Sync");
				sync.performLCMSync();
			}else {
				logger.writeToLog("Invalid parameters provided: '" + type + "'");
				performHousekeeping = false;
			}
			
			
			if(performHousekeeping) {
				File backup = new File(biardir, "backup");
				try {
					retention = Integer.valueOf(new String(Base64.getDecoder().decode(props.getProperty("HousekeepingLimit")),"utf-8"));
				} catch (Exception e) {
					logger.writeToLog("Unable to fetch HouseKeepingLimit Value from the properties file. "+e);
				}

				//int retention = Integer.valueOf( (String) props.get("HousekeepingLimit") );
				ArchiveHousekeeping hk = new ArchiveHousekeeping(retention);
				
				logger.writeToLog("Performing housekeeping on source export archives");
				hk.performHousekeeping(biardir);
				logger.writeToLog("Performing housekeeping on target backup archives");
				hk.performHousekeeping(backup);
				logger.writeToLog("Performing housekeeping on logs");
				hk.performHousekeeping(new File(workingDir + "/logs"));
				logger.writeToLog("Performing housekeeping on trace logs");
				hk.cleanupTraceLogs(new File(workingDir));
			}
			
		}else {
			logger.writeToLog("Properties file not available. Unable to perform sync");
		}
	}
	
	
	private String getCurrentDate() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat dateformat = new SimpleDateFormat("yyyyMMdd");
		return dateformat.format(cal.getTime());
	}
	

	public static void main(String[] args) {
		System.out.println("entering java class --> " + args.length);
		System.out.println("First argument --> " + args[0]);
		System.out.println("Second argument --> " + args[1]);
		if(args.length == 2) {
			BIS_Geo_Red geored = new BIS_Geo_Red();
			geored.startup(args[1]);
		}else {
			System.err.println("Invalid parameters provided");
		}

	}


}
