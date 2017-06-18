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

package com.liferay.calendar.web.internal.upgrade;

import com.liferay.calendar.web.internal.upgrade.v1_0_0.UpgradePortletId;
import com.liferay.calendar.web.internal.upgrade.v1_0_0.UpgradePortletPreferences;
import com.liferay.calendar.web.internal.upgrade.v1_0_2.UpgradeResourceBlockPermissions;
import com.liferay.calendar.web.internal.upgrade.v1_1_1.UpgradeEventsDisplayPortletId;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.service.PortletPreferencesLocalService;
import com.liferay.portal.kernel.service.ResourceActionLocalService;
import com.liferay.portal.kernel.service.ResourceBlockLocalService;
import com.liferay.portal.kernel.service.ResourcePermissionLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.upgrade.DummyUpgradeStep;
import com.liferay.portal.upgrade.registry.UpgradeStepRegistrator;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Marcellus Tavares
 * @author Manuel de la Peña
 */
@Component(
	immediate = true,
	service = {CalendarWebUpgrade.class, UpgradeStepRegistrator.class}
)
public class CalendarWebUpgrade implements UpgradeStepRegistrator {

	@Override
	public void register(Registry registry) {
		registry.register(
			"com.liferay.calendar.web", "0.0.0", "1.0.0",
			new DummyUpgradeStep());

		registry.register(
			"com.liferay.calendar.web", "0.0.1", "1.0.0",
			new UpgradePortletId(), new UpgradePortletPreferences());

		registry.register(
			"com.liferay.calendar.web", "1.0.0", "1.0.1",
			new com.liferay.calendar.web.internal.upgrade.v1_0_1.
				UpgradePortletPreferences());

		registry.register(
			"com.liferay.calendar.web", "1.0.1", "1.0.2",
			new UpgradeResourceBlockPermissions(
				_companyLocalService, _resourceActionLocalService,
				_resourceBlockLocalService, _roleLocalService));

		registry.register(
			"com.liferay.calendar.web", "1.0.2", "1.1.0",
			new com.liferay.calendar.web.internal.upgrade.v1_1_0.
				UpgradePortletId());

		registry.register(
			"com.liferay.calendar.web", "1.1.0", "1.1.1",
			new UpgradeEventsDisplayPortletId(
				_portletPreferencesLocalService,
				_resourcePermissionLocalService));
	}

	@Reference
	private CompanyLocalService _companyLocalService;

	@Reference
	private PortletPreferencesLocalService _portletPreferencesLocalService;

	@Reference
	private ResourceActionLocalService _resourceActionLocalService;

	@Reference
	private ResourceBlockLocalService _resourceBlockLocalService;

	@Reference
	private ResourcePermissionLocalService _resourcePermissionLocalService;

	@Reference
	private RoleLocalService _roleLocalService;

}