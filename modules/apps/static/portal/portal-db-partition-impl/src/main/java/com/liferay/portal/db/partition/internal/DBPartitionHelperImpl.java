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

package com.liferay.portal.db.partition.internal;

import com.liferay.document.library.kernel.service.DLFileEntryTypeLocalService;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBInspector;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.db.DBType;
import com.liferay.portal.kernel.dao.db.partition.DBPartitionHelper;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.model.CompanyConstants;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;

/**
 * @author Alberto Chaparro
 */
@Component(immediate = true, service = DBPartitionHelper.class)
public class DBPartitionHelperImpl implements DBPartitionHelper {

	@Override
	public void addPartition(long companyId) throws Exception {
		if (companyId == _defaultCompanyId) {
			return;
		}

		Connection connection = DataAccess.getConnection();

		String schemaName = "company" + companyId;

		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate(
				StringBundler.concat(
					"create schema if not exists ", schemaName,
					" character set utf8"));
		}

		DatabaseMetaData databaseMetaData = connection.getMetaData();

		DBInspector dbInspector = new DBInspector(connection);

		try (ResultSet tables = databaseMetaData.getTables(
				dbInspector.getCatalog(), dbInspector.getSchema(), null,
				new String[] {"TABLE"});
			Statement statement = connection.createStatement()) {

			while (tables.next()) {
				String tableName = dbInspector.normalizeName(
					tables.getString("TABLE_NAME"));

				if (_isControlTable(connection, tableName)) {
					statement.executeUpdate(
						StringBundler.concat(
							"create view ", schemaName, StringPool.PERIOD,
							tableName, " as select * from ", tableName));
				}
				else {
					statement.executeUpdate(
						StringBundler.concat(
							"create table ", schemaName, StringPool.PERIOD,
							tableName, " like ", tableName));
				}
			}
		}

		_dlFileEntryTypeLocalService.getBasicDocumentDLFileEntryType();
	}

	@Override
	public boolean removePartition(long companyId) {
		return true;
	}

	@Override
	public Connection usePartition(Connection connection) throws SQLException {
		if (connection.isReadOnly()) {
			return connection;
		}

		long companyId = CompanyThreadLocal.getCompanyId();

		if ((_defaultCompanyId == 0) &&
			(companyId != CompanyConstants.SYSTEM)) {

			_defaultCompanyId = companyId;
		}

		try (Statement statement = connection.createStatement()) {
			if ((companyId == CompanyConstants.SYSTEM) ||
				(companyId == _defaultCompanyId)) {

				statement.execute("USE companyDefault");
			}
			else {
				statement.execute("USE company" + companyId);
			}
		}

		return connection;
	}

	@Override
	public void validate() throws Exception {
		DB db = DBManagerUtil.getDB();

		if (db.getDBType() != DBType.MYSQL) {
			throw new RuntimeException("Database Partition requires MySQL");
		}
	}

	private boolean _isControlTable(Connection connection, String tableName)
		throws Exception {

		if (tableName.equals("Portlet") || tableName.equals("Company") ||
			tableName.equals("VirtualHost")) {

			return true;
		}

		DBInspector dbInspector = new DBInspector(connection);

		if (!dbInspector.hasColumn(tableName, "companyId") &&
			(!tableName.equals("CTMessage") ||
			 !tableName.equals("LayoutClassedModelUsage"))) {

			return true;
		}

		return false;
	}

	private static long _defaultCompanyId;

	@Reference(
		cardinality = ReferenceCardinality.OPTIONAL,
		policyOption = ReferencePolicyOption.GREEDY
	)
	private DLFileEntryTypeLocalService _dlFileEntryTypeLocalService;

}