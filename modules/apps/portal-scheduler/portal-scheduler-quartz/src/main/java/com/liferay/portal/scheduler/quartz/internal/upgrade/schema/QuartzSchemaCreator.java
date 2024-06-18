/**
 * SPDX-FileCopyrightText: (c) 2023 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.scheduler.quartz.internal.upgrade.schema;

import com.liferay.portal.db.DBResourceUtil;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.upgrade.UpgradeException;
import com.liferay.portal.upgrade.release.SchemaCreator;

import java.sql.Connection;

import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Component;

/**
 * @author Shuyang Zhou
 */
@Component(service = SchemaCreator.class)
public class QuartzSchemaCreator implements SchemaCreator {

	public QuartzSchemaCreator(Bundle bundle) {
		_bundle = bundle;
	}

	@Override
	public void create() throws UpgradeException {
		DB db = DBManagerUtil.getDB();

		String indexesSQL = DBResourceUtil.getModuleIndexesSQL(_bundle);
		String tablesSQL = DBResourceUtil.getModuleTablesSQL(_bundle);

		try (Connection connection = DataAccess.getConnection()) {
			db.runSQLTemplate(connection, tablesSQL, true);

			db.runSQLTemplate(connection, indexesSQL, true);
		}
		catch (Exception exception) {
			throw new UpgradeException(exception);
		}
	}

	@Override
	public String getBundleSymbolicName() {
		return _bundle.getSymbolicName();
	}

	@Override
	public String getSchemaVersion() {
		return null;
	}

	private final Bundle _bundle;

}