# *************************************************************************
# 	Ericsson Radio Systems AB                                     SCRIPT
# *************************************************************************
# 
#   (c) Ericsson Radio Systems AB 2019 - All rights reserved.
#   The copyright to the computer program(s) herein is the property
# 	and/or copied only with the written permission from Ericsson Radio
# 	Systems AB or in accordance with the terms and conditions stipulated
# 	in the agreement/contract under which the program(s) have been
# 	supplied.
#
# *************************************************************************
# 	Name    : GeoRed.ps1
# 	Date    : 27/05/2019
# 	Revision: 0.1
# 	Purpose : Script to implement the failover in Geo-redundancy
#
# 	Usage   : GeoRed.ps1 -[failover|sync|flagstate|createflag|deleteflag|changepwd]
# *************************************************************************

# Check arguments
[CmdletBinding()]
param(
	[switch] $failover = $false,
	[switch] $sync = $false,
	[switch] $flagstate = $false,
	[switch] $createflag = $false,
	[switch] $deleteflag = $false,
	[switch] $changeprop = $false

)

# Define GeoRed parameters in base
$BaseDir = 'C:\eniq_procus_geored\'
$ConfigFile = "$BaseDir" + 'geo_red.properties'
$Flag = "$BaseDir" + 'flag_geoRed'
# Define properties in remote
$FlagStatusScript = "$BaseDir" + "GeoRed.ps1"
$FlagStatusCommand = [scriptblock]::Create("$FlagStatusScript -flagstate")
$FlagDeleteCommand = [scriptblock]::Create("$FlagStatusScript -deleteflag")
$FlagCreateCommand = [scriptblock]::Create("$FlagStatusScript -createflag")


# Read properties from file
$Props = (Get-Content $ConfigFile)
foreach($line in $Props){
	
	If ($line.StartsWith("tgt_bis")){
		$key,$val = "$line" -split '=',2
		$RemoteServer = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($val))
	}
	ElseIf ($line.StartsWith("src_bis")){
		$key,$val = "$line" -split '=',2
		$LocalServer = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($val))
	}
	ElseIf ($line.StartsWith("tgt_win_password")){
		$key,$val = "$line" -split '=',2
		$tgt_win_password = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($val))
	}	
}


#Password change Feature
Function change($str){
	$pair = $str.split(' ')
	
	$Props = (Get-Content $ConfigFile)
	foreach($line in $Props){
	
		If ($line.StartsWith($pair[1])){
			(Get-Content $ConfigFile) -replace $line, $pair[0] | Set-Content $ConfigFile
			break
		}
	}
}
	


Function change_pwd{

Write-Host "============= Pick The Options To Update Properties =============="
Write-Host "`ta. 'P' to update the $LocalServer CMS Password"
Write-Host "`tb. 'S' to update the $RemoteServer CMS Password"
Write-Host "`tc. 'T' to update $RemoteServer Server Windows Password"
Write-Host "`td. 'H' to update the number of HouseKeeping retention days "
Write-Host "`te. 'Q' to Quit"
Write-Host "========================================================"
$choice = Read-Host "`nEnter Choice"

switch ($choice) {
   'P'{
		$SourcePassword = Read-Host -Prompt 'Enter Corresponding server BO CMS password'
		if (([string]::IsNullOrEmpty($SourcePassword)))
		{
			Write-Host "Invalid input"
			Return
		}
		Else{
			$Sourcebytes  = [System.Text.Encoding]::UTF8.GetBytes($SourcePassword)
			$SourceEncoded = [System.Convert]::ToBase64String($Sourcebytes)
			change("src_password=$SourceEncoded","src_password")
			Write-Host "Password Successfully changed"
		}
		
   }
   'S'{
		$SecondaryPassword = Read-Host -Prompt 'Enter Corresponding server BO CMS password'
		if (([string]::IsNullOrEmpty($SecondaryPassword)))
		{
			Write-Host "Invalid input"
			Return
		}
		Else{
			$Secondarybytes  = [System.Text.Encoding]::UTF8.GetBytes($SecondaryPassword)
			$SecondaryEncoded = [System.Convert]::ToBase64String($Secondarybytes)
			change("tgt_password=$SecondaryEncoded","tgt_password")
			Write-Host "Password Successfully changed"
		}
        
		
   }
   'T'{
		$TargetPassword = Read-Host -Prompt 'Enter Corresponding server  Windows password'
		if (([string]::IsNullOrEmpty($TargetPassword)))
		{
			Write-Host "Invalid input"
			Return
		}
		Else{
			$Targetbytes  = [System.Text.Encoding]::UTF8.GetBytes($TargetPassword)
			$TargetEncoded = [System.Convert]::ToBase64String($Targetbytes)
			change("tgt_win_password=$TargetEncoded","tgt_win_password")
			Write-Host "Password Successfully changed"		
		}
		
		
   }
   'H'{ 
		
		$retention = Read-Host -Prompt 'Enter new HouseKeeping retention days '
		if (([string]::IsNullOrEmpty($retention)))
		{
			Write-Host "Invalid input"
			Return
		}
		Else{
			$retentionbytes  = [System.Text.Encoding]::UTF8.GetBytes($retention)
			$retentionEncoded = [System.Convert]::ToBase64String($retentionbytes)
			change("HousekeepingLimit=$retentionEncoded","HousekeepingLimit")
			Write-Host "HouseKeepingLimit retention period changed successfully"
		}
		
				
   }
   'Q'{
		Return
   }
   default{
		Write-Host "Invalid Option Choosen"
		Return
   }
}
}


$pwd = convertto-securestring $tgt_win_password -asplaintext -force
$credentialObj = new-object -typename System.Management.Automation.PSCredential -argumentlist "Administrator",$pwd

#Validation function if both the server are up and running
Function server_status{
	$servers = $LocalServer,$RemoteServer

	foreach($server in $servers){
	
		If(Test-Connection -ComputerName $server -count 1 -Quiet ){
	
			Write-Host "$server is accessible."
		} Else {
	
			Write-Host "$server is not accessible. Terminating the execution of the script"
			exit
		}

	}
}


# Check if flag exists
Function checklocalFlag{
	If(Test-Path $Flag){
		Return $true
	}
	Else{
		Return $false
	}
}

Function createflag{
	$flagExists = checklocalFlag
	If($flagExists -eq $true){
		Write-Host 'Flag already exists. No further action required'
	}
	Else{
		New-Item -ItemType file -Path $Flag | Out-Null
		$flagExists = checklocalFlag
		If($flagExists -eq $true){
			Write-Host 'Successfully created flag'
		}
		Else{
			Write-Error 'Unable to create flag'
		}
	}
}

Function determinePrimary{
	$remoteflag = Invoke-Command -ComputerName $RemoteServer -Credential $credentialObj -ScriptBlock $FlagStatusCommand
	$flagExists = checklocalFlag
	
	If($remoteflag -eq 'No flag found' -and $flagExists -eq $true){
		Return 'Local'
	}
	Elseif($remoteflag -eq 'No flag found' -and $flagExists -ne $true){
		Return 'None'
	}
	Elseif($remoteflag -ne 'No flag found' -and $flagExists -ne $true){
		Return 'Remote'
	}
	Elseif($remoteflag -ne 'No flag found' -and $flagExists -eq $true){
		$localCreateTime = (Get-ChildItem $Flag).CreationTime
		If($remoteflag -gt $localCreateTime){
			deleteflag
			Return 'Remote'
		}
		Else{
		
			Invoke-Command -ComputerName $RemoteServer -Credential $credentialObj -ScriptBlock $FlagDeleteCommand
			Return 'Local'
		}
	}
}

Function getStatus{
	$flagExists = checklocalFlag
	If($flagExists -eq $true){
		$FileDate = (Get-ChildItem $Flag).CreationTime
		Return $FileDate
	}
	Else{
		Return 'No flag found'
	}
	
}

Function deleteflag{
	$flagExists = checklocalFlag
	If($flagExists -eq $true){
		Remove-Item -Path $Flag
		$flagExists = checklocalFlag
		If($flagExists -eq $true){
			Write-Error 'Unable to remove flag'
		}
		Else{
			Write-Host 'Successfully removed flag'
		}
	}
	Else{
		Write-Host 'Flag does not exist. No further action required'
	}
}

Function failover_deployment{
	server_status
	$Status = determinePrimary
	If($Status -eq 'None'){
		Write-Error 'No primary server set. Please configure a server as Primary by excuting this script on the desired Primary server using the -createflag option'
	}
	ElseIf($Status -eq 'Local'){
		Write-Host "$LocalServer is the current Primary"
		
		Write-Host "Removing local Primary flag"
		deleteflag
		
		Write-Host "Creating Primary flag on $RemoteServer"
		
		Invoke-Command -ComputerName $RemoteServer -Credential $credentialObj -ScriptBlock $FlagCreateCommand
		
		Write-Host "$RemoteServer is now set as Primary"
	}
	ElseIf($Status -eq 'Remote'){
		Write-Host "$RemoteServer is the current Primary"
		
		Write-Host "Removing Primary flag on $RemoteServer"
		Invoke-Command -ComputerName $RemoteServer -Credential $credentialObj -ScriptBlock $FlagDeleteCommand
		
		Write-Host "Creating Primary flag locally"
		createflag
		
		Write-Host "$LocalServer is now set as Primary"
	}
}

Function sync_deployments{
	server_status
	$isPrimary = determinePrimary
	Write-Host $isPrimary
	If($isPrimary -eq 'Local'){
		Write-Host "Syncing"
		Start-Process -FilePath C:\eniq_procus_geored\RunSync.bat -argumentList biar -WindowStyle hidden
	}
}


# Validate arguments and trigger execution
If($failover -ne $false){
	failover_deployment
}
Elseif($sync -ne $false){
	sync_deployments
}
Elseif($flagstate -ne $false){
	getStatus
}
Elseif($createflag -ne $false){
	createflag
}
Elseif($deleteflag -ne $false){
	deleteflag
}
Elseif($changeprop -ne $false){
	change_pwd
}
Else{
	Write-Host "Usage: GeoRed.ps1 -[failover|sync|flagstate|createflag|deleteflag|changeprop]"
	Write-Host "Options:"
	Write-Host "	-failover 	: Fails over the deployment"
	Write-Host "	-sync 		: Triggers a sync between the configured BIS deployments"
	Write-Host "	-flagstate 	: Used by the solution. Returns the state from the alternate deployment"
	Write-Host "	-createflag 	: Used by the solution. Creates the primary flag"
	Write-Host "	-deleteflag 	: Used by the solution. Deletes the primary flag"
	Write-Host "	-changeprop 	: Used to update the Properties File"
	Exit
}

