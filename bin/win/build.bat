@echo off
SETLOCAL EnableDelayedExpansion

:: --- PERFORMANCE CONFIGURATION ---
:: -T 1C uses parallel threads (1 per CPU Core)
:: Offloading heavy/redundant plugins that usually tie up local 'install' commands
SET "PERF_FLAGS=-T 1C -DskipTests -Dmaven.javadoc.skip=true -Dsource.skip=true"

IF "%~1" == "" GOTO :BUILDALL

:BUILDSPECIFIC
echo =======================================================
echo Targeted Mode: Safe incremental build for [%~1]
echo =======================================================
:: Clean ONLY the module you are targeting, plus any internal upstream dependencies (-am)
:: This prevents stale bytecode in the target module while saving the other modules from rebuilding.
mvnd -f ..\..\pom.xml clean install !PERF_FLAGS! -pl :%~1 -am
GOTO :END

:BUILDALL
echo =======================================================
echo Global Mode: Fast parallel clean install across reactor
echo =======================================================
:: If you build everything, use a full clean to ensure no ghost classes remain,
:: but maximize thread scheduling optimization (--builder smart)
mvnd -f ..\..\pom.xml clean install !PERF_FLAGS! --builder smart
GOTO :END

:END
ENDLOCAL
