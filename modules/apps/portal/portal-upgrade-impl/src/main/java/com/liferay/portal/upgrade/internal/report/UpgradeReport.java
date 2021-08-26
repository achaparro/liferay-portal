/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.upgrade.internal.report;

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.db.DBType;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.ReleaseConstants;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.ReleaseInfo;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.version.Version;
import com.liferay.portal.upgrade.PortalUpgradeProcess;
import com.liferay.portal.util.PropsValues;

import java.io.File;
import java.io.IOException;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.cm.PersistenceManager;

/**
 * @author Sam Ziemer
 */
public class UpgradeReport {

	public UpgradeReport(PersistenceManager persistenceManager) {
		_persistenceManager = persistenceManager;

		_initialBuildNumber = _getBuildNumber();
		_initialSchemaVersion = _getSchemaVersion();
	}

	public void addErrorMessage(String loggerName, String message) {
		List<String> errorMessages = _errorMessages.computeIfAbsent(
			loggerName, key -> new ArrayList<>());

		errorMessages.add(message);
	}

	public void addEventMessage(String loggerName, String message) {
		List<String> eventMessages = _eventMessages.computeIfAbsent(
			loggerName, key -> new ArrayList<>());

		eventMessages.add(message);
	}

	public void addWarningMessage(String loggerName, String message) {
		List<String> warningMessages = _warningMessages.computeIfAbsent(
			loggerName, key -> new ArrayList<>());

		warningMessages.add(message);
	}

	public void generateReport() {
		try {
			FileUtil.write(
				_getReportFile(),
				StringBundler.concat(
					_getPortalVersions(), _getDialectInfo(), _getProperties(),
					_getDatabaseInfo()));
		}
		catch (IOException ioException) {
			_log.error("Unable to generate the upgrade report");
		}
	}

	private int _getBuildNumber() {
		try (Connection connection = DataAccess.getConnection();
			PreparedStatement preparedStatement = connection.prepareStatement(
				"select buildNumber from Release_ where releaseId = " +
					ReleaseConstants.DEFAULT_ID)) {

			ResultSet resultSet = preparedStatement.executeQuery();

			if (resultSet.next()) {
				return resultSet.getInt("buildNumber");
			}
		}
		catch (Exception exception) {
			if (_log.isWarnEnabled()) {
				_log.warn("Unable to get build number");
			}
		}

		return 0;
	}

	private String _getDatabaseInfo() {
		try (Connection connection = DataAccess.getConnection()) {
			DatabaseMetaData metadata = connection.getMetaData();

			String catalog = connection.getCatalog();
			String schema = null;

			if ((catalog == null) &&
				Objects.equals(_getDBType(), DBType.ORACLE)) {

				catalog = metadata.getUserName();

				schema = catalog;
			}

			List<TableInfo> tableInfos = _getTableInfos(
				connection, metadata, catalog, schema);

			StringBundler sb = new StringBundler((2 * tableInfos.size()) + 5);

			sb.append("Tables in database \"");
			sb.append(catalog);
			sb.append("\" sorted by number of rows:\n\n");
			sb.append(String.format("%-30s %10s\n", "Table name", "Rows"));
			sb.append(
				String.format(
					"%-30s %10s\n", "--------------", "--------------"));

			Collections.sort(tableInfos);

			for (TableInfo tableInfo : tableInfos) {
				sb.append(tableInfo);
				sb.append("\n");
			}

			return sb.toString();
		}
		catch (SQLException sqlException) {
			if (_log.isWarnEnabled()) {
				_log.warn("Unable to get database information");
			}

			return null;
		}
	}

	private DBType _getDBType() {
		DB db = DBManagerUtil.getDB();

		return db.getDBType();
	}

	private String _getDialectInfo() {
		DB db = DBManagerUtil.getDB();

		return StringBundler.concat(
			"Using ", db.getDBType(), " version ", db.getMajorVersion(),
			StringPool.PERIOD, db.getMinorVersion(), StringPool.NEW_LINE);
	}

	private String _getPortalVersions() {
		Version latestSchemaVersion =
			PortalUpgradeProcess.getLatestSchemaVersion();

		return StringBundler.concat(
			_getReleaseInfo(
				_initialBuildNumber, _initialSchemaVersion, "initial"),
			StringPool.NEW_LINE,
			_getReleaseInfo(_getBuildNumber(), _getSchemaVersion(), "final"),
			StringPool.NEW_LINE,
			_getReleaseInfo(
				ReleaseInfo.getBuildNumber(), latestSchemaVersion.toString(),
				"expected"),
			StringPool.NEW_LINE);
	}

	private String _getProperties() {
		StringBuffer sb = new StringBuffer(12);

		sb.append("liferay.home=" + PropsValues.LIFERAY_HOME);
		sb.append("\nlocales=" + Arrays.toString(PropsValues.LOCALES));
		sb.append(
			"\nlocales.enabled=" +
				Arrays.toString(PropsValues.LOCALES_ENABLED));
		sb.append(StringPool.NEW_LINE);

		String dlStore = PropsValues.DL_STORE_IMPL;

		sb.append(PropsKeys.DL_STORE_IMPL + StringPool.EQUAL + dlStore);

		sb.append(StringPool.NEW_LINE);

		String rootDir = null;

		if (dlStore.equals(
				"com.liferay.portal.store.file.system." +
					"AdvancedFileSystemStore")) {

			rootDir = _getRootDir(
				_CONFIGURATION_PID_ADVANCED_FILE_SYSTEM_STORE);

			if (rootDir == null) {
				sb.append("The configuration \"rootDir\" is required. ");
				sb.append("Configure it in ");
				sb.append(_CONFIGURATION_PID_ADVANCED_FILE_SYSTEM_STORE);
				sb.append(".config");
			}
		}
		else if (dlStore.equals(
					"com.liferay.portal.store.file.system.FileSystemStore")) {

			rootDir = _getRootDir(_CONFIGURATION_PID_FILE_SYSTEM_STORE);

			if (rootDir == null) {
				sb.append("Using the default directory because the ");
				sb.append("configuration \"rootDir\" was not set");
			}
		}

		if (rootDir != null) {
			sb.append("rootDir=" + rootDir);
		}

		sb.append(StringPool.NEW_LINE);

		return sb.toString();
	}

	private String _getReleaseInfo(
		int buildNumber, String schemaVersion, String type) {

		StringBuffer sb = new StringBuffer();

		if (buildNumber != 0) {
			sb.append(StringUtil.upperCaseFirstLetter(type));
			sb.append(" portal build number: ");
			sb.append(buildNumber);
		}
		else {
			sb.append("Unable to determine ");
			sb.append(type);
			sb.append(" portal build number");
		}

		sb.append(StringPool.NEW_LINE);

		if (schemaVersion != null) {
			sb.append(StringUtil.upperCaseFirstLetter(type));
			sb.append(" portal schema version: ");
			sb.append(schemaVersion);
		}
		else {
			sb.append("Unable to determine ");
			sb.append(type);
			sb.append(" portal schema version");
		}

		return sb.toString();
	}

	private File _getReportFile() {
		File reportsDir = new File(".", "reports");

		if ((reportsDir != null) && !reportsDir.exists()) {
			reportsDir.mkdirs();
		}

		File reportFile = new File(reportsDir, "upgrade_report.info");

		if (reportFile.exists()) {
			String reportFileName = reportFile.getName();

			reportFile.renameTo(
				new File(
					reportsDir,
					reportFileName + "." + reportFile.lastModified()));

			reportFile = new File(reportsDir, reportFileName);
		}

		return reportFile;
	}

	private String _getRootDir(String dlStoreConfigurationPid) {
		try {
			Dictionary<String, String> configurations =
				_persistenceManager.load(dlStoreConfigurationPid);

			if (configurations != null) {
				return configurations.get("rootDir");
			}
		}
		catch (IOException ioException) {
			if (_log.isWarnEnabled()) {
				_log.warn("Unable to get document library store root dir");
			}
		}

		return null;
	}

	private String _getSchemaVersion() {
		try (Connection connection = DataAccess.getConnection();
			PreparedStatement preparedStatement = connection.prepareStatement(
				"select schemaVersion from Release_ where releaseId = " +
					ReleaseConstants.DEFAULT_ID)) {

			ResultSet resultSet = preparedStatement.executeQuery();

			if (resultSet.next()) {
				return resultSet.getString("schemaVersion");
			}
		}
		catch (SQLException sqlException) {
			if (_log.isWarnEnabled()) {
				_log.warn("Unable to get schema version");
			}
		}

		return null;
	}

	private List<TableInfo> _getTableInfos(
			Connection connection, DatabaseMetaData metadata, String catalog,
			String schema)
		throws SQLException {

		List<TableInfo> tableInfos = new ArrayList<>();

		try (ResultSet resultSet1 = metadata.getTables(
				catalog, schema, "%", null)) {

			DBType dbType = _getDBType();

			while (resultSet1.next()) {
				String tableType = resultSet1.getString("TABLE_TYPE");

				if (Objects.equals(dbType, DBType.MARIADB)) {
					if (!tableType.equals("BASE TABLE")) {
						continue;
					}
				}
				else if (!tableType.equals("TABLE")) {
					continue;
				}

				String tableName = resultSet1.getString("TABLE_NAME");

				try (PreparedStatement preparedStatement =
						connection.prepareStatement(
							"select count(*) from " + tableName);
					ResultSet resultSet2 = preparedStatement.executeQuery()) {

					if (resultSet2.next()) {
						tableInfos.add(
							new TableInfo(tableName, resultSet2.getInt(1)));
					}
				}
				catch (SQLException sqlException) {
					if (_log.isWarnEnabled()) {
						_log.warn("Unable to retrieve data from " + tableName);
					}
				}
			}
		}

		return tableInfos;
	}

	private static final String _CONFIGURATION_PID_ADVANCED_FILE_SYSTEM_STORE =
		"com.liferay.portal.store.file.system.configuration." +
			"AdvancedFileSystemStoreConfiguration";

	private static final String _CONFIGURATION_PID_FILE_SYSTEM_STORE =
		"com.liferay.portal.store.file.system.configuration." +
			"FileSystemStoreConfiguration";

	private static final Log _log = LogFactoryUtil.getLog(UpgradeReport.class);

	private final Map<String, ArrayList<String>> _errorMessages =
		new ConcurrentHashMap<>();
	private final Map<String, ArrayList<String>> _eventMessages =
		new ConcurrentHashMap<>();
	private final int _initialBuildNumber;
	private final String _initialSchemaVersion;
	private final PersistenceManager _persistenceManager;
	private final Map<String, ArrayList<String>> _warningMessages =
		new ConcurrentHashMap<>();

	private static class TableInfo implements Comparable<TableInfo> {

		public TableInfo(String name, int rows) {
			_name = name;
			_rows = rows;
		}

		public int compareTo(TableInfo other) {
			if (_rows == other._rows) {
				return _name.compareTo(other._name);
			}

			return other._rows - _rows;
		}

		public String toString() {
			return String.format("%-30s %10d", _name, _rows);
		}

		private final String _name;
		private int _rows;

	}

}