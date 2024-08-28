
set OLDPATH=%PATH%
Reg QUERY "HKEY_LOCAL_MACHINE\Software\Business Objects\Suite 12.0\default" /v INSTALLDIR 1>nul 2>&1 || goto check64bit:
goto xi3:

:check64bit
Reg QUERY "HKEY_LOCAL_MACHINE\Software\Wow6432Node\Business Objects\Suite 12.0\default" /v INSTALLDIR 1>nul 2>&1 || goto checkbi4:
goto xi364:

:checkbi4
Reg QUERY "HKEY_LOCAL_MACHINE\Software\SAP BusinessObjects\Suite XI 4.0\Config Manager" /v INSTALLDIR 1>nul 2>&1 || echo Failed to find a BO installation && goto end:
goto bi4:

:bi4
FOR /F "tokens=2* delims=	 " %%A IN ('Reg QUERY "HKEY_LOCAL_MACHINE\Software\SAP BusinessObjects\Suite XI 4.0\Config Manager" /v INSTALLDIR') DO SET BOHOME=%%B
set JAVA_HOME=%BOHOME%\SAP BusinessObjects Enterprise XI 4.0\win64_x64\sapjvm
set BOCLASSDIR=%BOHOME%\SAP BusinessObjects Enterprise XI 4.0\java\lib
goto runJava:

:xi3
FOR /F "tokens=2* delims=	 " %%A IN ('Reg QUERY "HKEY_LOCAL_MACHINE\Software\Business Objects\Suite 12.0\default" /v INSTALLDIR') DO SET BOHOME=%%B\..
set JAVA_HOME=%BOHOME%\javasdk\
set BOCLASSDIR=%BOHOME%\Common\4.0\java\lib
goto runJava:

:xi364
FOR /F "tokens=2* delims=	 " %%A IN ('Reg QUERY "HKEY_LOCAL_MACHINE\Software\Wow6432Node\Business Objects\Suite 12.0\default" /v INSTALLDIR') DO SET BOHOME=%%B\..
set JAVA_HOME=%BOHOME%\javasdk\
set BOCLASSDIR=%BOHOME%\Common\4.0\java\lib
goto runJava:

:runJava
"%JAVA_HOME%\bin\java" -Djava.ext.dirs="%BOCLASSDIR%";. -jar C:\eniq_procus_geored\geored.jar com.ericcson.eniq.procus.bo.geored.BIS_Geo_Red %1

:end
