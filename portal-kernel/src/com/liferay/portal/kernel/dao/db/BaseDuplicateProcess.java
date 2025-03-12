package com.liferay.portal.kernel.dao.db;

import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BaseDuplicateProcess extends UpgradeProcess implements DuplicateProcess {

	@Override
	public void removeDuplicates(String tableName, String index, List<HashMap<String, String>> duplicatesList, boolean deleteAll) {
				int duplicateCount = duplicatesList.size();

				for (HashMap<String, String> duplicate : duplicatesList) {
					if (duplicateCount == 1 && deleteAll) {
						break;
					}
					StringBundler sb = new StringBundler();

					sb.append("DELETE FROM ");
					sb.append(tableName);
					sb.append(" WHERE ");

					int counter = 0;

					for (Map.Entry<String, String> querySet :
						duplicate.entrySet()) {

						sb.append(querySet.getKey());

						if (querySet.getValue() == null) {
							sb.append(" IS NULL ");
						}
						else {
							sb.append(" = '");
							sb.append(escape(querySet.getValue()));
							sb.append("' ");
						}

						if (counter < (duplicate.size() - 1)) {
							sb.append("AND ");
						}

						counter++;
					}

					sb.append(";");

					String sql = sb.toString();

					try (Connection connection = DataAccess.getConnection()) {
						PreparedStatement preparedStatement1 =
							connection.prepareStatement(sql);

						preparedStatement1.execute();
					}
					catch (SQLException exception) {
						throw new RuntimeException(exception);
					}
					finally {
						logDeletedDuplicates(tableName, index, duplicate);
						duplicateCount--;
					}
				}
		}

	protected List<HashMap<String, String>> getDuplicatesSQL(
		String[] uniqueIndexColumn, String tableName, String orderBy)
		throws SQLException {
			List<HashMap<String, String>> queryResult = new ArrayList<>();

			String[] columns = uniqueIndexColumn[1].split(",");

		List<String[]> indexDuplicatesList = getIndexDuplicatesList(
			tableName, uniqueIndexColumn[1]);

		for (String[] indexDuplicateArray : indexDuplicatesList) {
			StringBundler sb = new StringBundler();

			sb.append("SELECT * FROM ");
			sb.append(tableName);
			sb.append(" WHERE ");

			for (int i = 0; i < indexDuplicateArray.length; i++) {
				sb.append(columns[i]);

				if (indexDuplicateArray[i] == null) {
					sb.append(" IS NULL ");
				}
				else {
					sb.append(" = '");
					sb.append(escape(indexDuplicateArray[i]));
					sb.append("' ");
				}

				if (i < (columns.length - 1)) {
					sb.append("AND ");
				}
			}

			if (orderBy != null) {
				sb.append("ORDER BY ");
				sb.append(orderBy);
			}

			sb.append(";");

			String sql = sb.toString();

			try (Connection connection = DataAccess.getConnection();
				 PreparedStatement preparedStatement =
					 connection.prepareStatement(sql);
				 ResultSet resultSet = preparedStatement.executeQuery()) {

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
				}
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		}

		return queryResult;
	}

	protected List<String[]> getIndexDuplicatesList(
		String tableName, String columns) {

		List<String[]> indexesDuplicatesList = new ArrayList<>();

		StringBundler sb = new StringBundler(7);

		sb.append("SELECT ");
		sb.append(columns);
		sb.append(" FROM ");
		sb.append(tableName);
		sb.append(" GROUP BY ");
		sb.append(columns);
		sb.append(" HAVING COUNT(*) > 1;");

		String sql = sb.toString();

		try (Connection connection = DataAccess.getConnection();
			 PreparedStatement preparedStatement =
				 connection.prepareStatement(sql);

			 ResultSet resultSet = preparedStatement.executeQuery()) {

			ResultSetMetaData metaData = resultSet.getMetaData();

			int columnCount = metaData.getColumnCount();

			String[] columnResults = new String[columnCount];

			while (resultSet.next()) {
				for (int i = 1; i <= columnCount; i++) {
					String value = resultSet.getString(i);

					columnResults[i - 1] = value;
				}

				indexesDuplicatesList.add(columnResults);
			}
		}
		catch (Exception exception) {
			throw new RuntimeException(exception);
		}

		return indexesDuplicatesList;
	}

	protected static String escape(Object object) {
		return StringUtil.replace(
			String.valueOf(object), _DB_ESCAPE_STRINGS[0],
			_DB_ESCAPE_STRINGS[1]);
	}

	protected void logDeletedDuplicates(
		String tableName, String index, Map<String, String> duplicate) {

		if (_log.isWarnEnabled()) {
			_log.warn(
				StringBundler.concat(
					"Duplicate removed:\n", tableName, "/", index, "/",
					duplicate.toString()));
		}
	}

	private static final String[][] _DB_ESCAPE_STRINGS = {
		{"\\", "'"}, {"\\\\", "''"}
	};

	private static final Log _log = LogFactoryUtil.getLog(
		BaseDuplicateProcess.class);


	@Override
	protected void doUpgrade() throws Exception {

	}
}
