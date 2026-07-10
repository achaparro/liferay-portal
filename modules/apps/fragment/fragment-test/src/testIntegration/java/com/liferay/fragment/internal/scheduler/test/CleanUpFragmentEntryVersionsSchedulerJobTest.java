/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.fragment.internal.scheduler.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.change.tracking.configuration.CTSettingsConfiguration;
import com.liferay.change.tracking.model.CTCollection;
import com.liferay.change.tracking.service.CTCollectionLocalService;
import com.liferay.change.tracking.service.CTCollectionServiceUtil;
import com.liferay.fragment.configuration.FragmentEntryVersionConfiguration;
import com.liferay.fragment.model.FragmentCollection;
import com.liferay.fragment.model.FragmentEntry;
import com.liferay.fragment.model.FragmentEntryVersion;
import com.liferay.fragment.service.FragmentEntryLocalService;
import com.liferay.fragment.test.util.FragmentEntryTestUtil;
import com.liferay.fragment.test.util.FragmentEntryVersionTestUtil;
import com.liferay.fragment.test.util.FragmentTestUtil;
import com.liferay.petra.function.UnsafeRunnable;
import com.liferay.petra.lang.SafeCloseable;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.configuration.module.configuration.ConfigurationProvider;
import com.liferay.portal.configuration.test.util.CompanyConfigurationTemporarySwapper;
import com.liferay.portal.kernel.change.tracking.CTCollectionThreadLocal;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.scheduler.SchedulerJobConfiguration;
import com.liferay.portal.kernel.test.TestInfo;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.util.HashMapDictionaryBuilder;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.test.log.LogCapture;
import com.liferay.portal.test.log.LogEntry;
import com.liferay.portal.test.log.LoggerTestUtil;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.test.rule.PermissionCheckerMethodTestRule;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Georgel Pop
 */
@RunWith(Arquillian.class)
public class CleanUpFragmentEntryVersionsSchedulerJobTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new LiferayIntegrationTestRule(),
			PermissionCheckerMethodTestRule.INSTANCE);

	@Before
	public void setUp() throws Exception {
		_group = GroupTestUtil.addGroup();

		_fragmentCollection = FragmentTestUtil.addFragmentCollection(
			_group.getGroupId());

		FragmentEntryVersionConfiguration fragmentEntryVersionConfiguration =
			_configurationProvider.getCompanyConfiguration(
				FragmentEntryVersionConfiguration.class,
				TestPropsValues.getCompanyId());

		_maximumVersionsPerEntry =
			fragmentEntryVersionConfiguration.maximumVersionsPerEntry();
	}

	@Test
	@TestInfo("LPD-75909")
	public void testCleanUpFragmentEntryVersions() throws Exception {
		_testCleanUpFragmentEntryVersions(0, _maximumVersionsPerEntry + 1);
		_testCleanUpFragmentEntryVersions(
			_maximumVersionsPerEntry, _maximumVersionsPerEntry);
		_testCleanUpFragmentEntryVersions(
			_maximumVersionsPerEntry, _maximumVersionsPerEntry + 1);
	}

	@Test
	@TestInfo("LPD-75909")
	public void testCleanUpFragmentEntryVersionsWhenPublishingCTCollection()
		throws Exception {

		try (CompanyConfigurationTemporarySwapper
				ctSettingsConfigurationTemporarySwapper =
					new CompanyConfigurationTemporarySwapper(
						TestPropsValues.getCompanyId(),
						CTSettingsConfiguration.class.getName(),
						HashMapDictionaryBuilder.<String, Object>put(
							"enabled", true
						).build())) {

			FragmentEntry fragmentEntry =
				FragmentEntryTestUtil.addFragmentEntry(
					_fragmentCollection.getFragmentCollectionId());

			FragmentEntryVersionTestUtil.insertFragmentEntryVersions(
				_maximumVersionsPerEntry + 1,
				CTCollectionThreadLocal.CT_COLLECTION_ID_PRODUCTION,
				fragmentEntry);

			CTCollection ctCollection =
				_ctCollectionLocalService.addCTCollection(
					null, TestPropsValues.getCompanyId(),
					TestPropsValues.getUserId(), 0,
					RandomTestUtil.randomString(),
					RandomTestUtil.randomString());

			try (SafeCloseable safeCloseable =
					CTCollectionThreadLocal.setCTCollectionIdWithSafeCloseable(
						ctCollection.getCtCollectionId())) {

				_fragmentEntryLocalService.updateFragmentEntry(
					TestPropsValues.getUserId(),
					fragmentEntry.getFragmentEntryId(),
					fragmentEntry.getFragmentCollectionId(),
					fragmentEntry.getName(), StringPool.BLANK,
					RandomTestUtil.randomString(), StringPool.BLANK, false,
					StringPool.BLANK, StringPool.BLANK, 0, false,
					StringPool.BLANK, WorkflowConstants.STATUS_APPROVED);
			}

			int fragmentEntryVersionsCount =
				FragmentEntryVersionTestUtil.getFragmentEntryVersionsCount(
					CTCollectionThreadLocal.CT_COLLECTION_ID_PRODUCTION,
					fragmentEntry);
			int ctCollectionFragmentEntryVersionsCount =
				FragmentEntryVersionTestUtil.getFragmentEntryVersionsCount(
					ctCollection.getCtCollectionId(), fragmentEntry);

			UnsafeRunnable<Exception> jobExecutorUnsafeRunnable =
				_schedulerJobConfiguration.getJobExecutorUnsafeRunnable();

			jobExecutorUnsafeRunnable.run();

			Assert.assertTrue(
				fragmentEntryVersionsCount > _maximumVersionsPerEntry);

			Assert.assertEquals(
				_maximumVersionsPerEntry,
				FragmentEntryVersionTestUtil.getFragmentEntryVersionsCount(
					CTCollectionThreadLocal.CT_COLLECTION_ID_PRODUCTION,
					fragmentEntry));
			Assert.assertEquals(
				ctCollectionFragmentEntryVersionsCount,
				FragmentEntryVersionTestUtil.getFragmentEntryVersionsCount(
					ctCollection.getCtCollectionId(), fragmentEntry));

			try (LogCapture logCapture = LoggerTestUtil.configureLog4JLogger(
					"com.liferay.portal.background.task.internal.messaging." +
						"BackgroundTaskMessageListener",
					LoggerTestUtil.ERROR)) {

				CTCollectionServiceUtil.publishCTCollection(
					TestPropsValues.getUserId(),
					ctCollection.getCtCollectionId());

				List<LogEntry> logEntries = logCapture.getLogEntries();

				Assert.assertEquals(
					logEntries.toString(), 0, logEntries.size());
			}
		}
	}

	private void _testCleanUpFragmentEntryVersions(
			int maximumVersionsPerEntry, int fragmentEntryVersionsCount)
		throws Exception {

		try (CompanyConfigurationTemporarySwapper
				companyConfigurationTemporarySwapper =
					new CompanyConfigurationTemporarySwapper(
						TestPropsValues.getCompanyId(),
						FragmentEntryVersionConfiguration.class.getName(),
						HashMapDictionaryBuilder.<String, Object>put(
							"maximumVersionsPerEntry", maximumVersionsPerEntry
						).build())) {

			FragmentEntry fragmentEntry =
				FragmentEntryTestUtil.addFragmentEntry(
					_fragmentCollection.getFragmentCollectionId());

			FragmentEntryVersionTestUtil.insertFragmentEntryVersions(
				fragmentEntryVersionsCount - 1,
				CTCollectionThreadLocal.CT_COLLECTION_ID_PRODUCTION,
				fragmentEntry);

			List<FragmentEntryVersion> fragmentEntryVersions =
				_fragmentEntryLocalService.getVersions(fragmentEntry);

			UnsafeRunnable<Exception> jobExecutorUnsafeRunnable =
				_schedulerJobConfiguration.getJobExecutorUnsafeRunnable();

			jobExecutorUnsafeRunnable.run();

			int retainedCount = fragmentEntryVersions.size();

			if (maximumVersionsPerEntry > 0) {
				retainedCount = Math.min(
					maximumVersionsPerEntry, retainedCount);
			}

			Assert.assertEquals(
				fragmentEntryVersions.subList(0, retainedCount),
				_fragmentEntryLocalService.getVersions(fragmentEntry));
		}
	}

	@Inject
	private ConfigurationProvider _configurationProvider;

	@Inject
	private CTCollectionLocalService _ctCollectionLocalService;

	private FragmentCollection _fragmentCollection;

	@Inject
	private FragmentEntryLocalService _fragmentEntryLocalService;

	@DeleteAfterTestRun
	private Group _group;

	private int _maximumVersionsPerEntry;

	@Inject(
		filter = "component.name=com.liferay.fragment.internal.scheduler.CleanUpFragmentEntryVersionsSchedulerJobConfiguration"
	)
	private SchedulerJobConfiguration _schedulerJobConfiguration;

}