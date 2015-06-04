@ECHO OFF

set CNT=-1
set args=""

:start
set /A CNT=%CNT% + 1
echo iteration: %CNT%

java -cp target/EduSPIM-0.0.1-SNAPSHOT-jar-with-dependencies.jar main.Microscope %args%

echo Error level:
echo %ERRORLEVEL%

if %ERRORLEVEL% EQU -5 (
	echo Fatal error.
	set args="--fatal"
	GOTO start
)
if %ERRORLEVEL% LSS 0 (
	echo Error, starting again.
	set args=""
	GOTO start
)

pause

ECHO on
