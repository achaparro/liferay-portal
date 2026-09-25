/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.vulcan.internal.jaxrs.exception.mapper;

import com.liferay.portal.kernel.exception.NoSuchModelException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.ExternalReferenceCodeModel;
import com.liferay.portal.kernel.model.PersistedModel;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.service.PersistedModelLocalService;
import com.liferay.portal.kernel.servlet.HttpMethods;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.vulcan.jaxrs.exception.mapper.BaseExceptionMapper;
import com.liferay.portal.vulcan.jaxrs.exception.mapper.Problem;

import jakarta.servlet.http.HttpServletRequest;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Providers;

import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * Converts a {@code PrincipalException.MustHavePermission} raised by a {@code
 * VIEW} check on an entity the request addresses by external reference code to
 * the same {@code 404} error a {@code NoSuchModelException} with that code
 * produces, so that an entity the caller may not view is indistinguishable
 * from a missing one. Converts any other {@code PrincipalException} to a {@code
 * 404} error in case it is a GET request, or a {@code 403} error for any other
 * request.
 *
 * @author Brian Wing Shun Chan
 * @review
 */
public class PrincipalExceptionMapper
	extends BaseExceptionMapper<PrincipalException> {

	public PrincipalExceptionMapper(BundleContext bundleContext) {
		_bundleContext = bundleContext;
	}

	@Override
	public Response toResponse(PrincipalException principalException) {
		String externalReferenceCode = _getExternalReferenceCode(
			principalException);

		if (externalReferenceCode != null) {
			NoSuchModelException noSuchModelException =
				new NoSuchModelException(principalException);

			noSuchModelException.setExternalReferenceCode(
				externalReferenceCode);

			ExceptionMapper<NoSuchModelException> exceptionMapper =
				_providers.getExceptionMapper(NoSuchModelException.class);

			return exceptionMapper.toResponse(noSuchModelException);
		}

		String method = _httpServletRequest.getMethod();

		if (method.equals(HttpMethods.GET)) {
			ExceptionMapper<NotFoundException> exceptionMapper =
				_providers.getExceptionMapper(NotFoundException.class);

			return exceptionMapper.toResponse(
				new NotFoundException(principalException));
		}

		return super.toResponse(principalException);
	}

	@Override
	protected Problem getProblem(PrincipalException principalException) {
		if (_log.isWarnEnabled()) {
			_log.warn(principalException);
		}

		return new Problem(
			Response.Status.FORBIDDEN,
			LanguageUtil.get(_httpServletRequest.getLocale(), "forbidden"));
	}

	private String _getExternalReferenceCode(
		PrincipalException principalException) {

		if (!(principalException instanceof
				PrincipalException.MustHavePermission)) {

			return null;
		}

		PrincipalException.MustHavePermission mustHavePermission =
			(PrincipalException.MustHavePermission)principalException;

		String[] actionIds = mustHavePermission.actionId;

		if ((actionIds.length != 1) || !ActionKeys.VIEW.equals(actionIds[0]) ||
			(mustHavePermission.resourceId == 0) ||
			Validator.isNull(mustHavePermission.resourceName)) {

			return null;
		}

		String externalReferenceCode = _getExternalReferenceCode(
			mustHavePermission.resourceName, mustHavePermission.resourceId);

		if (Validator.isNull(externalReferenceCode)) {
			return null;
		}

		MultivaluedMap<String, String> pathParameters =
			_uriInfo.getPathParameters();

		for (List<String> values : pathParameters.values()) {
			if (values.contains(externalReferenceCode)) {
				return externalReferenceCode;
			}
		}

		return null;
	}

	private String _getExternalReferenceCode(
		String className, long primaryKey) {

		try {
			for (ServiceReference<PersistedModelLocalService> serviceReference :
					_bundleContext.getServiceReferences(
						PersistedModelLocalService.class,
						"(model.class.name=" + className + ")")) {

				PersistedModelLocalService persistedModelLocalService =
					_bundleContext.getService(serviceReference);

				try {
					PersistedModel persistedModel =
						persistedModelLocalService.getPersistedModel(
							primaryKey);

					if (persistedModel instanceof ExternalReferenceCodeModel) {
						ExternalReferenceCodeModel externalReferenceCodeModel =
							(ExternalReferenceCodeModel)persistedModel;

						return externalReferenceCodeModel.
							getExternalReferenceCode();
					}
				}
				finally {
					_bundleContext.ungetService(serviceReference);
				}
			}
		}
		catch (InvalidSyntaxException | PortalException exception) {
			if (_log.isDebugEnabled()) {
				_log.debug(exception);
			}
		}

		return null;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		PrincipalExceptionMapper.class);

	private final BundleContext _bundleContext;

	@Context
	private HttpServletRequest _httpServletRequest;

	@Context
	private Providers _providers;

	@Context
	private UriInfo _uriInfo;

}