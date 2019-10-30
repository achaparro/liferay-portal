@echo off

pushd "%~dp0"

path %PATH%;%JAVA_HOME%\bin

java -jar com.liferay.portal.tools.db.partitioning.client.jar %*

popd

@echo on