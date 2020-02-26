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
import com.liferay.portal.kernel.dao.jdbc.CurrentConnection;
import com.liferay.portal.kernel.dao.jdbc.CurrentConnectionUtil;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.service.CompanyInfoLocalService;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.AssumeTestRule;
import com.liferay.portal.kernel.test.util.PropsTestUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

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
import org.osgi.framework.ServiceReference;
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
		Bundle bundle = FrameworkUtil.getBundle(DBPartitionHelperTest.class);

		_bundleContext = bundle.getBundleContext();

		PropsTestUtil.setProps(
			"database.partition.enabled", Boolean.TRUE.toString());

		_restartDBPartitionHelperImpl();

		ServiceReference<DBPartitionHelper> serviceReference =
			_bundleContext.getServiceReference(DBPartitionHelper.class);

		_dbPartitionHelper = _bundleContext.getService(serviceReference);

		_dbPartitionHelper.setDefaultCompanyId(_portal.getDefaultCompanyId());

		_db = DBManagerUtil.getDB();

		_connection = DataAccess.getConnection();
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		_db.runSQL("drop schema company" + _COMPANY_ID);

		PropsTestUtil.setProps(
			"database.partition.enabled", Boolean.FALSE.toString());

		_restartDBPartitionHelperImpl();

		_bundleContext.ungetService(
			_bundleContext.getServiceReference(DBPartitionHelper.class));

		DataAccess.cleanUp(_connection);
	}

	@Test
	public void testAddDefaultPartition() {
		_dbPartitionHelper.addPartition(_portal.getDefaultCompanyId());
	}

	@Test
	public void testAddPartition() throws Exception {
		CurrentConnection defaultCurrentConnection =
			CurrentConnectionUtil.getCurrentConnection();

		try {
			CurrentConnection currentConnection = new CurrentConnection() {

				@Override
				public Connection getConnection(DataSource dataSource) {
					return _connection;
				}

			};

			ReflectionTestUtil.setFieldValue(
				CurrentConnectionUtil.class, "_currentConnection",
				currentConnection);

			_dbPartitionHelper.addPartition(_COMPANY_ID);

			try (Statement statement = _connection.createStatement()) {
				statement.execute(
					"select 1 from company" + _COMPANY_ID + ".CompanyInfo");
			}
		}
		finally {
			ReflectionTestUtil.setFieldValue(
				CurrentConnectionUtil.class, "_currentConnection",
				defaultCurrentConnection);
		}
	}

	@Test
	public void testUseDefaultPartition() throws SQLException {
		try (SafeClosable safeClosable =
				CompanyThreadLocal.setCompanyIdInitialization(
					_portal.getDefaultCompanyId())) {

			_dbPartitionHelper.usePartition(_connection);

			Assert.assertTrue(_hasCompanyInfoRecords());
		}
	}

	@Test
	public void testUsePartition() throws SQLException {
		try (SafeClosable safeClosable =
				CompanyThreadLocal.setCompanyIdInitialization(_COMPANY_ID)) {

			_dbPartitionHelper.usePartition(_connection);

			Assert.assertFalse(_hasCompanyInfoRecords());
		}
		finally {
			_dbPartitionHelper.usePartition(_connection);
		}
	}

	private static void _restartDBPartitionHelperImpl() throws Exception {
		ServiceReference<DBPartitionHelper> serviceReference =
			_bundleContext.getServiceReference(DBPartitionHelper.class);

		ComponentDescriptionDTO componentDescriptionDTO =
			_serviceComponentRuntime.getComponentDescriptionDTO(
				serviceReference.getBundle(),
				"com.liferay.portal.db.partition.internal." +
					"DBPartitionHelperImpl");

		Promise<Void> promise = _serviceComponentRuntime.disableComponent(
			componentDescriptionDTO);

		promise.getValue();

		promise = _serviceComponentRuntime.enableComponent(
			componentDescriptionDTO);

		promise.getValue();
	}

	private boolean _hasCompanyInfoRecords() throws SQLException {
		try (PreparedStatement ps = _connection.prepareStatement(
				"select companyId from CompanyInfo");
			ResultSet rs = ps.executeQuery()) {

			return rs.next();
		}
	}

	private static final long _COMPANY_ID = 1L;

	private static BundleContext _bundleContext;

	@Inject
	private static CompanyInfoLocalService _companyInfoLocalService;

	private static Connection _connection;
	private static DB _db;
	private static volatile DBPartitionHelper _dbPartitionHelper;

	@Inject
	private static Portal _portal;

	@Inject
	private static ServiceComponentRuntime _serviceComponentRuntime;

}