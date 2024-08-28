package com.ericcson.eniq.procus.bo.geored;

public class BIS_Queries {
	


	public final String Export_Query_1 = "SELECT TOP 100000 * "
			+ "FROM CI_INFOOBJECTS "
			+ "WHERE SI_KIND IN ('Folder','FavoritesFolder','PersonalCategory','Inbox','Webi','CrystalReport','Category','Hyperlink',"
			+ "'FullClient','Txt','Excel','Analysis','Pdf','Word','Rtf') "
			+ "AND SI_PARENT_CUID NOT IN ('AQbJ_w80vzFKoHvId1bZRIQ') "
			+ "AND SI_NAME NOT IN ('Sample Category')";
	
	public final String Export_Query_2 = "SELECT TOP 100000 * "
			+ "FROM CI_APPOBJECTS "
			+ "WHERE SI_KIND IN ('Folder','Universe','DSL.MetaDataFile') "
			+ "AND SI_OWNER NOT IN ('System Account') "
			+ "AND SI_DEFAULT_OBJECT IS null "
			+ "AND SI_NAME NOT IN ('Report Conversion Tool Audit Universe','Activity', 'eFashion', "
			+ "'eFashion.unx','Island Resorts Marketing', 'Samples', 'Rio2016.unx', 'Sample Category')";
	
	public final String Export_Query_3 = "SELECT TOP 100000 * "
			+ "FROM CI_SYSTEMOBJECTS "
			+ "WHERE SI_KIND NOT IN ('Folder','Connection','Server', 'ServerGroup','AuditEventInfo','LicenseKey','Install','Service',"
				+ "'ServiceCategory','ServiceContainer','EnterpriseNode','MetricDescriptions','DeploymentFile','AlertNotification',"
				+ "'DependencyRule','MON.Subscription','MON.MonitoringEvent','CryptographicKey') "
			+ "AND SI_KIND IS NOT NULL "
			+ "AND SI_NAME NOT IN ('AuditEventDefinitions2','Full Control') "
			+ "AND SI_ID NOT IN (12)";
	
	public final String Delete_Query_1 = "SELECT TOP 100000 * "
			+ "FROM CI_INFOOBJECTS "
			+ "WHERE SI_KIND = 'Folder' "
			+ "AND (SI_PATH.SI_FOLDER_NAME1 LIKE 'ENIQ%' OR SI_PATH.SI_FOLDER_NAME2 = 'User Folders')";
	
	public final String Delete_Query_2 = "SELECT TOP 100000 * "
			+ "FROM CI_INFOOBJECTS "
			+ "WHERE SI_KIND IN ('Webi','CrystalReport','Excel','Pdf','Word','Rtf', 'Category') "
			+ "AND SI_NAME NOT LIKE 'ZZ_%'";
	
	public final String Delete_Query_3 = "SELECT TOP 100000 * "
			+ "FROM CI_APPOBJECTS "
			+ "WHERE SI_KIND IN ('Universe','DSL.MetaDataFile') "
			+ "AND SI_OWNER NOT IN ('System Account') "
			+ "AND SI_NAME NOT IN ('Report Conversion Tool Audit Universe','Activity','eFashion','Island Resorts Marketing',"
				+ "'Monitoring TrendData Universe','eFashion.unx','Rio2016.unx')";
	
	public final String Delete_Query_4 = "SELECT TOP 100000 * "
			+ "FROM CI_SYSTEMOBJECTS WHERE SI_KIND IN ('User','UserGroup') "
			+ "AND SI_NAME NOT IN ('Guest','Administrator','SMAdmin','QaaWSServletPrincipal','Everyone','Administrators',"
				+ "'Cryptographic Officers','Data Federation Administrators','Monitoring Users','QaaWS Group Designer',"
				+ "'Report Conversion Tool Users','Translators','Universe Designer Users')";
	

}
