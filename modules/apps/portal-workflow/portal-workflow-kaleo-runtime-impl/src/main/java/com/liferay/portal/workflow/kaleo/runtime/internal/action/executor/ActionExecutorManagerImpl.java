/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.workflow.kaleo.runtime.internal.action.executor;

import com.liferay.osgi.service.tracker.collections.map.ServiceReferenceMapperFactory;
import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerMap;
import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerMapFactory;
import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringPool;
import com.liferay.petra.string.StringUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.CompanyConstants;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.util.ClassUtil;
import com.liferay.portal.workflow.kaleo.definition.ActionType;
import com.liferay.portal.workflow.kaleo.definition.ScriptLanguage;
import com.liferay.portal.workflow.kaleo.model.KaleoAction;
import com.liferay.portal.workflow.kaleo.runtime.ExecutionContext;
import com.liferay.portal.workflow.kaleo.runtime.action.ActionExecutorManager;
import com.liferay.portal.workflow.kaleo.runtime.action.executor.ActionExecutor;

import java.util.List;
import java.util.Objects;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 * @author Leonardo Barros
 */
@Component(service = ActionExecutorManager.class)
public class ActionExecutorManagerImpl implements ActionExecutorManager {

	@Override
	public void executeKaleoAction(
			KaleoAction kaleoAction, ExecutionContext executionContext)
		throws PortalException {

		String actionExecutorKey = _getActionExecutorKey(kaleoAction);

		ActionExecutor actionExecutor = null;

		List<ActionExecutor> actionExecutors = _getActionExecutors(
			actionExecutorKey);

		if (actionExecutors != null) {
			if (Objects.equals(
					String.valueOf(ScriptLanguage.JAVA),
					kaleoAction.getScriptLanguage())) {

				String className = kaleoAction.getScript();

				for (ActionExecutor innerActionExecutor : actionExecutors) {
					if (Objects.equals(
							ClassUtil.getClassName(innerActionExecutor),
							className)) {

						actionExecutor = innerActionExecutor;

						break;
					}
				}
			}
			else {
				actionExecutor = actionExecutors.get(0);
			}
		}

		if (actionExecutor == null) {
			throw new PortalException(
				"No action executor for " + actionExecutorKey);
		}

		actionExecutor.execute(kaleoAction, executionContext);
	}

	@Override
	public String[] getFunctionActionExecutorKeys() {
		return TransformUtil.transformToArray(
			_serviceTrackerMap.keySet(),
			key -> {
				if (key.startsWith("function")) {
					List<String> parts = StringUtil.split(key, CharPool.AT);

					long companyId = Long.valueOf(parts.get(1));

					if (companyId == CompanyThreadLocal.getCompanyId()) {
						return parts.get(0);
					}
				}

				return null;
			},
			String.class);
	}

	@Activate
	protected void activate(BundleContext bundleContext) {
		_serviceTrackerMap = ServiceTrackerMapFactory.openMultiValueMap(
			bundleContext, ActionExecutor.class, null,
			ServiceReferenceMapperFactory.create(
				bundleContext,
				(actionExecutor, emitter) -> emitter.emit(
					_encodeKey(
						actionExecutor.getActionExecutorKey(),
						actionExecutor.getCompanyId()))));
	}

	@Deactivate
	protected void deactivate() {
		_serviceTrackerMap.close();
	}

	private String _encodeKey(String actionExecutorKey, long companyId) {
		return actionExecutorKey + StringPool.AT + companyId;
	}

	private String _getActionExecutorKey(KaleoAction kaleoAction) {
		ActionType actionType = ActionType.valueOf(kaleoAction.getType());

		if (Objects.equals(actionType, ActionType.UPDATE_STATUS)) {
			return actionType.name();
		}

		return kaleoAction.getScriptLanguage();
	}

	private List<ActionExecutor> _getActionExecutors(String actionExecutorKey) {
		List<ActionExecutor> actionExecutors = _serviceTrackerMap.getService(
			_encodeKey(actionExecutorKey, CompanyConstants.SYSTEM));

		if (actionExecutors != null) {
			return actionExecutors;
		}

		return _serviceTrackerMap.getService(
			_encodeKey(actionExecutorKey, CompanyThreadLocal.getCompanyId()));
	}

	private ServiceTrackerMap<String, List<ActionExecutor>> _serviceTrackerMap;

}