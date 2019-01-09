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

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.util.UnicodeProperties;

/**
 * This class is used to fix the Group typesettings of Stageable and also
 * Instanceable Portlets after they have received an instanceId.
 *
 * @author Balázs Sáfrány-Kovalik
 */
public class BaseUpgradeInstanceableStagingGroupTypeSettings
	extends BaseUpgradeGroupTypeSettings {

	public BaseUpgradeInstanceableStagingGroupTypeSettings(
		CompanyLocalService companyLocalService,
		GroupLocalService groupLocalService, String portletId) {

		this.companyLocalService = companyLocalService;
		this.groupLocalService = groupLocalService;
		_portletId = portletId;
	}

	@Override
	protected void updateTypeSettings(
		UnicodeProperties typeSettingsProperties, Group group) {

		String stagedPortletId = getStagedPortletId(_portletId);

		StringBundler sb = new StringBundler(2);

		sb.append(stagedPortletId);
		sb.append("_INSTANCE_\\w{12}");

		String regexp = sb.toString();

		String typeSettingsString = typeSettingsProperties.toString();

		typeSettingsString = typeSettingsString.replaceAll(
			regexp, stagedPortletId);

		typeSettingsProperties = new UnicodeProperties();

		typeSettingsProperties.fastLoad(typeSettingsString);

		group.setTypeSettingsProperties(typeSettingsProperties);

		groupLocalService.updateGroup(group);
	}

	private final String _portletId;

}