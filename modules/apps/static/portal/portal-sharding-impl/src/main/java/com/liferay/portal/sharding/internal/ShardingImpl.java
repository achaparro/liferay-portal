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

package com.liferay.portal.sharding.internal;

import com.liferay.document.library.kernel.service.DLFileEntryTypeLocalService;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.events.StartupHelperUtil;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBInspector;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.db.DBType;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.model.CompanyConstants;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.sharding.kernel.util.Sharding;

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
@Component(immediate = true, service = Sharding.class)
public class ShardingImpl implements Sharding {

	@Override
	public void addShard(long companyId) throws Exception {
		Connection connection = DataAccess.getConnection();

		String schemaName = "company" + companyId;

		Statement statement = connection.createStatement();

		statement.executeUpdate("create schema " + schemaName);

		DatabaseMetaData databaseMetaData = connection.getMetaData();

		DBInspector dbInspector = new DBInspector(connection);

		ResultSet tables = databaseMetaData.getTables(
			dbInspector.getCatalog(), dbInspector.getSchema(), null,
			new String[] {"TABLE"});

		while (tables.next()) {
			String tableName = dbInspector.normalizeName(
				tables.getString("TABLE_NAME"));

			if (_isControlTable(connection, tableName)) {
				statement.executeUpdate(
					StringBundler.concat(
						"CREATE VIEW ", schemaName, StringPool.PERIOD,
						tableName, " AS SELECT * from ", tableName));
			}
			else {
				statement.executeUpdate(
					StringBundler.concat(
						"CREATE TABLE ", schemaName, StringPool.PERIOD,
						tableName, " LIKE ", tableName));
			}
		}

		_dlFileEntryTypeLocalService.getBasicDocumentDLFileEntryType();
	}

	@Override
	public Connection useShard(Connection connection) throws SQLException {
		if (connection.isReadOnly()) {
			return connection;
		}

		long companyId = CompanyThreadLocal.getCompanyId();

		Statement statement = connection.createStatement();

		if ((companyId == CompanyConstants.SYSTEM) ||
			!StartupHelperUtil.isStartupFinished() ||
			(companyId == _portal.getDefaultCompanyId())) {

			statement.execute("USE companyDefault");
		}
		else {
			statement.execute("USE company" + companyId);
		}

		return connection;
	}

	@Override
	public void validate() throws Exception {
		DB db = DBManagerUtil.getDB();

		if (db.getDBType() != DBType.MYSQL) {
			throw new RuntimeException("Sharding requires MySQL");
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

	@Reference(
		cardinality = ReferenceCardinality.OPTIONAL,
		policyOption = ReferencePolicyOption.GREEDY
	)
	private DLFileEntryTypeLocalService _dlFileEntryTypeLocalService;

	@Reference(
		cardinality = ReferenceCardinality.OPTIONAL,
		policyOption = ReferencePolicyOption.GREEDY
	)
	private Portal _portal;

}