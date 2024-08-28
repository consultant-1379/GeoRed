package com.ericcson.eniq.procus.bo.geored;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import com.businessobjects.lcm.utilities.LCMException;
import com.businessobjects.sdk.biar.BIARException;
import com.businessobjects.sdk.biar.BIARFactory;
import com.businessobjects.sdk.biar.IExportOptions;
import com.businessobjects.sdk.biar.IImportOptions;
import com.businessobjects.sdk.biar.om.IManagedObjectIterator;
import com.businessobjects.sdk.biar.om.IObjectManager;
import com.businessobjects.sdk.biar.om.OMException;
import com.businessobjects.sdk.lcm.ExportScenario;
import com.businessobjects.sdk.lcm.Host;
import com.businessobjects.sdk.lcm.HostFactory;
import com.businessobjects.sdk.lcm.ImportScenario;
import com.businessobjects.sdk.lcm.LcmArchive;
import com.businessobjects.sdk.lcm.LcmArchiveFactory;
import com.businessobjects.sdk.lcm.ObjectBrowser;
import com.businessobjects.sdk.plugin.desktop.recyclebin.IRecycleBinObject;
import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.framework.CrystalEnterprise;
import com.crystaldecisions.sdk.framework.IEnterpriseSession;
import com.crystaldecisions.sdk.occa.infostore.ICommitError;
import com.crystaldecisions.sdk.occa.infostore.ICommitResult;
import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;
import com.crystaldecisions.sdk.plugin.desktop.user.IUser;

public class BIS_Sync {

	
	private LogFile logger = LogFile.getInstance();
	private BIS_Queries bis_queries;
	private Properties properties;
	private File biardir;
	private String currentDate;
	private boolean backupFlag = false;
	private boolean exportFlag = false;
	
	public BIS_Sync(Properties properties, File biardir, String currentDate) {
		this.properties = properties;
		this.bis_queries = new BIS_Queries();
		this.biardir = biardir;
		this.currentDate = currentDate;
	}

	public void performSync() {
		HashMap<String,String> importsource = new HashMap<String,String>();
		IEnterpriseSession targetSession = null;
		try {
			//Open a connection to the source and target systems
			logger.writeToLog("Establishing connections to the source and target systems");
			IEnterpriseSession sourceSession = openConnection("source", properties);
			targetSession = openConnection("target", properties);
			
			//Take a back up of the target system just in case there are issues
			File backup = new File(biardir, "backup");
			backup.mkdirs();
			logger.writeToLog("Taking backup of target system");
			
			HashMap<String,String> backupData = exportData(targetSession, backup);
			if( backupData!= null){
				importsource.putAll(backupData);
				logger.writeToLog("Target backup complete");
				backupFlag = true;
			}else{
				logger.writeToLog("Target backup failed");
				backupFlag = false;
			}
			
			//Export data from the source system
			logger.writeToLog("Exporting data from source system");
			HashMap<String,String> exportArchives = exportData(sourceSession, biardir);
			logger.writeToLog("Data export complete");
			
			if(exportArchives != null){
				exportFlag = true;
				importsource.clear(); 
				importsource.putAll(exportArchives);
			}else{
				exportFlag = false;
			}
			if(backupFlag == true && exportFlag == true){
				//Delete from the target
				logger.writeToLog("Cleaning up target system ahead of sync");
				deleteData(targetSession);
				logger.writeToLog("Target clean up complete");
			}else{
				logger.writeToLog("Something went wrong , Skipping the deletion from target server ");
			}
			
			/*
			 * importsource.clear(); importsource.putAll(exportArchives);
			 */
		} catch (SDKException e) {
			logger.writeToLog("Unable to perform sync. " + e);
			e.printStackTrace();
		} catch (BIARException e) {
			logger.writeToLog("Unable to perform sync. " + e);
			e.printStackTrace();
		}
		
		//Import data into the target system
		try {
			
			if(exportFlag) {
				if(targetSession != null) {
					logger.writeToLog("Importing data to target system");
					importData(targetSession, importsource);
					logger.writeToLog("Data import complete");
				}
			}else {
				logger.writeToLog("Source Export got failed so sync can not be proceeded");
			}
			
		}catch (SDKException e) {
			logger.writeToLog("Unable to perform sync. " + e);
			e.printStackTrace();
		} catch (BIARException e) {
			logger.writeToLog("Unable to perform sync. " + e);
			e.printStackTrace();
		}catch(Exception e){
			logger.writeToLog("Unable to perform sync. " + e);
			e.printStackTrace();
		}
//		
		
	}
	
	public void performLCMSync() {
		ArrayList<String> importsource = new ArrayList<String>();
		IEnterpriseSession targetSession = null;
		IEnterpriseSession sourceSession = null;
		try {
			//Open a connection to the source and target systems
			sourceSession = openConnection("source", properties);
			targetSession = openConnection("target", properties);
			
			//Take a back up of the target system just in case there are issues
			File backup = new File(biardir, "backup");
			backup.mkdirs();
			importsource.addAll( exportLCMData(targetSession, backup) );
			
			//Export data from the source system
			ArrayList<String> exportArchives = exportLCMData(sourceSession, biardir);
			
			//Delete from the target
			deleteData(targetSession);
			
			importsource.clear();
			importsource.addAll(exportArchives);
			
		} catch (SDKException e) {
			logger.writeToLog("Unable to perform sync. " + e);
			e.printStackTrace();
		} catch (LCMException e) {
			logger.writeToLog("Unable to perform sync. " + e);
			e.printStackTrace();
		}
		
		//Import data into the target system
		try {
			
			if(targetSession != null) {
				importLCMData(targetSession, importsource);
			}

		}catch (SDKException e) {
			logger.writeToLog("Unable to perform sync. " + e);
			e.printStackTrace();
		}catch (LCMException e) {
			logger.writeToLog("Unable to perform sync. " + e);
			e.printStackTrace();
		} catch (OMException e) {
			logger.writeToLog("Unable to perform sync. " + e);
			e.printStackTrace();
		}
		
		sourceSession.logoff();
		targetSession.logoff();
		
	}
	
	private ArrayList<String> exportLCMData(IEnterpriseSession session, File outputdir) throws SDKException, LCMException {
		ArrayList<String> filescreated = new ArrayList<String>();
		
		logger.writeToLog("Creating Scenario");
		//Create a new LCM Job to export to lcmbiar file
		Host host = HostFactory.newInstance(session);
		ExportScenario myScenario = host.newExportScenario("ProCus_GeoRed_Export");
		
		logger.writeToLog("Creating LCM archive");
		//set the export destination for the LCMBiar file
		LcmArchive myExport = LcmArchiveFactory.newInstance(outputdir.getAbsolutePath() + "/" + currentDate + "_export.lcmbiar");
		myScenario.setDestination(myExport);
		
		logger.writeToLog("Creating Object Browser");
		//Add objects to be exported
		ObjectBrowser oBrowser = myScenario.setSource(session);
		oBrowser.addQuery(bis_queries.Export_Query_1);
		oBrowser.addQuery(bis_queries.Export_Query_2);
		oBrowser.addQuery(bis_queries.Export_Query_3);
		
		logger.writeToLog("Computing dependencies");
		oBrowser.computeDependencies();
		for (Set<IInfoObject> dependencies :oBrowser.getDependencies().values())
		{
			oBrowser.addObjects(dependencies);
		}
		
		logger.writeToLog("Running scenario");
		//Run the scenario and release the host
		myScenario.run();
		host.release();
		
		filescreated.add(outputdir.getAbsolutePath() + "/" + currentDate + "_export.lcmbiar");
		
		logger.writeToLog("Export Complete");
		
		return filescreated;
		
	}
	
	private void importLCMData(IEnterpriseSession session, ArrayList<String> filesToImport) throws SDKException, OMException, LCMException {
		for(String filename : filesToImport) {
			
			logger.writeToLog("Importing " + filename);
			//Create a new LCM Job to import lcmbiar file
			Host host = HostFactory.newInstance(session);
			ImportScenario myScenario = host.newImportScenario("ProCus_GeoRed_Import");
			
			logger.writeToLog("Loading archive");
			//load source file for import job.
			LcmArchive importFile = LcmArchiveFactory.newInstance(filename);
			importFile.getFile();
			myScenario.setSource(importFile);
			myScenario.setDestination(session);
			
			logger.writeToLog("Running scenario");
			myScenario.run();
			
			host.release();
			
			logger.writeToLog("Import Complete");
		}
	}
	
	
	
	private void importData(IEnterpriseSession session, HashMap<String,String> filesToImport) throws SDKException, OMException {
		String users = filesToImport.get("Users");
		String reports = filesToImport.get("Reports");		
		
		runImport(session, users);
		runImport(session, reports);
	}
	
	private void runImport(IEnterpriseSession session, String filename) throws OMException, SDKException {
		logger.writeToLog("Creating BIAR Manager");
		BIARFactory biarFactory = BIARFactory.getFactory();
		IObjectManager objectManager = biarFactory.createOM(session);
        
		logger.writeToLog("Setting Import options");
		IImportOptions importOptions = biarFactory.createImportOptions();
		importOptions.setIncludeSecurity(true);
		importOptions.setFailUnresolvedCUIDs(false);
		
		
		try {
			
			
			logger.writeToLog("Importing archive " + filename);
			
			if(filename.endsWith("_users.lcmbiar")){
				
				objectManager.importArchive(filename, importOptions);
				
				ArrayList<String> cuids = commit(objectManager);
				logger.writeToLog(cuids.size() + " objects processed.");
				objectManager.deleteAllCuids(cuids.iterator());
				
				if (objectManager.size() > 0) {
					logger.writeToLog("Import produced errors, retrying import for objects with errors");
					commit(objectManager);
					logger.writeToLog(objectManager.size() + " objects processed ");
				}

			}else{
				String[] type = {"Folder","FavoritesFolder","PersonalCategory","Inbox","Universe","DSL.MetaDataFile","Webi","FullClient","CrystalReport","Category","Hyperlink","Txt","Excel","Pdf","Word","Rtf"};
				
				objectManager.importArchive(filename, importOptions);
				
				for(String str : type){
					
					try {

						IObjectManager universesOM = biarFactory.createOM(session);
		                IManagedObjectIterator managedObjectIter = objectManager.readAllKind(str);
		                while(managedObjectIter.hasNext()) {
		                				
		                	IInfoObject infoObject = managedObjectIter.next();
		                    universesOM.insert(infoObject);
		                }
		                
		                managedObjectIter.close();

		                if(universesOM.size()>0) {
		                               logger.writeToLog("Found "+universesOM.size()+ " " +str+ " in archive ");

		                               ArrayList cuids = commit(universesOM);
		                               logger.writeToLog("Committed "+cuids.size()+ " " + str);
		                               Iterator cuidsIter = cuids.iterator(); 
		                               
		                               //First Verification
		                               IObjectManager failed =  verification(universesOM,str,session);
		                               
		                               if(failed != null && failed.size()>0){
		                            	   // Trying to reimport the objects
		    	                           logger.writeToLog("Number of Failed Object of type " +str +" "+ failed.size() );
		    	                           
		    	                           //Deleting from CMS
		    	                           deleteFailedObject(failed, session);
		    	                        	   
		    	                           // ReImport
		    	                           	commit(failed);
		    	                           	
		    	                           	// Second Verification
		    	                           	IObjectManager reImportFailed = verification(failed,str,session);
		    	                           	
		    	                           	if(reImportFailed == null){
		    	                           		logger.writeToLog("Unable to import" + reImportFailed.size() + " "+ str+ " failed object ");
		    	                           		
		    	               				}else if(reImportFailed.size() >0){
		    	               					logger.writeToLog("Unable to import" + reImportFailed.size() + " "+ str+ " failed object ");
		    	                           		
		    	                           		// Deleting the Objects from CMS if they fails in the second attempt as well
			    	                           	deleteFailedObject(reImportFailed, session);
			    	                           	reImportFailed.dispose();
		    	                           	
		    	               				}else{
		    	               					logger.writeToLog("No Objects of "+ str + " type has failed while reimport");
		    	               					reImportFailed.dispose();
		    	               				}

		    	                           	
		                               }else if(failed == null){
		                            	   logger.writeToLog("WARNING: Unable to Verify the Objects imported");
		                               }else{
		                            	   logger.writeToLog("No Object of type "+str+ " has failed");
		                            	   failed.dispose(); 
		                               }
		                           
		                }
		                // Disposing UniverseOM after importing every object   
		                universesOM.dispose();

					} catch (Exception e) {
						logger.writeToLog("WARNING: Exception while importing "+ str + " Objects " + e);
					}
				}
			}

		} catch (BIARException e) {
			logger.writeToLog("ERROR importing: " + e.getMessage());
		}finally{
			objectManager.dispose();
		}
}
	
public void deleteFailedObject(IObjectManager universesOM,IEnterpriseSession session){

		try {
			
			IManagedObjectIterator managedObjectIter1 = universesOM.readAll();
		  	   ArrayList<String> deletedTitles = new ArrayList<String>();
		  	   
		         while(managedObjectIter1.hasNext()){
		      	   
		      	   IInfoObject infoObject = managedObjectIter1.next();
		      	   logger.writeToLog("Deleting  from CMS" + "SI_CUID: " + infoObject.getCUID() + ", SI_ID: " + infoObject.getID()
						+ ", SI_NAME: " + infoObject.getTitle() + ", SI_KIND: " + infoObject.getKind());
		      	   deletedTitles.add(infoObject.getTitle() + " - " + infoObject.getID());
		      	   infoObject.deleteNow();
		      	   

		         }
		         managedObjectIter1.close();
		         universesOM.dispose();
		         
		         
		         // Removing from Recycle bin
		         String queryString = "SELECT TOP 100000 * FROM CI_INFOOBJECTS WHERE SI_KIND = '" +IRecycleBinObject.KIND+"'";
		         IInfoStore infoStore = (IInfoStore) session.getService("InfoStore");
		         IInfoObjects infoObjects = infoStore.query(queryString);
		 			for(int i=0; i<infoObjects.size(); i++) {
		 				IRecycleBinObject infoObject = (IRecycleBinObject) infoObjects.get(i);
		 				logger.writeToLog("Removing from RecyleBin SI_CUID: " + infoObject.getCUID() 
		 				+ ", SI_NAME: " + infoObject.getTitle() + ", SI_KIND: " + infoObject.getKind());
		 				
		 					infoObject.deleteNow();
		 		}
		} catch (Exception e) {
			logger.writeToLog("Exception while deleting from the CMS "+e);
		}
	}	

public IObjectManager verification( IObjectManager universesOM, String str, IEnterpriseSession session){

		boolean result = false;
		IObjectManager failed =  null;
		try {
			BIARFactory biarFactory = BIARFactory.getFactory();
			failed = biarFactory.createOM(session);
			
			IInfoStore infoStore = (IInfoStore) session.getService("InfoStore");
			IManagedObjectIterator managedObjectIter1 = universesOM.readAllKind(str);
			
			
			File dir = new File("C:/ebid/install/");
			String[] files = dir.list();
			String FRS = "";
			String dirPath = "";
			if (files.length == 0) {
			    System.out.println("The instalation directory is empty");
			} else {
				
			    for (String aFile : files) {
			    	File file =  new File("C:/ebid/install/"+aFile);
					BufferedReader br;
					
					
					try {
						br = new BufferedReader(new FileReader(file));
						String st; 
						 while ((st = br.readLine()) != null){ 
						   if(st.startsWith("installdir=")){
							   FRS = st.substring("installdir=".length());
							   break;
						   }
						   
						 }

					} catch (FileNotFoundException e) {
						logger.writeToLog("WARNING: Unable to find the "+file+ " directory" + e);
						e.printStackTrace();
					} catch (Exception e) {
						logger.writeToLog("WARNING: Exception occured while Creating FRS File path" + e);
					}
			    	
			    }
			    
			    String[] dirArray = FRS.split("\\\\");
			    
			    
			    for(int i = 0; i<dirArray.length; i++){
			    	dirPath = dirPath.concat(dirArray[i] + "/");
			    }
			    
			}

			while(managedObjectIter1.hasNext()){
				IInfoObject infoObject = managedObjectIter1.next();
				
				// Checking in CMS and File store
				String CUID = infoObject.getCUID();
				try {
					
					// Check in CMS
					String queryString = "select TOP 100000 * from CI_INFOOBJECTS,CI_APPOBJECTS,CI_SYSTEMOBJECTS where SI_CUID='"+CUID+"'";
					IInfoObjects queryResult = infoStore.query(queryString);
					int querySize = queryResult.getResultSize();
					
					if(querySize ==  0){
						failed.insert(infoObject);
					
					}else{
						
						if(str.equals("Webi") || str.equals("FullClient") || str.equals("Universe") || str.equals("DSL.MetaDataFile")){
							
							// Check in File store
							    
							    File directory =  new File(dirPath.concat("SAP BusinessObjects Enterprise XI 4.0/FileStore/" + infoObject.getFiles().getFRSPathURL().substring("frs://".length())));

							    if(directory.isDirectory() && directory.list().length == 0) {
									failed.insert(infoObject);
								} 
							}

					}
					
					
				} catch (Exception e) {
					
					logger.writeToLog("WARNING Verification failing on objects having SI_CUID " + infoObject.getCUID() + 
				              ", SI_NAME: " + infoObject.getTitle() + ", SI_KIND: " + infoObject.getKind() + e);
				}
				
				}
	
			managedObjectIter1.close();
			

		} catch (Exception e) {
			logger.writeToLog("WARNING:  Exception occured while verification "+e);
			return null;
		}

		return failed;
		
	}
	

	public ArrayList<String> commit(IObjectManager objectManager) throws OMException, SDKException {

		ArrayList<String> cuids = new ArrayList<String>();
		
		try {
			ICommitResult result = objectManager.commit();
			
			HashMap<String, String> errorIDs = new HashMap<String, String>();

			Iterator<ICommitError> errors = result.getErrors().iterator();
			while (errors.hasNext()) {
				ICommitError error = errors.next();
				for (Object id : error.getIDs()) {
					errorIDs.put(String.valueOf((int) id), error.getCMSMessage());
				}
			}
			
			IManagedObjectIterator managedObjectIter = objectManager.readAll();
			
			while (managedObjectIter.hasNext()) {
				IInfoObject infoObject = managedObjectIter.next();
				String ID = String.valueOf(infoObject.getID());
				String error = "";
				if (!errorIDs.containsKey(ID)) {
					cuids.add(infoObject.getCUID());
				}else {
					error = errorIDs.get(ID);
				}
				
				if(error.length() > 0){
					
					logger.writeToLog("SI_CUID: " + infoObject.getCUID() + ", SI_ID: " + infoObject.getID()
					+ ", SI_NAME: " + infoObject.getTitle() + ", SI_KIND: " + infoObject.getKind() + ", Warning: " + error);
				}else{
					logger.writeToLog("SI_CUID: " + infoObject.getCUID() + ", SI_ID: " + infoObject.getID()
					+ ", SI_NAME: " + infoObject.getTitle() + ", SI_KIND: " + infoObject.getKind());
				}
				
				
			}
			managedObjectIter.close();
		} catch (Exception e) {
			logger.writeToLog("WARNING: Unable to import the data to target from the archieve "+e);
		}

		return cuids;
	}
	
	
	private void createUsers(IEnterpriseSession session, IObjectManager objectManager) throws OMException, SDKException{
		IManagedObjectIterator managedObjectIter = objectManager.readAll();
		
		IInfoStore infostore = (IInfoStore) session.getService("InfoStore");  		
		IInfoObjects newUsers = infostore.newInfoObjectCollection();
		
		while (managedObjectIter.hasNext()) {
			IInfoObject infoObject = managedObjectIter.next();
			if(infoObject.getKind().equals(IUser.KIND)){
				try {
					IUser user = (IUser) infoObject;
					newUsers.add(user);
					infostore.commit(newUsers);
				}catch(Exception e) {
					logger.writeToLog("Error creating user. " + e.getMessage());
				}
			}
			
		}
		managedObjectIter.close();
	}
	
	private HashMap<String, String> exportData(IEnterpriseSession session, File outputdir) throws SDKException, BIARException{
		
		HashMap<String, String> filescreated = new HashMap<String, String>();

		try {
			
			IInfoStore infoStore = (IInfoStore) session.getService("InfoStore");
			
			logger.writeToLog("Executing Export Query 1");
			IInfoObjects query1 = infoStore.query(bis_queries.Export_Query_1);
			
			logger.writeToLog("Executing Export Query 2");
			IInfoObjects query2 = infoStore.query(bis_queries.Export_Query_2);
			
			logger.writeToLog("Creating BIAR archive");
			BIARFactory biarFactory = BIARFactory.getFactory();
			IObjectManager objectManager = biarFactory.createOM(session);
			objectManager.insertAll(query2.iterator());
			objectManager.insertAll(query1.iterator());

			logger.writeToLog("Setting export options");
			IExportOptions exportOptions = biarFactory.createExportOptions();
			exportOptions.setIncludeSecurity(true);
			exportOptions.setFailUnresolvedIDs(false);
			//exportOptions.setIncludeDependencies(true);

			logger.writeToLog("Exporting archive");
			objectManager.exportToArchive(outputdir.getAbsolutePath() + "/" + currentDate + "_export.lcmbiar", exportOptions);
			filescreated.put("Reports", outputdir.getAbsolutePath() + "/" + currentDate + "_export.lcmbiar");
			listObjects(objectManager);
			
			logger.writeToLog("Export complete. " + objectManager.size() + " objects exported.");
			objectManager.dispose();
			
			logger.writeToLog("Executing Export Query 3");
			IInfoObjects query3 = infoStore.query(bis_queries.Export_Query_3);
			
			logger.writeToLog("Creating BIAR archive");
			biarFactory = BIARFactory.getFactory();
			objectManager = biarFactory.createOM(session);
			objectManager.insertAll(query3.iterator());

			logger.writeToLog("Setting export options");
			exportOptions = biarFactory.createExportOptions();
			exportOptions.setIncludeSecurity(true);
			exportOptions.setFailUnresolvedIDs(false);

			logger.writeToLog("Exporting archive");
			objectManager.exportToArchive(outputdir.getAbsolutePath() + "/" + currentDate + "_export_users.lcmbiar", exportOptions);
			filescreated.put("Users", outputdir.getAbsolutePath() + "/" + currentDate + "_export_users.lcmbiar");
			listObjects(objectManager);
			
			logger.writeToLog("Export complete. " + objectManager.size() + " objects exported.");
			
			objectManager.dispose();
		} catch (Exception e) {
			logger.writeToLog("WARNING: Taking Backup from the target or creation of source export got failed " + e);
			return null;
		}
		return filescreated;
	}
	
	private void listObjects(IObjectManager objectManager) throws OMException, SDKException {
		IManagedObjectIterator managedObjectIter = objectManager.readAll();

		while (managedObjectIter.hasNext()) {
			IInfoObject infoObject = managedObjectIter.next();
			logger.writeToLog("Exported SI_CUID: " + infoObject.getCUID() 
				+ ", SI_ID: " + infoObject.getID() +
				", SI_NAME: " + infoObject.getTitle() +
				", SI_KIND: " + infoObject.getKind());
			
		}
		managedObjectIter.close();
	}
	
	
	private void deleteData(IEnterpriseSession session) throws SDKException{
		logger.writeToLog("Running Delete Query 1");
		delete(session, bis_queries.Delete_Query_1);
		
		logger.writeToLog("Running Delete Query 2");
		delete(session, bis_queries.Delete_Query_2);
		
		logger.writeToLog("Running Delete Query 3");
		delete(session, bis_queries.Delete_Query_3);
		
		logger.writeToLog("Running Delete Query 4");
		delete(session, bis_queries.Delete_Query_4);
	}
	
	
	public void delete(IEnterpriseSession enterpriseSession, String query) throws SDKException {
		IInfoStore infoStore = (IInfoStore) enterpriseSession.getService("InfoStore");
		
		ArrayList<String> deletedTitles = new ArrayList<String>();
				
		logger.writeToLog("Executing Query");
		IInfoObjects infoObjects = infoStore.query(query);
		int deleteCount = 0;
		logger.writeToLog("Removing objects");
		for(int i=0; i<infoObjects.size(); i++) {
			IInfoObject infoObject = (IInfoObject)infoObjects.get(i);
			deletedTitles.add(infoObject.getTitle() + " - " + infoObject.getID());
			logger.writeToLog("Removing SI_CUID: " + infoObject.getCUID() 
			+ ", SI_NAME: " + infoObject.getTitle() + ", SI_KIND: " + infoObject.getKind());
			
			infoObject.deleteNow();
			deleteCount++;
		}
		if(infoObjects.size() == 0) {
			logger.writeToLog("0 Objects removed");
		}else{
			logger.writeToLog("Number of objects deleted from CMS : "+deleteCount);
		}
		
		logger.writeToLog("Removing items from the recycle bin ");
		String queryString = "SELECT TOP 100000 * FROM CI_INFOOBJECTS WHERE SI_KIND = '" +IRecycleBinObject.KIND+"'";

		infoObjects = infoStore.query(queryString);
		for(int i=0; i<infoObjects.size(); i++) {
			IRecycleBinObject infoObject = (IRecycleBinObject) infoObjects.get(i);
			logger.writeToLog("Removing from RecyleBin SI_CUID: " + infoObject.getCUID() 
			+ ", SI_NAME: " + infoObject.getTitle() + ", SI_KIND: " + infoObject.getKind());
				
			infoObject.deleteNow();
			
		}
		/*if(infoObjects.size() == 0) {
			logger.writeToLog("0 Objects removed from Recycle Bin");
		}*/
		
		
		logger.writeToLog("Removal complete");
		
	}
	
	
	public IEnterpriseSession openConnection(String option, Properties props) throws SDKException {
		IEnterpriseSession enterpriseSession = null;
		
		if(option.equalsIgnoreCase("SOURCE")) {
			try {
				String user = new String(Base64.getDecoder().decode(props.getProperty("src_username")),"utf-8");
				String pass = new String(Base64.getDecoder().decode(props.getProperty("src_password")),"utf-8");
				String bisname = new String(Base64.getDecoder().decode(props.getProperty("src_bis")),"utf-8");
				String security = "secEnterprise";
				try {
					enterpriseSession = CrystalEnterprise.getSessionMgr().logon(user, pass,
							bisname, security);
				} catch (Exception e) {
					logger.writeToLog("WARNING : Unable to establish connection ..." + e);
					System.exit(0);
				}
				
				
			} catch (Exception e) {
				logger.writeToLog("WARNING : Problem Occured while decrypting the Properties File" +e);
				System.exit(0);
			}
			
			
		}
		else if(option.equalsIgnoreCase("TARGET")) {
			
			
			try {
				String user = new String(Base64.getDecoder().decode(props.getProperty("tgt_username")),"utf-8");
				String pass = new String(Base64.getDecoder().decode(props.getProperty("tgt_password")),"utf-8");
				String bisname = new String(Base64.getDecoder().decode(props.getProperty("tgt_bis")),"utf-8");
				String security = "secEnterprise";
				
				try {
					enterpriseSession = CrystalEnterprise.getSessionMgr().logon(user, pass,
							bisname, security);
				} catch (Exception e) {
					logger.writeToLog("WARNING : Unable to establish connection ..." + e);
					System.exit(0);
				}
				
				
			} catch (Exception e) {
				logger.writeToLog("WARNING : Problem Occured while decrypting the Properties File"+e);
				System.exit(0);
			}
			
			
		}
		
		return enterpriseSession;
	}
	
	


}
