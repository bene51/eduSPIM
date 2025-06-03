@ECHO OFF

echo Parameter 1: %1

if "%1" EQU "--startup" (
	timeout 60 /nobreak
)

set CNT=-1
set args=""

:start
set /A CNT=%CNT% + 1
echo iteration: %CNT%

echo %date% %time% Starting eduSPIM (args = %args% iteration = %CNT%) >> eduSPIM_start.log

java -Xmx4g -cp target/EduSPIM-1.1.0-jar-with-dependencies.jar main.Microscope %args%

echo Error level:
echo %ERRORLEVEL%

if %ERRORLEVEL% EQU -5 (
	echo Fatal error.
	set args="--fatal"
	GOTO start
)
if %ERRORLEVEL% NEQ 0 (
	echo Error, starting again.
	set args=""
	GOTO start
)

pause

ECHO on
