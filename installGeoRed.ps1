# *************************************************************************
# 	Ericsson Radio Systems AB                                     SCRIPT
# *************************************************************************
# 
#   (c) Ericsson Radio Systems AB 2019 - All rights reserved.
#   The copyright to the computer program(s) herein is the property and/or 
# 	copied only with the written permission from Ericsson Radio Systems AB 
# 	or in accordance with the terms and conditions stipulated in the 
# 	agreement/contract under which the program(s) have been supplied.
#
# *************************************************************************
# 	Name    : installGeoRed.ps1
# 	Date    : 16/04/2019
# 	Revision: 0.1
# 	Purpose : Script to install Geo-Red in the server
#
# 	Usage   : installGeoRed.ps1 -[primary|secondary]
# *************************************************************************

param(
	[switch]$primary = $false,
	[switch]$secondary = $false
)

# Check arguments
if(($primary -eq $false) -and ($secondary -eq $false)){
	Write-Host "Usage: installGeoRed.ps1 -[primary|secondary]"
	Write-Host "Options:"
	Write-Host "-primary : configures server as primary"
	Write-Host "-secondary : configures server as secondary"
	exit
}

# Set base properties
$BaseDir = "C:\eniq_procus_geored\"
$PropsFile = "$BaseDir" + "geo_red.properties"
$FlagCreateCommand = "$BaseDir" + "GeoRed.ps1 -createflag"
$BOSyncScript = "$BaseDir" + "GeoRed.ps1 -sync"
$DefaultSyncTime = "5am"
$DaysOfWeek = @("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

# Check if correct directory
if ($BaseDir -eq ("$PSScriptRoot" + "\")){
	Write-Host "Installation of Georedundancy feature started"
}
else{
	Write-Host "The package should be extracted in $BaseDir and the install script should only be executed from there (as Administrator)"
	exit
}

# Get installed directory and set lcmcli path
$InstallFile = "C:\ebid\install\" + (Get-ChildItem -Path "C:\ebid\install\" -Filter "*_server.ini")
$EbidInstall = (Get-Content $InstallFile)
foreach($line in $EbidInstall){
	
	if ($line.StartsWith("installdir")){
		$line = $line.split('=')
		$InstallDir = $line[1]
	}
}
$LcmCliPath = '"' + $InstallDir + "SAP BusinessObjects Enterprise XI 4.0\win64_x64\scripts\lcm_cli.bat" + '"'

while($true){
	# Get primary server details
	$SourceCMS = Read-Host -Prompt 'Enter current server BO CMS IP Address'
	$SourceUsername = Read-Host -Prompt 'Enter current server BO CMS username'
	$SourcePassword = Read-Host -Prompt 'Enter current server BO CMS password'

	# Get remote server details
	$RemoteCMS = Read-Host -Prompt 'Enter remote server BO CMS IP Address'
	$RemoteUsername = Read-Host -Prompt 'Enter remote server BO CMS username'
	$RemotePassword = Read-Host -Prompt 'Enter remote server BO CMS password'
	
	# Get Remote server OS Password
	$Target_windows_password = Read-Host -Prompt 'Enter Target server windows password'
	
	while($true){
		$Retention = Read-Host -Prompt 'Enter number of days to retain logs (Press enter for default - 10)'
		if ($Retention -eq ''){
			$Retention = 10
			break
		}
		else{
			if(($Retention -match "^[\d\.]+$") -and ($Retention -gt 0)){
				break
			}
			else{
				Write-Host "`n`Incorrect choice!"
			}
		}
	}
	
#**************** USED TO DEFINE A DAY TO PERFORM FULL SYNC ****************
#	Write-Host "`n`Choose day for performing full sync:"
#	for($opt=1; $opt -le $DaysOfWeek.Count ; $opt++){
#		$Option = $DaysOfWeek[$opt - 1]
#		Write-Host "[$opt] $Option"
#	}
#	while($true){
#		$Entry = Read-Host -Prompt "`n`Enter serial number"
#		$Entry = [int]($Entry.trim())
#		if(($Entry -match "^[\d\.]+$") -and ($Entry -gt 0) -and ($Entry -le $DaysOfWeek.Count)){
#			$SyncDay = $DaysOfWeek[$Entry - 1]
#			break
#		}
#		else{
#			Write-Host "`n`Incorrect choice!"
#		}
#	}

		
	# Add sync script to TaskSchduler
	while($true){
		$SyncTime = Read-Host -Prompt "Specify time when Sync should happen daily in format <Time value><am|pm>(Press enter for default $DefaultSyncTime)"
		if ($SyncTime -eq ''){
			$SyncTime = $DefaultSyncTime
			break
		}
		else{
			$TimeVal = $SyncTime.Substring(0,$SyncTime.length - 2)
			$TimeVal = [int]($TimeVal.trim())
			$AMPM = $SyncTime.Substring($SyncTime.length - 2,2)
			if((($TimeVal -match "^[\d\.]+$") -and ($TimeVal -gt 0) -and ($TimeVal -le 13)) -and (($AMPM -eq "am") -or ($AMPM -eq "pm"))){
				break
			}
			else{
				Write-Host "`n`Incorrect choice!"
			}
		}
	}

	# Confirm all properties
	Write-Host "`n`n"
	Write-Host "Properties -"
	Write-Host "IP Address of current server: $SourceCMS"
	Write-Host "BO CMS Username of current server: $SourceUsername"
	Write-Host "BO CMS Password of current server: $SourcePassword"
	Write-Host "IP Address of remote server: $RemoteCMS"
	Write-Host "BO CMS Username of remote server: $RemoteUsername"
	Write-Host "BO CMS Password of remote server: $RemotePassword"
	Write-Host "Target Server Windows Password: $Target_windows_password"
	Write-Host "Retention period of logs: $Retention"
#	Write-Host "Sync will run daily at $SyncTime with full sync every $SyncDay"
	$corr = Read-Host -Prompt "`n`Please confirm the above details are correct (Y)"
	if ($corr -eq "y"){
		break
	}
}

# Write properties file
$content = "src_bis=$SourceCMS","src_username=$SourceUsername","src_password=$SourcePassword","tgt_bis=$RemoteCMS","tgt_username=$RemoteUsername","tgt_password=$RemotePassword","HousekeepingLimit=$Retention","tgt_win_password=$Target_windows_password"
foreach($line in $content){
	$arr = $line.split('=')
	$key = $arr[0]
	$value = $arr[1]
	$bytes  = [System.Text.Encoding]::UTF8.GetBytes($value)
	$Encoded = [System.Convert]::ToBase64String($bytes)
	"$key=$Encoded" | Out-File -FilePath $PropsFile -Encoding ASCII -Append
	
}

#"src_bis=$SourceCMS" | Out-File -FilePath $PropsFile -Encoding ASCII
#"src_username=$SourceUsername" | Out-File -FilePath $PropsFile -Encoding ASCII -Append
#"src_password=$SourcePassword" | Out-File -FilePath $PropsFile -Encoding ASCII -Append
#"tgt_bis=$RemoteCMS" | Out-File -FilePath $PropsFile -Encoding ASCII -Append
#"tgt_username=$RemoteUsername" | Out-File -FilePath $PropsFile -Encoding ASCII -Append
#"tgt_password=$RemotePassword" | Out-File -FilePath $PropsFile -Encoding ASCII -Append
#"HousekeepingLimit=$Retention" | Out-File -FilePath $PropsFile -Encoding ASCII -Append
#"fullsyncday=$SyncDay" | Out-File -FilePath $PropsFile -Encoding ASCII -Append

# Set permissions for properties file
$acl = Get-Acl $PropsFile
if($acl.AreAccessRulesProtected){
	$acl.Access | % {$acl.purgeaccessrules($_.IdentityReference)}
}
else{
	$isProtected = $true
	$preserveInheritance = $false
	$acl.SetAccessRuleProtection($isProtected, $preserveInheritance)
}
$rights=[System.Security.AccessControl.FileSystemRights]::FullControl
$inheritance=[System.Security.AccessControl.InheritanceFlags]::None
$propagation=[System.Security.AccessControl.PropagationFlags]::None
$allowdeny=[System.Security.AccessControl.AccessControlType]::Allow
$dirACE=New-Object System.Security.AccessControl.FileSystemAccessRule ("Administrators",$rights,$inheritance,$propagation,$allowdeny)
$acl.AddAccessRule($dirACE)
Set-Acl -aclobject $acl -path $PropsFile
																
# Create flag if primary
if ($primary -eq $true){
	Invoke-Expression -Command $FlagCreateCommand
}


$TaskName = 'BOSync'
$tasks = Get-ScheduledTask -TaskName "BOSync" -ErrorAction SilentlyContinue
$Name = $tasks.TaskName
if($Name -eq $TaskName){
	Write-Host 'Removing Existing scheduled task'
	Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false
}

Write-Host 'Scheduling task to sync BIS systems.'
$trigger =  New-ScheduledTaskTrigger -Daily -At $SyncTime
$action = New-ScheduledTaskAction -Execute 'powershell' -WorkingDirectory "$BaseDir" -Argument "-NoProfile -WindowStyle Hidden -command $BOSyncScript"
$principal = New-ScheduledTaskPrincipal -UserID "Administrator" -LogonType ServiceAccount -RunLevel Highest
Register-ScheduledTask -Action $action -Trigger $trigger -TaskName $TaskName -Principal $principal -Description "Sync BIS servers for GeoRedundancy"

# Enable remote connection and sync direction
Enable-PSRemoting -Force
Set-Item WSMan:\localhost\Client\TrustedHosts -Value "$RemoteCMS" -Force

# End of Installation
Write-Host "GeoRed has been installed in the server. Please execute the same in remote server $RemoteCMS, if not done yet"
