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

package com.liferay.comment.upgrade;

import com.liferay.message.boards.model.MBDiscussion;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.subscription.model.Subscription;
import com.liferay.subscription.service.SubscriptionLocalService;

import java.util.List;

/**
 * @author Roberto Díaz
 */
public abstract class BaseUpgradeDiscussionSubscriptionClassName
	extends UpgradeProcess {

	protected void addSubscriptions() throws PortalException {
		List<Subscription> subscriptions =
			subscriptionLocalService.getSubscriptions(getClassName());

		for (Subscription subscription : subscriptions) {
			subscriptionLocalService.addSubscription(
				subscription.getUserId(), subscription.getGroupId(),
				MBDiscussion.class.getName() + StringPool.UNDERLINE +
					getClassName(),
				subscription.getClassPK());
		}
	}

	protected void deleteSubscriptions() throws PortalException {
		List<Subscription> subscriptions =
			subscriptionLocalService.getSubscriptions(getClassName());

		for (Subscription subscription : subscriptions) {
			subscriptionLocalService.deleteSubscription(
				subscription.getSubscriptionId());
		}
	}

	protected abstract String getClassName();

	protected SubscriptionLocalService subscriptionLocalService;

}