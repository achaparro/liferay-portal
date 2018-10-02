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

package com.liferay.portal.index.updater.internal;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.index.updater.IndexUpdater;
import com.liferay.portal.index.updater.exception.NotLiferayServiceException;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;

import java.net.URL;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * @author Ricardo Couso
 */
@Component(immediate = true, service = IndexUpdater.class)
public class IndexUpdaterImpl implements IndexUpdater {

	@Activate
	public void activate(BundleContext bundleContext) {
		_bundleContext = bundleContext;
	}

	@Override
	public void updateIndexes(long bundleId) throws NotLiferayServiceException {
		Bundle bundle = _bundleContext.getBundle(bundleId);

		if (bundle == null) {
			throw new IllegalArgumentException(
				"Module with id " + bundleId + " does not exist");
		}

		_updateIndexes(bundle);
	}

	@Override
	public void updateIndexes(String bundleSymbolicName)
		throws NotLiferayServiceException {

		Bundle bundle = _getBundle(bundleSymbolicName);

		if (bundle == null) {
			throw new IllegalArgumentException(
				"Module with symbolic name " + bundleSymbolicName +
					" does not exist");
		}

		_updateIndexes(bundle);
	}

	@Override
	public void updateIndexesAll() {
		DB db = DBManagerUtil.getDB();

		Connection connection = null;

		try {
			connection = DataAccess.getConnection();

			for (Bundle bundle : _bundleContext.getBundles()) {
				if (_isLiferayService(bundle)) {
					_executeUpdateIndexes(db, connection, bundle);
				}
			}
		}
		catch (Exception e) {
			_log.error(e, e);
		}
		finally {
			DataAccess.cleanUp(connection);
		}
	}

	private void _executeUpdateIndexes(
			DB db, Connection connection, Bundle bundle)
		throws IOException, SQLException {

		if (_log.isInfoEnabled()) {
			_log.info(
				"Updating database indexes for " + bundle.getSymbolicName());
		}

		String indexesSQL = _getSQLTemplateString(bundle, "indexes.sql");
		String tablesSQL = _getSQLTemplateString(bundle, "tables.sql");

		if ((indexesSQL == null) || (tablesSQL == null)) {
			return;
		}

		db.updateIndexes(connection, tablesSQL, indexesSQL, true);
	}

	private Bundle _getBundle(String bundleSymbolicName) {
		for (Bundle bundle : _bundleContext.getBundles()) {
			if (bundleSymbolicName.equals(bundle.getSymbolicName())) {
				return bundle;
			}
		}

		return null;
	}

	private String _getSQLTemplateString(Bundle bundle, String templateName) {
		URL resource = bundle.getResource("/META-INF/sql/" + templateName);

		if (resource == null) {
			return null;
		}

		try (InputStream inputStream = resource.openStream()) {
			return StringUtil.read(inputStream);
		}
		catch (IOException ioe) {
			_log.error("Unable to read SQL template " + templateName, ioe);

			return null;
		}
	}

	private boolean _isLiferayService(Bundle bundle) {
		if (bundle == null) {
			return false;
		}

		Dictionary<String, String> headers = bundle.getHeaders(
			StringPool.BLANK);

		return GetterUtil.getBoolean(headers.get("Liferay-Service"));
	}

	private void _updateIndexes(Bundle bundle)
		throws NotLiferayServiceException {

		if (_isLiferayService(bundle)) {
			DB db = DBManagerUtil.getDB();

			Connection connection = null;

			try {
				connection = DataAccess.getConnection();

				_executeUpdateIndexes(db, connection, bundle);
			}
			catch (IOException | SQLException e) {
				_log.error(e, e);
			}
			finally {
				DataAccess.cleanUp(connection);
			}
		}
		else {
			throw new NotLiferayServiceException(
				"Module " + bundle.getSymbolicName() +
					" is not of type Liferay-Service");
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		IndexUpdaterImpl.class);

	private BundleContext _bundleContext;

}