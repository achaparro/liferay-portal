/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.vulcan.internal.jaxrs.exception.mapper;

import com.liferay.portal.kernel.exception.NoSuchModelException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.servlet.HttpMethods;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.vulcan.jaxrs.exception.mapper.BaseExceptionMapper;
import com.liferay.portal.vulcan.jaxrs.exception.mapper.Problem;

import jakarta.servlet.http.HttpServletRequest;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Providers;

/**
 * Converts any {@code PrincipalException} that carries an external reference
 * code to the same {@code 404} error a {@code NoSuchModelException} with that
 * code produces, so that an entity the caller may not view is
 * indistinguishable from a missing one. Otherwise, converts it to a {@code
 * 404} error in case it is a GET request, or a {@code 403} error for any
 * other request.
 *
 * @author Brian Wing Shun Chan
 * @review
 */
public class PrincipalExceptionMapper
	extends BaseExceptionMapper<PrincipalException> {

	@Override
	public Response toResponse(PrincipalException principalException) {
		String externalReferenceCode =
			principalException.getExternalReferenceCode();

		if (Validator.isNotNull(externalReferenceCode)) {
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

	private static final Log _log = LogFactoryUtil.getLog(
		PrincipalExceptionMapper.class);

	@Context
	private HttpServletRequest _httpServletRequest;

	@Context
	private Providers _providers;

}