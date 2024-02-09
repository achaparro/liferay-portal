/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.dao.db;

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.MapUtil;
import com.liferay.portal.kernel.util.StringUtil;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Mariano Álvaro Sáiz
 */
public class IndexSQLUtil {

	public static final String INDEX_NAME_PREFIX = "IX_";

	public static String getCreateSQL(
		boolean unique, String tableName, String[] columnNames,
		int[] columnSizes, String indexPrefix) {

		int sbSize = 8 + (columnNames.length * 2);

		if (ArrayUtil.isNotEmpty(columnSizes)) {
			columnNames = trimColumnNames(columnNames);
		}

		if (columnSizes != null) {
			sbSize += columnNames.length * 3;
		}

		StringBundler sb = new StringBundler(sbSize);

		if (unique) {
			sb.append("create unique ");
		}
		else {
			sb.append("create ");
		}

		sb.append("index ");
		sb.append(_createIndexName(tableName, columnNames, indexPrefix));
		sb.append(" on ");
		sb.append(tableName);

		sb.append(StringPool.SPACE);
		sb.append(StringPool.OPEN_PARENTHESIS);

		for (int i = 0; i < columnNames.length; i++) {
			sb.append(columnNames[i]);

			if ((columnSizes != null) && (columnSizes[i] > 0)) {
				sb.append("[$COLUMN_LENGTH:");
				sb.append(columnSizes[i]);
				sb.append("$]");
			}

			sb.append(StringPool.COMMA_AND_SPACE);
		}

		sb.setIndex(sb.index() - 1);

		sb.append(StringPool.CLOSE_PARENTHESIS);
		sb.append(StringPool.SEMICOLON);

		return sb.toString();
	}

	public static String getCreateSQL(
			Connection connection, boolean unique, String tableName,
			String[] columnNames, String indexPrefix)
		throws SQLException {

		return getCreateSQL(
			unique, tableName, columnNames,
			_getColumnSizes(connection, tableName, columnNames), indexPrefix);
	}

	public static String[] trimColumnNames(String[] columnNames) {
		String[] trimmedColumnNames = columnNames.clone();

		for (int i = 0; i < trimmedColumnNames.length; i++) {
			trimmedColumnNames[i] = _trimColumnName(trimmedColumnNames[i]);
		}

		return trimmedColumnNames;
	}

	private static String _createIndexName(
		String tableName, String[] columnNames, String indexPrefix) {

		StringBundler sb = new StringBundler(4 + (columnNames.length * 2));

		sb.append(tableName);
		sb.append(StringPool.SPACE);
		sb.append(StringPool.OPEN_PARENTHESIS);

		for (String columnName : columnNames) {
			sb.append(columnName);
			sb.append(StringPool.COMMA_AND_SPACE);
		}

		sb.setIndex(sb.index() - 1);

		sb.append(StringPool.CLOSE_PARENTHESIS);
		sb.append(StringPool.SEMICOLON);

		String specification = sb.toString();

		String specificationHash = StringUtil.toHexString(
			specification.hashCode());

		specificationHash = StringUtil.toUpperCase(specificationHash);

		return indexPrefix.concat(specificationHash);
	}

	private static int[] _getColumnSizes(
			Connection connection, String tableName, String[] columnNames)
		throws SQLException {

		DatabaseMetaData databaseMetaData = connection.getMetaData();

		DB db = DBManagerUtil.getDB();

		DBInspector dbInspector = new DBInspector(connection);

		int[] columnSizes = new int[columnNames.length];

		try (ResultSet resultSet = databaseMetaData.getColumns(
				dbInspector.getCatalog(), dbInspector.getSchema(),
				dbInspector.normalizeName(tableName), null)) {

			Map<String, Integer> columnSizeMap = new HashMap<>();

			while (resultSet.next()) {
				int columnType = resultSet.getInt("DATA_TYPE");

				if (!db.isVarchar(columnType)) {
					continue;
				}

				columnSizeMap.put(
					dbInspector.normalizeName(
						resultSet.getString("COLUMN_NAME"), databaseMetaData),
					resultSet.getInt("COLUMN_SIZE"));
			}

			for (int i = 0; i < columnNames.length; i++) {
				columnSizes[i] = MapUtil.getInteger(
					columnSizeMap, columnNames[i], 0);
			}
		}

		return columnSizes;
	}

	private static String _trimColumnName(String columnName) {
		int index = columnName.indexOf("[$COLUMN_LENGTH:");

		if (index > 0) {
			columnName = columnName.substring(0, index);
		}

		return columnName;
	}

}