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

package com.liferay.document.library.google.docs.internal.upgrade.v1_0_0;

import com.liferay.document.library.google.drive.configuration.DLGoogleDriveCompanyConfiguration;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.module.configuration.ConfigurationProvider;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.HashMapDictionary;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.PrefsProps;
import com.liferay.portal.kernel.util.Validator;

import javax.portlet.PortletPreferences;

/**
 * @author Alejandro Tardín
 */
public class UpgradePortletPreferences extends UpgradeProcess {

	public UpgradePortletPreferences(
		ConfigurationProvider configurationProvider, PrefsProps prefsProps) {

		_configurationProvider = configurationProvider;
		_prefsProps = prefsProps;
	}

	@Override
	protected void doUpgrade() throws Exception {
		PortalUtil.runCompanies(
			company -> {
				DLGoogleDriveCompanyConfiguration
					dlGoogleDriveCompanyConfiguration =
						_configurationProvider.getCompanyConfiguration(
							DLGoogleDriveCompanyConfiguration.class,
							company.getCompanyId());

				PortletPreferences portletPreferences =
					_prefsProps.getPreferences(company.getCompanyId());

				String apiKey = portletPreferences.getValue(
					"googleAppsAPIKey", StringPool.BLANK);
				String clientId = portletPreferences.getValue(
					"googleClientId", StringPool.BLANK);

				if (Validator.isNotNull(apiKey) &&
					Validator.isNotNull(clientId) &&
					Validator.isNull(
						dlGoogleDriveCompanyConfiguration.clientId()) &&
					Validator.isNull(
						dlGoogleDriveCompanyConfiguration.clientSecret())) {

					_configurationProvider.saveCompanyConfiguration(
						DLGoogleDriveCompanyConfiguration.class,
						company.getCompanyId(),
						new HashMapDictionary<String, Object>() {
							{
								put("clientId", clientId);
								put("pickerAPIKey", apiKey);
							}
						});
				}
			});
	}

	private final ConfigurationProvider _configurationProvider;
	private final PrefsProps _prefsProps;

}