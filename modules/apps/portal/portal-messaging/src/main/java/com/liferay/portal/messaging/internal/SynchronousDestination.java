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

package com.liferay.portal.messaging.internal;

import com.liferay.petra.lang.CentralizedThreadLocal;
import com.liferay.portal.kernel.cache.thread.local.Lifecycle;
import com.liferay.portal.kernel.cache.thread.local.ThreadLocalCacheManager;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.messaging.MessageListener;

import java.util.Set;
import java.util.concurrent.Future;

/**
 * @author Shuyang Zhou
 */
public class SynchronousDestination extends BaseAsyncDestination {

	@Override
	protected void dispatch(
		Set<MessageListener> messageListeners, final Message message) {

		Thread currentThread = Thread.currentThread();

		Future<?> future = submit(
			() -> {
				try {
					MessageBusThreadLocalUtil.populateThreadLocalsFromMessage(
						message, permissionCheckerFactory, userLocalService);

					for (MessageListener messageListener : messageListeners) {
						messageListener.receive(message);
					}
				}
				finally {
					if (Thread.currentThread() != currentThread) {
						ThreadLocalCacheManager.clearAll(Lifecycle.REQUEST);

						CentralizedThreadLocal.clearShortLivedThreadLocals();
					}
				}

				return null;
			});

		try {
			future.get();
		}
		catch (Exception exception) {
			_log.error("Unable to process message " + message, exception);
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		SynchronousDestination.class);

}