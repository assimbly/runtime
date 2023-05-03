@echo off

IF "%~1"=="" GOTO :BUILDALL
mvn -f ..\..\%~1\pom.xml clean install -DskipTests
:BUILDALL
mvn -f ..\..\pom.xml clean install -DskipTests