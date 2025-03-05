package com.liferay.portal.db.remover;

import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringUtil;

import java.sql.ResultSetMetaData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PortalDuplicateRemover implements DuplicateRemover {

	@Override
	public void removeDuplicates(String tableName, String indexesSQL) {
		Map<Long, List<HashMap<String, String>>> duplicatesMap =
			getDuplicates(tableName, indexesSQL);

		for (Map.Entry<Long, List<HashMap<String, String>>> entry : duplicatesMap.entrySet()) {
			List<HashMap<String, String>> duplicateGroup = entry.getValue();

			int duplicateNumber = duplicateGroup.size();

			int counter = 0;

			for (HashMap<String, String> queryMap : duplicateGroup) {
				StringBundler sb = new StringBundler();
				sb.append("DELETE FROM ");
				sb.append(tableName);
				sb.append(" WHERE ");

				for (String key : queryMap.keySet()) {
					sb.append(key);
					sb.append(" = '");
					sb.append(queryMap.get(key)+"' ");

					if (counter < queryMap.size() - 1 ) {
						sb.append("AND ");
					}
				}
				String sql = sb.toString();

				try(Connection connection = DataAccess.getConnection()) {
					PreparedStatement preparedStatement1 =
						connection.prepareStatement(sql);

					preparedStatement1.execute();
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
				finally {
					duplicateNumber--;
				}
				if (duplicateNumber == 1) break;

			}


		}
	}

	@Override
	public Map<Long, List<HashMap<String, String>>> getDuplicates(String tableName, String indexesSQL) {
		Map<String[], String> indexesDuplicatesMap = new LinkedHashMap<>();

		Map<Long, List<HashMap<String, String>>> duplicatesMap = new LinkedHashMap<>();

		List<String> indexesColumnsList = _getIndexesColumnsList(indexesSQL);

		for (String indexColumns : indexesColumnsList){
			StringBundler sb = new StringBundler();
			sb.append("SELECT ");
			sb.append(indexColumns);
			sb.append(" FROM");
			sb.append(tableName);
			sb.append(" GROUP BY ");
			sb.append(indexColumns);
			sb.append(" HAVING COUNT(*) > 1;");
			String sql = sb.toString();
			try(Connection connection = DataAccess.getConnection();
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

					indexesDuplicatesMap.put(columnResults, indexColumns);
				}

			}
			catch (Exception e){
				throw new RuntimeException(e);
			}

		}

		Long duplicateGroupNumber = 1L;

		for (Map.Entry<String[], String> entry : indexesDuplicatesMap.entrySet()) {

			String[] results = entry.getKey();

			String[] columns = entry.getValue().split(", ");

			StringBundler sb = new StringBundler();

			sb.append("SELECT * FROM");

			sb.append(tableName);

			sb.append(" WHERE ");

			for (int i = 0; i < columns.length; i++) {
				sb.append(columns[i]);
				sb.append(" = ");
				sb.append(results[i]);
				if (i < columns.length - 1 ) {
					sb.append(" AND ");
				}
			}

			String sql = sb.toString();

			List<HashMap<String, String>>
				queryResult= new ArrayList<>();

			try(Connection connection = DataAccess.getConnection();
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
					HashMap<String, String>
						queryMap = new HashMap<>();

					for (int i = 0; i < columnCount; i++) {
						String value = resultSet.getString(columnNames[i]);

						queryMap.put(columnNames[i], value);
					}

					queryResult.add(queryMap);

					duplicatesMap.put(duplicateGroupNumber, queryResult);
				}

				duplicateGroupNumber++;
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		return duplicatesMap;
	}

	private static List<String> _getIndexesColumnsList(String indexesSQL) {
		List<String> indexesColumns = new ArrayList<>();
		String[] indexColumnsArray = StringUtil.split(indexesSQL, "\n");
			for (String indexColumns : indexColumnsArray) {
				String columns = indexColumns.substring(indexColumns.indexOf(" (")+2, indexColumns.indexOf(");"));
				columns = columns.replaceAll("\\[.*?]", "");
				indexesColumns.add(columns);
			}
		return indexesColumns;
	}
}
