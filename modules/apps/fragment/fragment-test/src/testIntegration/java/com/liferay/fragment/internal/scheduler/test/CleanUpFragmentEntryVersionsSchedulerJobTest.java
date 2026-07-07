/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.fragment.internal.scheduler.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.change.tracking.constants.CTConstants;
import com.liferay.fragment.configuration.FragmentEntryVersionConfiguration;
import com.liferay.fragment.model.FragmentEntry;
import com.liferay.fragment.test.util.FragmentEntryVersionTestUtil;
import com.liferay.petra.function.UnsafeRunnable;
import com.liferay.portal.configuration.test.util.CompanyConfigurationTemporarySwapper;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.scheduler.SchedulerJobConfiguration;
import com.liferay.portal.kernel.test.TestInfo;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.util.HashMapDictionaryBuilder;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

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
		new LiferayIntegrationTestRule();

	@Before
	public void setUp() throws Exception {
		_group = GroupTestUtil.addGroup();
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
				FragmentEntryVersionTestUtil.addFragmentEntry(
					_group.getGroupId());

			FragmentEntryVersionTestUtil.insertFragmentEntryVersions(
				FragmentEntryVersionTestUtil.MAX_VERSIONS_PER_ENTRY + 1,
				CTConstants.CT_COLLECTION_ID_PRODUCTION, fragmentEntry1);

			FragmentEntry fragmentEntry2 =
				FragmentEntryVersionTestUtil.addFragmentEntry(
					_group.getGroupId());

			FragmentEntryVersionTestUtil.insertFragmentEntryVersions(
				FragmentEntryVersionTestUtil.MAX_VERSIONS_PER_ENTRY - 1,
				CTConstants.CT_COLLECTION_ID_PRODUCTION, fragmentEntry2);

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
					CTConstants.CT_COLLECTION_ID_PRODUCTION, fragmentEntry2));
		}
	}

	@DeleteAfterTestRun
	private Group _group;

	@Inject(
		filter = "component.name=com.liferay.fragment.internal.scheduler.CleanUpFragmentEntryVersionsSchedulerJobConfiguration"
	)
	private SchedulerJobConfiguration _schedulerJobConfiguration;

}