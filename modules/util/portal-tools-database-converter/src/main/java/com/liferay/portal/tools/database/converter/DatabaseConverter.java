/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.tools.database.converter;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.dao.db.MySQLDB;
import com.liferay.portal.dao.db.PostgreSQLDB;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBInspector;
import com.liferay.portal.kernel.util.StringUtil;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Alberto Chaparro
 */
public class DatabaseConverter {

	public static void main(String[] args) throws Exception {
		System.out.println("Start");

		// source connection

		Class.forName("com.mysql.cj.jdbc.Driver");

		Connection sourceConnection = DriverManager.getConnection(
			"jdbc:mysql://localhost/master", "root", "Toor123#");

		// target connection

		Class.forName("org.postgresql.Driver");

		Connection targetConnection = DriverManager.getConnection(
			"jdbc:postgresql://localhost:5432/migration", "postgres",
			"postgres");

		// Build type mapping array

		for (int i = 0; i < LIFERAY_TYPES.length; i++) {
			_typesMap.put(_MYSQL_JAVA_TYPES[i], LIFERAY_TYPES[i]);
		}

		// Loop over tables, and build the creation table statement

		DBInspector sourceDBInspector = new DBInspector(sourceConnection);
		DBInspector targetDBInspector = new DBInspector(targetConnection);

		DB mySQLDB = new MySQLDB(0, 0);
		PostgreSQLDB postgreSQLDB = new PostgreSQLDB(0, 0);

		for (String tableName : sourceDBInspector.getTableNames(null)) {
			String createTableSQL = postgreSQLDB.buildSQL(
				_getCreateTableSQL(
					sourceConnection, sourceDBInspector, targetDBInspector,
					mySQLDB, tableName));

			System.out.println(createTableSQL);

			Statement statement = targetConnection.createStatement();

			statement.execute(createTableSQL);
		}
	}

	protected static final String[] LIFERAY_TYPES = {
		" SBLOB", " BIGDECIMAL", " BOOLEAN", " DATE", " DOUBLE", " INTEGER",
		" LONG", " TEXT", " VARCHAR"
	};

	private static String _getCreateTableSQL(
			Connection connection, DBInspector sourceDBInspector,
			DBInspector targetDBInspector, DB db, String tableName)
		throws Exception {

		// Use methods from kernel when possible

		ResultSet columnsResultSet = sourceDBInspector.getColumnsResultSet(
			tableName);

		StringBundler sb = new StringBundler();

		sb.append("create table ");
		sb.append(targetDBInspector.normalizeName(tableName));
		sb.append(" (");

		while (columnsResultSet.next()) {
			String columnName = columnsResultSet.getString("COLUMN_NAME");

			sb.append(targetDBInspector.normalizeName(columnName));
			sb.append(_getType(columnsResultSet, tableName, columnName));

			String defaultValue = columnsResultSet.getString("COLUMN_DEF");

			if (defaultValue != null) {
				sb.append(" default ");
				sb.append(defaultValue);
			}

			if (columnsResultSet.getInt("NULLABLE") ==
					DatabaseMetaData.columnNoNulls) {

				sb.append(" not null");
			}

			sb.append(",");
		}

		String[] primaryKeyColumnNames = db.getPrimaryKeyColumnNames(
			connection, tableName);

		sb.append("primary key (");

		for (String primaryKeyColumnName : primaryKeyColumnNames) {
			sb.append(primaryKeyColumnName);
			sb.append(", ");
		}

		sb.setIndex(sb.index() - 1);

		sb.append("))");

		return sb.toString();
	}

	// we should get this from Liferay classes
	// removed blob and string

	private static String _getType(
			ResultSet resultSet, String tableName, String columnName)
		throws Exception {

		int columnType = resultSet.getInt("DATA_TYPE");

		String tableColumnName = tableName + "." + columnName;

		if ((columnType == Types.LONGVARBINARY) &&
			_blobTableColumnNames.contains(
				StringUtil.toLowerCase(tableColumnName))) {

			return " BLOB";
		}

		String liferayColumnType = _typesMap.get(columnType);

		if (liferayColumnType == null) {
			throw new Exception("Invalid type for " + tableColumnName);
		}

		if (columnType == Types.VARCHAR) {
			liferayColumnType += "(" + resultSet.getInt("COLUMN_SIZE") + ")";
		}

		return liferayColumnType;
	}

	private static final int[] _MYSQL_JAVA_TYPES = {
		Types.LONGVARBINARY, Types.DECIMAL, Types.TINYINT, Types.TIMESTAMP,
		Types.DOUBLE, Types.INTEGER, Types.BIGINT, Types.LONGVARCHAR,
		Types.VARCHAR
	};

	// This should be read from a file

	private static final List<String> _blobTableColumnNames = Arrays.asList(
		"analyticsmessage.body", "batchengineexporttask.content",
		"batchengineimporttask.content", "ctscontent.data_", "dlcontent.data_");

	// we should get this from Liferay classes

	private static final Map<Integer, String> _typesMap = new HashMap<>();

}