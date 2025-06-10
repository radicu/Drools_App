@echo off
cd /d "%~dp0"

echo Deleting old EXE...
del RuleEngine.exe

echo Rebuilding with Launch4j...
"C:\Program Files (x86)\Launch4j\launch4j.exe" "%cd%\RuleEngine.xml" > build.log 2>&1

echo Done. Checking output:
dir RuleEngine.exe

echo --------------------------
echo Log Output:
type build.log
pause
