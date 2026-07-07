/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.fragment.internal.upgrade.v3_0_3;

import com.liferay.fragment.configuration.FragmentEntryVersionConfiguration;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.configuration.module.configuration.ConfigurationProvider;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;

/**
 * @author Georgel Pop
 */
public class FragmentEntryVersionUpgradeProcess extends UpgradeProcess {

	public FragmentEntryVersionUpgradeProcess(
		CompanyLocalService companyLocalService,
		ConfigurationProvider configurationProvider) {

		_companyLocalService = companyLocalService;
		_configurationProvider = configurationProvider;
	}

	@Override
	protected void doUpgrade() throws Exception {
		_companyLocalService.forEachCompanyId(
			this::_cleanUpFragmentEntryVersions);
	}

	private void _cleanUpFragmentEntryVersions(long companyId)
		throws Exception {

		FragmentEntryVersionConfiguration fragmentEntryVersionConfiguration =
			_configurationProvider.getCompanyConfiguration(
				FragmentEntryVersionConfiguration.class, companyId);

		int maximumVersionsPerEntry =
			fragmentEntryVersionConfiguration.maximumVersionsPerEntry();

		if (maximumVersionsPerEntry <= 0) {
			return;
		}

		String sql = StringBundler.concat(
			"delete from FragmentEntryVersion where fragmentEntryVersionId in ",
			"(select fragmentEntryVersionId from (select ",
			"fragmentEntryVersionId from FragmentEntryVersion ",
			"FragmentEntryVersion1 where FragmentEntryVersion1.companyId = ",
			companyId, " and (select count(*) from FragmentEntryVersion ",
			"FragmentEntryVersion2 where FragmentEntryVersion2.ctCollectionId ",
			"= FragmentEntryVersion1.ctCollectionId and ",
			"FragmentEntryVersion2.fragmentEntryId = ",
			"FragmentEntryVersion1.fragmentEntryId and ",
			"FragmentEntryVersion2.version >= FragmentEntryVersion1.version) ",
			"> ", maximumVersionsPerEntry, ") tempFragmentEntryVersion)");

		runSQL(sql);
	}

	private final CompanyLocalService _companyLocalService;
	private final ConfigurationProvider _configurationProvider;

}