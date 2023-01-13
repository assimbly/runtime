@echo off

if [%1]==[] goto usage
echo:
@echo update version to %1
echo:
mvn -f ..\..\pom.xml versions:set -DgenerateBackupPoms=false -DnewVersion=%1
mvn -f ..\..\pom.xml versions:set-property -Dproperty=assimbly.version -DnewVersion=%1
:usage
echo:
@echo updateversion ^<versionnumber^>
echo:
@echo example:
@echo updateversion 1.0.0
exit /B 1