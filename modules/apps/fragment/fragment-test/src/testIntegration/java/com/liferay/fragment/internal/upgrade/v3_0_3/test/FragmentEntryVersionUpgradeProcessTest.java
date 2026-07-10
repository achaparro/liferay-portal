/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.fragment.internal.upgrade.v3_0_3.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.change.tracking.constants.CTConstants;
import com.liferay.fragment.configuration.FragmentEntryVersionConfiguration;
import com.liferay.fragment.model.FragmentCollection;
import com.liferay.fragment.model.FragmentEntry;
import com.liferay.fragment.test.util.FragmentEntryTestUtil;
import com.liferay.fragment.test.util.FragmentEntryVersionTestUtil;
import com.liferay.fragment.test.util.FragmentTestUtil;
import com.liferay.portal.configuration.module.configuration.ConfigurationProvider;
import com.liferay.portal.configuration.test.util.CompanyConfigurationTemporarySwapper;
import com.liferay.portal.kernel.cache.MultiVMPool;
import com.liferay.portal.kernel.dao.orm.EntityCache;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.test.TestInfo;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.HashMapDictionaryBuilder;
import com.liferay.portal.kernel.version.Version;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.test.rule.PermissionCheckerMethodTestRule;
import com.liferay.portal.upgrade.registry.UpgradeStepRegistrator;
import com.liferay.portal.upgrade.test.util.UpgradeTestUtil;

import java.util.ArrayList;
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
public class FragmentEntryVersionUpgradeProcessTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new LiferayIntegrationTestRule(),
			PermissionCheckerMethodTestRule.INSTANCE);

	@Before
	public void setUp() throws Exception {
		_group = GroupTestUtil.addGroup();
	}

	@Test
	@TestInfo("LPD-75909")
	public void testUpgrade() throws Exception {
		FragmentEntryVersionConfiguration fragmentEntryVersionConfiguration =
			_configurationProvider.getCompanyConfiguration(
				FragmentEntryVersionConfiguration.class,
				TestPropsValues.getCompanyId());

		int maximumVersionsPerEntry =
			fragmentEntryVersionConfiguration.maximumVersionsPerEntry();

		_testUpgrade(
			0, maximumVersionsPerEntry + 1, maximumVersionsPerEntry + 1);
		_testUpgrade(
			maximumVersionsPerEntry, maximumVersionsPerEntry,
			maximumVersionsPerEntry + 1);
		_testUpgrade(
			maximumVersionsPerEntry, maximumVersionsPerEntry + 1,
			maximumVersionsPerEntry);
	}

	private int _getStartIndex(int maximumVersionsPerEntry, int size) {
		if (maximumVersionsPerEntry <= 0) {
			return 0;
		}

		return Math.max(0, size - maximumVersionsPerEntry);
	}

	private void _runUpgrade() throws Exception {
		_entityCache.clearCache();
		_multiVMPool.clear();

		for (UpgradeProcess upgradeProcess :
				UpgradeTestUtil.getUpgradeSteps(
					_upgradeStepRegistrator, new Version(3, 0, 3))) {

			upgradeProcess.upgrade();
		}

		_entityCache.clearCache();
		_multiVMPool.clear();
	}

	private void _testUpgrade(
			int maximumVersionsPerEntry, int ctCollectionVersionsCount,
			int productionVersionsCount)
		throws Exception {

		try (CompanyConfigurationTemporarySwapper
				companyConfigurationTemporarySwapper =
					new CompanyConfigurationTemporarySwapper(
						TestPropsValues.getCompanyId(),
						FragmentEntryVersionConfiguration.class.getName(),
						HashMapDictionaryBuilder.<String, Object>put(
							"maximumVersionsPerEntry", maximumVersionsPerEntry
						).build())) {

			FragmentCollection fragmentCollection =
				FragmentTestUtil.addFragmentCollection(_group.getGroupId());

			FragmentEntry fragmentEntry =
				FragmentEntryTestUtil.addFragmentEntry(
					fragmentCollection.getFragmentCollectionId());

			List<Integer> productionVersions = new ArrayList<>(
				FragmentEntryVersionTestUtil.getVersions(
					CTConstants.CT_COLLECTION_ID_PRODUCTION, fragmentEntry));

			long ctCollectionId = RandomTestUtil.randomLong();
			List<Integer> ctCollectionVersions = new ArrayList<>();

			if (ctCollectionVersionsCount > 0) {
				ctCollectionVersions =
					FragmentEntryVersionTestUtil.insertFragmentEntryVersions(
						ctCollectionVersionsCount, ctCollectionId,
						fragmentEntry);
			}

			productionVersions.addAll(
				FragmentEntryVersionTestUtil.insertFragmentEntryVersions(
					productionVersionsCount - 1,
					CTConstants.CT_COLLECTION_ID_PRODUCTION, fragmentEntry));

			_runUpgrade();

			Assert.assertEquals(
				productionVersions.subList(
					_getStartIndex(
						maximumVersionsPerEntry, productionVersions.size()),
					productionVersions.size()),
				FragmentEntryVersionTestUtil.getVersions(
					CTConstants.CT_COLLECTION_ID_PRODUCTION, fragmentEntry));

			if (ctCollectionVersionsCount > 0) {
				Assert.assertEquals(
					ctCollectionVersions.subList(
						_getStartIndex(
							maximumVersionsPerEntry,
							ctCollectionVersions.size()),
						ctCollectionVersions.size()),
					FragmentEntryVersionTestUtil.getVersions(
						ctCollectionId, fragmentEntry));
			}
		}
	}

	@Inject
	private ConfigurationProvider _configurationProvider;

	@Inject
	private EntityCache _entityCache;

	@DeleteAfterTestRun
	private Group _group;

	@Inject
	private MultiVMPool _multiVMPool;

	@Inject(
		filter = "(&(component.name=com.liferay.fragment.internal.upgrade.registry.FragmentServiceUpgradeStepRegistrator))"
	)
	private UpgradeStepRegistrator _upgradeStepRegistrator;

}