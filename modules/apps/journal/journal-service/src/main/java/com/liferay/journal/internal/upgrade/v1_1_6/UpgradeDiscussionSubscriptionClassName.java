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

package com.liferay.journal.internal.upgrade.v1_1_6;

import com.liferay.comment.upgrade.BaseUpgradeDiscussionSubscriptionClassName;
import com.liferay.journal.model.JournalArticle;
import com.liferay.subscription.service.SubscriptionLocalService;

/**
 * @author Roberto Díaz
 */
public class UpgradeDiscussionSubscriptionClassName
	extends BaseUpgradeDiscussionSubscriptionClassName {

	public UpgradeDiscussionSubscriptionClassName(
		SubscriptionLocalService service) {

		subscriptionLocalService = service;
	}

	@Override
	protected void doUpgrade() throws Exception {
		addSubscriptions();
	}

	@Override
	protected String getClassName() {
		return JournalArticle.class.getName();
	}

}