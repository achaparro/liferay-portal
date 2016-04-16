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

package com.liferay.portal.upgrade.registry;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 * @author Carlos Sierra Andrés
 */
@Component(
	immediate = true,
	property = {"osgi.command.function=queue", "osgi.command.scope=upgrade"},
	service = UpgradeQueue.class
)
public class UpgradeQueue {

	@Activate
	public void activate() {
		_upgradeThread = new UpgradeThread();

		_upgradeThread.start();
	}

	@Deactivate
	public void deactivate() {
		_upgradeThread.cancel();
	}

	public void push(Callable<Void> voidCallable) {
		synchronized (this) {
			_queue.push(voidCallable);
		}
	}

	public void queue() {
		System.out.println("Pending Jobs: " + _queue.size());

		for (Callable<Void> callable : _queue) {
			System.out.println(callable);
		}
	}

	private final BlockingDeque<Callable<Void>> _queue =
		new LinkedBlockingDeque<>();
	private UpgradeThread _upgradeThread;

	private class UpgradeThread extends Thread {

		public UpgradeThread() {
			super("Upgrade Thread");
		}

		public void cancel() {
			_canceled = true;

			interrupt();
		}

		@Override
		public void run() {
			while (!isInterrupted() && !_isCanceled()) {
				try {
					Callable<Void> callable = _queue.pollLast(
						1, TimeUnit.SECONDS);

					if (callable != null) {
						callable.call();
					}
				}
				catch (InterruptedException ie) {
					_canceled = true;
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				finally {
					if (_queue.isEmpty()) {
						synchronized (UpgradeQueue.this) {
							UpgradeQueue.this.notifyAll();
						}
					}
				}
			}
		}

		private boolean _isCanceled() {
			return _canceled;
		}

		private volatile boolean _canceled = false;

	}

}