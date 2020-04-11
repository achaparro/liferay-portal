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

package com.liferay.portal.kernel.messaging;

import com.liferay.portal.kernel.model.CompanyConstants;
import com.liferay.portal.kernel.util.PortalUtil;

/**
 * @author Alberto Chaparro
 */
public abstract class BaseGlobalMessageListener implements MessageListener {

	@Override
	public void receive(Message message) throws MessageListenerException {
		long companyId = message.getLong("companyId");

		if ((companyId != CompanyConstants.SYSTEM) &&
			(PortalUtil.getDefaultCompanyId() != companyId)) {

			return;
		}

		try {
			doReceive(message);
		}
		catch (MessageListenerException messageListenerException) {
			throw messageListenerException;
		}
		catch (Exception exception) {
			throw new MessageListenerException(exception);
		}
	}

	protected abstract void doReceive(Message message) throws Exception;

}