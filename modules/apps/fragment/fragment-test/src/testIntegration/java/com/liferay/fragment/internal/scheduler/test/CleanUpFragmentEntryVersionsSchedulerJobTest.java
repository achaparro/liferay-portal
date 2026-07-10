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
import com.liferay.fragment.service.FragmentEntryLocalService;
import com.liferay.fragment.test.util.FragmentEntryTestUtil;
import com.liferay.fragment.test.util.FragmentEntryVersionTestUtil;
import com.liferay.fragment.test.util.FragmentTestUtil;
import com.liferay.petra.function.UnsafeRunnable;
import com.liferay.petra.lang.SafeCloseable;
import com.liferay.petra.string.StringPool;
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
	}

	@Test
	@TestInfo("LPD-75909")
	public void testCleanUpFragmentEntryVersions() throws Throwable {
		try (CompanyConfigurationTemporarySwapper
				companyConfigurationTemporarySwapper =
					new CompanyConfigurationTemporarySwapper(
						TestPropsValues.getCompanyId(),
						FragmentEntryVersionConfiguration.class.getName(),
						HashMapDictionaryBuilder.<String, Object>put(
							"maximumVersionsPerEntry",
							FragmentEntryVersionTestUtil.MAX_VERSIONS_PER_ENTRY
						).build())) {

			FragmentEntry fragmentEntry1 =
				FragmentEntryTestUtil.addFragmentEntry(
					_fragmentCollection.getFragmentCollectionId());

			FragmentEntryVersionTestUtil.insertFragmentEntryVersions(
				FragmentEntryVersionTestUtil.MAX_VERSIONS_PER_ENTRY + 1,
				CTCollectionThreadLocal.CT_COLLECTION_ID_PRODUCTION,
				fragmentEntry1);

			FragmentEntry fragmentEntry2 =
				FragmentEntryTestUtil.addFragmentEntry(
					_fragmentCollection.getFragmentCollectionId());

			FragmentEntryVersionTestUtil.insertFragmentEntryVersions(
				FragmentEntryVersionTestUtil.MAX_VERSIONS_PER_ENTRY - 1,
				CTCollectionThreadLocal.CT_COLLECTION_ID_PRODUCTION,
				fragmentEntry2);

			List<Integer> versions = FragmentEntryVersionTestUtil.getVersions(
				fragmentEntry1);

			UnsafeRunnable<Exception> jobExecutorUnsafeRunnable =
				_schedulerJobConfiguration.getJobExecutorUnsafeRunnable();

			jobExecutorUnsafeRunnable.run();

			Assert.assertEquals(
				versions.subList(
					versions.size() -
						FragmentEntryVersionTestUtil.MAX_VERSIONS_PER_ENTRY,
					versions.size()),
				FragmentEntryVersionTestUtil.getVersions(fragmentEntry1));
			Assert.assertEquals(
				FragmentEntryVersionTestUtil.MAX_VERSIONS_PER_ENTRY,
				FragmentEntryVersionTestUtil.getFragmentEntryVersionsCount(
					CTCollectionThreadLocal.CT_COLLECTION_ID_PRODUCTION,
					fragmentEntry2));
		}
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
						).build());
			CompanyConfigurationTemporarySwapper
				fragmentEntryVersionConfigurationTemporarySwapper =
					new CompanyConfigurationTemporarySwapper(
						TestPropsValues.getCompanyId(),
						FragmentEntryVersionConfiguration.class.getName(),
						HashMapDictionaryBuilder.<String, Object>put(
							"maximumVersionsPerEntry",
							FragmentEntryVersionTestUtil.MAX_VERSIONS_PER_ENTRY
						).build())) {

			FragmentEntry fragmentEntry =
				FragmentEntryTestUtil.addFragmentEntry(
					_fragmentCollection.getFragmentCollectionId());

			FragmentEntryVersionTestUtil.insertFragmentEntryVersions(
				FragmentEntryVersionTestUtil.MAX_VERSIONS_PER_ENTRY + 1,
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

			int productionCount =
				FragmentEntryVersionTestUtil.getFragmentEntryVersionsCount(
					CTCollectionThreadLocal.CT_COLLECTION_ID_PRODUCTION,
					fragmentEntry);
			int publicationCount =
				FragmentEntryVersionTestUtil.getFragmentEntryVersionsCount(
					ctCollection.getCtCollectionId(), fragmentEntry);

			UnsafeRunnable<Exception> jobExecutorUnsafeRunnable =
				_schedulerJobConfiguration.getJobExecutorUnsafeRunnable();

			jobExecutorUnsafeRunnable.run();

			Assert.assertTrue(
				productionCount >
					FragmentEntryVersionTestUtil.MAX_VERSIONS_PER_ENTRY);

			Assert.assertEquals(
				FragmentEntryVersionTestUtil.MAX_VERSIONS_PER_ENTRY,
				FragmentEntryVersionTestUtil.getFragmentEntryVersionsCount(
					CTCollectionThreadLocal.CT_COLLECTION_ID_PRODUCTION,
					fragmentEntry));
			Assert.assertEquals(
				publicationCount,
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

	@Inject
	private CTCollectionLocalService _ctCollectionLocalService;

	private FragmentCollection _fragmentCollection;

	@Inject
	private FragmentEntryLocalService _fragmentEntryLocalService;

	@DeleteAfterTestRun
	private Group _group;

	@Inject(
		filter = "component.name=com.liferay.fragment.internal.scheduler.CleanUpFragmentEntryVersionsSchedulerJobConfiguration"
	)
	private SchedulerJobConfiguration _schedulerJobConfiguration;

}