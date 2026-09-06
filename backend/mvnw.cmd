@echo off
setlocal
set "DIR=%~dp0"
if "%JAVA_HOME%"=="" (
  set "JAVACMD=java"
) else (
  set "JAVACMD=%JAVA_HOME%\bin\java.exe"
)
"%JAVACMD%" "-Dmaven.multiModuleProjectDirectory=%DIR%." -classpath "%DIR%.mvn\wrapper\maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain %*
if %ERRORLEVEL% neq 0 exit /b %ERRORLEVEL%
