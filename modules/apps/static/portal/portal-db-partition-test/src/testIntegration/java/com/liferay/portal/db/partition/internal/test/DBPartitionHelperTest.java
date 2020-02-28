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

package com.liferay.portal.db.partition.internal.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.petra.lang.SafeClosable;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.db.DBType;
import com.liferay.portal.kernel.dao.db.partition.DBPartitionHelper;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.model.CompanyInfo;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.service.CompanyInfoLocalService;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.AssumeTestRule;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Objects;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.util.promise.Promise;

/**
 * @author Alberto Chaparro
 */
@RunWith(Arquillian.class)
public class DBPartitionHelperTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new AssumeTestRule("assume"), new LiferayIntegrationTestRule());

	public static void assume() {
		DB db = DBManagerUtil.getDB();

		Assume.assumeTrue(db.getDBType() == DBType.MYSQL);
	}

	@BeforeClass
	public static void setUpClass() throws Exception {
		Properties properties = PropsUtil.getProperties();

		properties.setProperty(
			"database.partition.enabled", Boolean.TRUE.toString());

		Bundle bundle = FrameworkUtil.getBundle(DBPartitionHelperTest.class);

		BundleContext bundleContext = bundle.getBundleContext();

		ComponentDescriptionDTO componentDescriptionDTO = null;

		for (Bundle currentBundle : bundleContext.getBundles()) {
			if (Objects.equals(
					currentBundle.getSymbolicName(),
					"com.liferay.portal.db.partition.impl")) {

				componentDescriptionDTO =
					_componentController.getComponentDescriptionDTO(
						currentBundle,
						"com.liferay.portal.db.partition.internal." +
							"DBPartitionHelperImpl");
			}
		}

		Promise<Void> promise = _componentController.disableComponent(componentDescriptionDTO);

		Promise<Void> promise2 = _componentController.enableComponent(componentDescriptionDTO);

		_db = DBManagerUtil.getDB();

		_connection = DataAccess.getConnection();

		_dbPartitionHelper.setDefaultCompanyId(_portal.getDefaultCompanyId());
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		Properties properties = PropsUtil.getProperties();

		properties.remove("database.partition.enabled");

		Bundle bundle = FrameworkUtil.getBundle(DBPartitionHelperTest.class);

		BundleContext bundleContext = bundle.getBundleContext();

		_db.runSQL("drop schema company" + _COMPANY_ID);

		DataAccess.cleanUp(_connection);
	}

	@Test
	public void testAddDefaultPartition() {
		_dbPartitionHelper.addPartition(_portal.getDefaultCompanyId());
	}

	@Test
	public void testAddPartition() throws Exception {
		_dbPartitionHelper.addPartition(_COMPANY_ID);

		try (Statement statement = _connection.createStatement()) {
			statement.execute(
				"select 1 from company" + _COMPANY_ID + ".CompanyInfo");
		}
	}

	@Test
	public void testUseDefaultPartition() {
		try (SafeClosable safeClosable =
				CompanyThreadLocal.setCompanyIdInitialization(
					_portal.getDefaultCompanyId())) {

			_dbPartitionHelper.usePartition(_connection);

			CompanyInfo companyInfo = _companyInfoLocalService.fetchCompany(
				_portal.getDefaultCompanyId());

			Assert.assertEquals(
				_portal.getDefaultCompanyId(), companyInfo.getCompanyId());
		}
		finally {
			_dbPartitionHelper.usePartition(_connection);
		}
	}

	@Test
	public void testUsePartition() {
		try (SafeClosable safeClosable =
				CompanyThreadLocal.setCompanyIdInitialization(_COMPANY_ID)) {

			_dbPartitionHelper.usePartition(_connection);

			CompanyInfo companyInfo = _companyInfoLocalService.fetchCompany(
				_COMPANY_ID);

			Assert.assertEquals(null, companyInfo);
		}
		finally {
			_dbPartitionHelper.usePartition(_connection);
		}
	}

	private static final long _COMPANY_ID = 1L;

	@Inject
	private static ServiceComponentRuntime _componentController;

	@Inject
	private static CompanyInfoLocalService _companyInfoLocalService;

	private static Connection _connection;
	private static DB _db;

	@Inject
	private static DBPartitionHelper _dbPartitionHelper;

	@Inject
	private static Portal _portal;

}