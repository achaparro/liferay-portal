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

package com.liferay.portal.db.partition.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.petra.lang.SafeClosable;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.dao.init.DBInitUtil;
import com.liferay.portal.db.partition.DBPartitionUtil;
import com.liferay.portal.kernel.bean.PortalBeanLocatorUtil;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.db.DBType;
import com.liferay.portal.kernel.dao.jdbc.CurrentConnection;
import com.liferay.portal.kernel.dao.jdbc.CurrentConnectionUtil;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.messaging.BaseMessageListener;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.messaging.MessageListener;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.AssumeTestRule;
import com.liferay.portal.kernel.test.util.CompanyTestUtil;
import com.liferay.portal.kernel.util.InfrastructureUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

/**
 * @author Alberto Chaparro
 */
@RunWith(Arquillian.class)
public class DBPartitionUtilTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new AssumeTestRule("assume"), new LiferayIntegrationTestRule());

	public static void assume() {
		_db = DBManagerUtil.getDB();

		Assume.assumeTrue(_db.getDBType() == DBType.MYSQL);
	}

	@BeforeClass
	public static void setUpClass() throws Exception {
		_connection = DataAccess.getConnection();
		_currentDatabasePartitionEnabledValue =
			ReflectionTestUtil.getAndSetFieldValue(
				DBPartitionUtil.class, "_DATABASE_PARTITION_ENABLED", true);
		_currentDatabasePartitionInstanceIdValue =
			ReflectionTestUtil.getAndSetFieldValue(
				DBPartitionUtil.class, "_DATABASE_PARTITION_INSTANCE_ID",
				_DB_PARTITION_INSTANCE_ID);
		_currentDataSource = ReflectionTestUtil.getFieldValue(
			DBInitUtil.class, "_dataSource");

		_defaultSchemaName = _connection.getCatalog();

		DBPartitionUtil.setDefaultCompanyId(_portal.getDefaultCompanyId());

		DataSource dbPartitionDataSource = DBPartitionUtil.wrapDataSource(
			_currentDataSource);

		_lazyConnectionDataSourceProxy =
			(LazyConnectionDataSourceProxy)PortalBeanLocatorUtil.locate(
				"liferayDataSource");

		_lazyConnectionDataSourceProxy.setTargetDataSource(
			dbPartitionDataSource);

		ReflectionTestUtil.setFieldValue(
			DBInitUtil.class, "_dataSource", dbPartitionDataSource);
		ReflectionTestUtil.setFieldValue(
			InfrastructureUtil.class, "_dataSource",
			_lazyConnectionDataSourceProxy);

		_db.runSQL(
			"create schema " + _getSchemaName(_COMPANY_ID) +
				" character set utf8");
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		_db.runSQL("drop schema " + _getSchemaName(_COMPANY_ID));

		DataAccess.cleanUp(_connection);

		ReflectionTestUtil.setFieldValue(
			DBInitUtil.class, "_dataSource", _currentDataSource);
		ReflectionTestUtil.setFieldValue(
			DBPartitionUtil.class, "_DATABASE_PARTITION_ENABLED",
			_currentDatabasePartitionEnabledValue);
		ReflectionTestUtil.setFieldValue(
			DBPartitionUtil.class, "_DATABASE_PARTITION_INSTANCE_ID",
			_currentDatabasePartitionInstanceIdValue);

		_lazyConnectionDataSourceProxy.setTargetDataSource(_currentDataSource);

		ReflectionTestUtil.setFieldValue(
			InfrastructureUtil.class, "_dataSource",
			_lazyConnectionDataSourceProxy);
	}

	@After
	public void tearDown() throws Exception {
		if (_company != null) {
			ReflectionTestUtil.setFieldValue(
				DBPartitionUtil.class, "_DATABASE_PARTITION_ENABLED",
				_currentDatabasePartitionEnabledValue);

			_companyLocalService.deleteCompany(_company);

			ReflectionTestUtil.setFieldValue(
				DBPartitionUtil.class, "_DATABASE_PARTITION_ENABLED", true);

			_db.runSQL(
				"drop schema " + _getSchemaName(_company.getCompanyId()));
		}

		try (Statement statement = _connection.createStatement()) {
			statement.execute("use " + _defaultSchemaName);
		}
	}

	@Test
	public void testAccessCompanyByCompanyThreadLocal() throws SQLException {
		try (SafeClosable safeClosable =
				CompanyThreadLocal.setInitializingCompanyId(_COMPANY_ID);
			Connection connection = DataAccess.getConnection();
			Statement statement = connection.createStatement()) {

			statement.executeUpdate(
				StringBundler.concat(
					"create table ", _getSchemaName(_COMPANY_ID), ".TestTable ",
					"(testColumn int)"));

			statement.execute("select 1 from TestTable");

			statement.execute("use " + _defaultSchemaName);
		}
	}

	@Test
	public void testAccessDefaultCompanyByCompanyThreadLocal()
		throws SQLException {

		long currentCompanyId = CompanyThreadLocal.getCompanyId();

		CompanyThreadLocal.setCompanyId(_portal.getDefaultCompanyId());

		try (Connection connection = DataAccess.getConnection();
			Statement statement = connection.createStatement()) {

			statement.execute("select 1 from CompanyInfo");

			statement.execute("use " + _defaultSchemaName);
		}
		finally {
			CompanyThreadLocal.setCompanyId(currentCompanyId);
		}
	}

	@Test
	public void testAddDBPartition() throws Exception {
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

			DBPartitionUtil.addDBPartition(_COMPANY_ID);

			try (Statement statement = _connection.createStatement()) {
				statement.execute(
					"select 1 from " + _getSchemaName(_COMPANY_ID) +
						".CompanyInfo");
			}
		}
		finally {
			ReflectionTestUtil.setFieldValue(
				CurrentConnectionUtil.class, "_currentConnection",
				defaultCurrentConnection);
		}
	}

	@Test
	public void testAddDefaultDBPartition() throws PortalException {
		Assert.assertFalse(
			DBPartitionUtil.addDBPartition(_portal.getDefaultCompanyId()));
	}

	@Test
	public void testReceiveMessage() throws Exception {
		_company = CompanyTestUtil.addCompany();

		TestMessageListener testMessageListener = new TestMessageListener();

		MessageListener wrapMessageListener =
			DBPartitionUtil.wrapMessageListener(testMessageListener);

		wrapMessageListener.receive(new Message());

		Assert.assertArrayEquals(
			_getActiveCompanyIds(),
			testMessageListener.getThreadLocalCompanyIds());
	}

	@Test
	public void testReceiveMessageWithCompanyId() throws Exception {
		_company = CompanyTestUtil.addCompany();

		TestMessageListener testMessageListener = new TestMessageListener();

		MessageListener wrapMessageListener =
			DBPartitionUtil.wrapMessageListener(testMessageListener);

		Message message = new Message();

		message.put("companyId", _COMPANY_ID);

		wrapMessageListener.receive(message);

		Assert.assertArrayEquals(
			new Long[] {_COMPANY_ID},
			testMessageListener.getMessageCompanyIds());
	}

	private static Long[] _getActiveCompanyIds() {
		List<Company> companies = _companyLocalService.getCompanies(false);

		Set<Long> companyIds = new TreeSet<>();

		for (Company company : companies) {
			if (company.isActive()) {
				companyIds.add(company.getCompanyId());
			}
		}

		return companyIds.toArray(new Long[0]);
	}

	private static String _getSchemaName(long companyId) {
		return _DB_PARTITION_INSTANCE_ID + StringPool.UNDERLINE + companyId;
	}

	private static final long _COMPANY_ID = 1L;

	private static final String _DB_PARTITION_INSTANCE_ID =
		"dbPartitionUtilTest";

	private static Company _company;

	@Inject
	private static CompanyLocalService _companyLocalService;

	private static Connection _connection;
	private static boolean _currentDatabasePartitionEnabledValue;
	private static String _currentDatabasePartitionInstanceIdValue;
	private static DataSource _currentDataSource;
	private static DB _db;
	private static String _defaultSchemaName;
	private static LazyConnectionDataSourceProxy _lazyConnectionDataSourceProxy;

	@Inject
	private static Portal _portal;

	private class TestMessageListener extends BaseMessageListener {

		public Long[] getMessageCompanyIds() {
			return _messageCompanyIds.toArray(new Long[0]);
		}

		public Long[] getThreadLocalCompanyIds() {
			return _threadLocalCompanyIds.toArray(new Long[0]);
		}

		@Override
		protected void doReceive(Message message) {
			_messageCompanyIds.add(message.getLong("companyId"));
			_threadLocalCompanyIds.add(CompanyThreadLocal.getCompanyId());
		}

		private final Set<Long> _messageCompanyIds = new TreeSet<>();
		private final Set<Long> _threadLocalCompanyIds = new TreeSet<>();

	}

}