@echo off
setlocal
set "ROOT=%~dp0"

echo Building Light_Task_Server...
call "%ROOT%Light_Task_Server\mvnw.cmd" -f "%ROOT%Light_Task_Server\pom.xml" package -DskipTests
if errorlevel 1 exit /b 1

echo Building Heavy_Task_Server...
call "%ROOT%Heavy_Task_Server\mvnw.cmd" -f "%ROOT%Heavy_Task_Server\pom.xml" package -DskipTests
if errorlevel 1 exit /b 1

echo Starting Light_Task_Server on port 8080...
start "Light_Task_Server" java -jar "%ROOT%Light_Task_Server\target\instance-1-0.0.1-SNAPSHOT.jar"
start "Light_Task_Server_2" java -jar "%ROOT%Light_Task_Server\target\instance-1-0.0.1-SNAPSHOT.jar" --server.port=8082 --instance.name=Light_Task_Server_2
echo Starting Heavy_Task_Server on port 8081...
start "Heavy_Task_Server" java -jar "%ROOT%Heavy_Task_Server\target\instance-2-0.0.1-SNAPSHOT.jar"
echo Both instances launched in separate windows.
endlocal
