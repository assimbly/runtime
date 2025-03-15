@echo off

IF "%~1"=="" GOTO :BUILDALL
mvnd -f ..\..\%~1\pom.xml clean install -DskipTests
:BUILDALL
mvnd -f ..\..\pom.xml clean install -DskipTests
