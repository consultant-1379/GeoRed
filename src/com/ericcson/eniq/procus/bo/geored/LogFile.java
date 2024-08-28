package com.ericcson.eniq.procus.bo.geored;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class LogFile {
	

	
	private static LogFile single_instance = null;
	private PrintStream out = null;
	
	public void createLogFile(String logDirectory) throws FileNotFoundException {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat filenameformat = new SimpleDateFormat("yyyyMMdd_HHmmss");
		
		File logdir = new File(logDirectory);
		logdir.mkdirs();
		
		File logfile = new File(logdir, filenameformat.format(cal.getTime()) + ".log");
		
		out = new PrintStream(logfile);
		System.setOut(out);
		System.setErr(out);
		
	}
	
	public boolean isLogAvailable() {
		if(out == null) {
			return false;
		}
		return true;
	}
	
	public void writeToLog(String message) {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat filenameformat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		
		out.println(filenameformat.format(cal.getTime()) + " : " + message);
		out.flush();
	}
	
	public void closeLog() {
		out.flush();
		out.close();
	}
	
	public static LogFile getInstance() 
    { 
        if (single_instance == null) 
            single_instance = new LogFile(); 
  
        return single_instance; 
    } 
	


}
