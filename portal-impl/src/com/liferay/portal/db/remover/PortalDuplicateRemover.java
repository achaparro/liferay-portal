/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.db.remover;

import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jorge Avalos
 */
public class PortalDuplicateRemover implements DuplicateRemover {

	@Override
	public void removeDuplicates(String tableName, String indexesSQL) {
		Map<String, List<HashMap<String, String>>> duplicatesMap =
			getDuplicatesSQL(tableName, indexesSQL);

		for (Map.Entry<String, List<HashMap<String, String>>> entry :
				duplicatesMap.entrySet()) {

			String index = entry.getKey();

			List<HashMap<String, String>> duplicateGroup = entry.getValue();

			int duplicateNumber = duplicateGroup.size();

			int counter = 0;

			for (HashMap<String, String> queryMap : duplicateGroup) {
				StringBundler sb = new StringBundler();

				sb.append("DELETE FROM ");

				sb.append(tableName);

				sb.append(" WHERE ");

				for (Map.Entry<String, String> querySet : queryMap.entrySet()) {
					sb.append(querySet.getKey());
					sb.append(" = '");
					sb.append(querySet.getValue());
					sb.append("' ");

					if (counter < (queryMap.size() - 1)) {
						sb.append("AND ");
					}
				}

				sb.append(";");

				String sql = sb.toString();

				try (Connection connection = DataAccess.getConnection()) {
					PreparedStatement preparedStatement1 =
						connection.prepareStatement(sql);

					preparedStatement1.execute();
				}
				catch (Exception exception) {
					throw new RuntimeException(exception);
				}
				finally {
					if (_log.isWarnEnabled()) {
						_log.warn(index);
					}

					duplicateNumber--;
				}

				if (duplicateNumber == 1) {
					break;
				}
			}
		}
	}

	protected Map<String, List<HashMap<String, String>>> getDuplicatesSQL(
		String tableName, String indexesSQL) {

		Map<String[], String> indexesDuplicatesMap = new LinkedHashMap<>();

		Map<String, String> indexesColumnsMap = getIndexesColumnsList(
			indexesSQL);

		for (Map.Entry<String, String> indexColumns :
				indexesColumnsMap.entrySet()) {

			StringBundler sb = new StringBundler(7);

			sb.append("SELECT ");
			sb.append(indexColumns.getKey());
			sb.append(" FROM");
			sb.append(tableName);
			sb.append(" GROUP BY ");
			sb.append(indexColumns.getKey());
			sb.append(" HAVING COUNT(*) > 1;");

			String sql = sb.toString();

			try (Connection connection = DataAccess.getConnection();
				PreparedStatement preparedStatement1 =
					connection.prepareStatement(sql);
				ResultSet resultSet = preparedStatement1.executeQuery()) {

				ResultSetMetaData metaData = resultSet.getMetaData();

				int columnCount = metaData.getColumnCount();

				String[] columnResults = new String[columnCount];

				while (resultSet.next()) {
					for (int i = 1; i <= columnCount; i++) {
						String value = resultSet.getString(i);

						columnResults[i - 1] = value;
					}

					indexesDuplicatesMap.put(
						columnResults, indexColumns.getKey());
				}
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		}

		Map<String, List<HashMap<String, String>>> duplicatesMap =
			new LinkedHashMap<>();

		for (Map.Entry<String[], String> entry :
				indexesDuplicatesMap.entrySet()) {

			String[] results = entry.getKey();

			String index = indexesColumnsMap.get(entry.getValue());

			String[] columns = entry.getValue(
			).split(
				", "
			);

			StringBundler sb = new StringBundler();

			sb.append("SELECT * FROM");
			sb.append(tableName);
			sb.append(" WHERE ");

			for (int i = 0; i < columns.length; i++) {
				sb.append(columns[i]);
				sb.append(" = ");
				sb.append(results[i]);

				if (i < (columns.length - 1)) {
					sb.append(" AND ");
				}
			}

			String sql = sb.toString();

			List<HashMap<String, String>> queryResult = new ArrayList<>();

			try (Connection connection = DataAccess.getConnection();
				PreparedStatement preparedStatement1 =
					connection.prepareStatement(sql);
				ResultSet resultSet = preparedStatement1.executeQuery()) {

				ResultSetMetaData metaData = resultSet.getMetaData();

				int columnCount = metaData.getColumnCount();

				String[] columnNames = new String[columnCount];

				for (int i = 1; i <= columnCount; i++) {
					String columnName = metaData.getColumnName(i);

					columnNames[i - 1] = columnName;
				}

				while (resultSet.next()) {
					HashMap<String, String> queryMap = new HashMap<>();

					for (int i = 0; i < columnCount; i++) {
						String value = resultSet.getString(columnNames[i]);

						queryMap.put(columnNames[i], value);
					}

					queryResult.add(queryMap);

					duplicatesMap.put(index, queryResult);
				}
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		}

		return duplicatesMap;
	}

	protected Map<String, String> getIndexesColumnsList(String indexesSQL) {
		Map<String, String> indexesColumns = new HashMap<>();

		String[] indexColumnsArray = StringUtil.split(indexesSQL, "\n");

		for (String indexColumns : indexColumnsArray) {
			String index = indexColumns.substring(
				indexColumns.indexOf("index ") + 1, indexColumns.indexOf("on"));

			String columns = indexColumns.substring(
				indexColumns.indexOf(" (") + 2, indexColumns.indexOf(");"));

			columns = columns.replaceAll("\\[.*?]", "");

			indexesColumns.put(columns, index);
		}

		return indexesColumns;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		PortalDuplicateRemover.class);

}