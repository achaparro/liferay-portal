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

package com.liferay.portal.kernel.upgrade;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.upgrade.util.UpgradeProcessUtil;
import com.liferay.portal.kernel.util.AggregateResourceBundleLoader;
import com.liferay.portal.kernel.util.LocalizationUtil;
import com.liferay.portal.kernel.util.ResourceBundleLoader;
import com.liferay.portal.kernel.util.ResourceBundleUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringUtil;

import java.io.IOException;

import java.sql.SQLException;
import java.sql.Types;

import java.util.Locale;
import java.util.Map;

/**
 * @author Leon Chi
 */
public abstract class BaseUpgradeLocalizedColumn extends UpgradeProcess {

	protected void executeClobUpgrades() throws Exception {
	}

	protected void upgradeLocalizedColumn(
			ResourceBundleLoader resourceBundleLoader, Class<?> tableClass,
			String columnName, String originalContent,
			String localizationMapKey, String localizationXMLKey,
			long[] companyIds)
		throws SQLException {

		Class<?> clazz = getClass();

		resourceBundleLoader = new AggregateResourceBundleLoader(
			ResourceBundleUtil.getResourceBundleLoader(
				"content.Language", clazz.getClassLoader()),
			resourceBundleLoader);

		for (long companyId : companyIds) {
			try {
				_upgrade(
					resourceBundleLoader, tableClass, columnName,
					originalContent, localizationMapKey, localizationXMLKey,
					companyId);
			}
			catch (Exception e) {
				throw new SQLException(e);
			}
		}
	}

	/**
	* @deprecated As of 7.0.0,
	* use {@link BaseUpgradeLocalizedColumn#upgradeLocalizedColumn(ResourceBundleLoader, Class, String, String, String, String, long[])}
	*/
	@Deprecated
	protected void upgradeLocalizedColumn(
			ResourceBundleLoader resourceBundleLoader, String tableName,
			String columnName, String originalContent,
			String localizationMapKey, String localizationXMLKey,
			long[] companyIds)
		throws SQLException {

		Class<?> clazz = getClass();

		resourceBundleLoader = new AggregateResourceBundleLoader(
			ResourceBundleUtil.getResourceBundleLoader(
				"content.Language", clazz.getClassLoader()),
			resourceBundleLoader);

		for (long companyId : companyIds) {
			_upgrade(
				resourceBundleLoader, tableName, columnName, originalContent,
				localizationMapKey, localizationXMLKey, companyId);
		}
	}

	private void _checkJavaColumnType(Class<?> tableClass, String columnName)
		throws Exception {

		if (_getJavaColumnType(tableClass, columnName) != Types.CLOB) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					String.format(
						_CLOB_ERROR, columnName, getTableName(tableClass)));
			}
		}
	}

	private String _escape(String string) {
		return string.replace(
			StringPool.APOSTROPHE, StringPool.DOUBLE_APOSTROPHE);
	}

	private int _getJavaColumnType(Class<?> tableClass, String columnName)
		throws Exception {

		Object[][] tableColumns = getTableColumns(tableClass);

		for (Object[] column : tableColumns) {
			String name = (String)column[0];

			if (StringUtil.equalsIgnoreCase(columnName, name)) {
				return (int)column[1];
			}
		}

		throw new IllegalArgumentException(
			String.format(
				_INVALID_COLUMN_NAME, columnName, getTableName(tableClass)));
	}

	private String _getLocalizationXML(
			String localizationMapKey, String localizationXMLKey,
			long companyId, ResourceBundleLoader resourceBundleLoader)
		throws SQLException {

		Long originalCompanyId = CompanyThreadLocal.getCompanyId();

		CompanyThreadLocal.setCompanyId(companyId);

		try {
			Map<Locale, String> localizationMap =
				ResourceBundleUtil.getLocalizationMap(
					resourceBundleLoader, localizationMapKey);

			String defaultLanguageId = UpgradeProcessUtil.getDefaultLanguageId(
				companyId);

			return LocalizationUtil.updateLocalization(
				localizationMap, "", localizationXMLKey, defaultLanguageId);
		}
		finally {
			CompanyThreadLocal.setCompanyId(originalCompanyId);
		}
	}

	private void _upgrade(
			ResourceBundleLoader resourceBundleLoader, Class<?> tableClass,
			String columnName, String originalContent,
			String localizationMapKey, String localizationXMLKey,
			long companyId)
		throws Exception {

		_checkJavaColumnType(tableClass, columnName);
		executeClobUpgrades();

		_upgrade(
			resourceBundleLoader, getTableName(tableClass), columnName,
			originalContent, localizationMapKey, localizationXMLKey, companyId);
	}

	private void _upgrade(
			ResourceBundleLoader resourceBundleLoader, String tableName,
			String columnName, String originalContent,
			String localizationMapKey, String localizationXMLKey,
			long companyId)
		throws SQLException {

		String localizationXML = _getLocalizationXML(
			localizationMapKey, localizationXMLKey, companyId,
			resourceBundleLoader);

		String sql = StringBundler.concat(
			"update ", tableName, " set ", columnName, " = '",
			_escape(localizationXML), "' where CAST_CLOB_TEXT(", columnName,
			") = '", _escape(originalContent), "' and companyId = ",
			String.valueOf(companyId));

		try {
			runSQL(sql);
		}
		catch (IOException ioe) {
			throw new SystemException(ioe);
		}
	}

	private static final String _CLOB_ERROR =
		"If column %s is internationalized in %s it should be changed to CLOB";

	private static final String _INVALID_COLUMN_NAME =
		"Invalid column name %s for %s";

	private static final Log _log = LogFactoryUtil.getLog(
		BaseUpgradeLocalizedColumn.class);

}