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

package com.liferay.portal.kernel.util;

import com.liferay.petra.function.UnsafeConsumer;
import com.liferay.petra.function.UnsafeFunction;
import com.liferay.petra.reflect.ReflectionUtil;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.service.CompanyLocalServiceUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * @author Alberto Chaparro
 * @author Carlos Sierra Andrés
 */
public class CompaniesUtil {

	@SuppressWarnings("all")
	public static <T, E extends Exception> Stream<T> applyForEachCompanyId(
			UnsafeFunction<Long, T, E> unsafeFunction)
		throws E {

		LongStream longStream = Arrays.stream(_getCompanyIds());

		return applyForEachCompanyId(
			unsafeFunction, (__, e) -> ReflectionUtil.throwException(e),
			longStream.boxed());
	}

	public static <T, E extends Exception> Stream<T> applyForEachCompanyId(
		UnsafeFunction<Long, T, E> unsafeFunction,
		BiConsumer<Long, E> biConsumer) {

		LongStream longStream = Arrays.stream(_getCompanyIds());

		return applyForEachCompanyId(
			unsafeFunction, biConsumer, longStream.boxed());
	}

	public static <T, E extends Exception> Stream<T> applyForEachCompanyId(
		UnsafeFunction<Long, T, E> unsafeFunction,
		BiConsumer<Long, E> biConsumer, Stream<Long> companyIdsStream) {

		long currentCompanyId = CompanyThreadLocal.getCompanyId();

		return companyIdsStream.flatMap(
			companyId -> {
				try {
					CompanyThreadLocal.setCompanyId(companyId);

					return Stream.of(unsafeFunction.apply(companyId));
				}
				catch (Exception exception) {
					biConsumer.accept(companyId, (E)exception);

					return Stream.empty();
				}
				finally {
					CompanyThreadLocal.setCompanyId(currentCompanyId);
				}
			});
	}

	public static <E extends Exception> void forEach(
			UnsafeConsumer<Company, E> unsafeConsumer)
		throws E {

		forEach(unsafeConsumer, CompanyLocalServiceUtil.getCompanies(false));
	}

	public static <E extends Exception> void forEach(
		UnsafeConsumer<Company, E> unsafeConsumer,
		BiConsumer<Company, E> biConsumer) {

		forEach(
			unsafeConsumer, biConsumer,
			CompanyLocalServiceUtil.getCompanies(false));
	}

	public static <E extends Exception> void forEach(
		UnsafeConsumer<Company, E> unsafeConsumer,
		BiConsumer<Company, E> biConsumer, List<Company> companies) {

		long currentCompanyId = CompanyThreadLocal.getCompanyId();

		try {
			for (Company company : companies) {
				CompanyThreadLocal.setCompanyId(company.getCompanyId());

				try {
					unsafeConsumer.accept(company);
				}
				catch (Exception exception) {
					biConsumer.accept(company, (E)exception);
				}
			}
		}
		finally {
			CompanyThreadLocal.setCompanyId(currentCompanyId);
		}
	}

	@SuppressWarnings("all")
	public static <E extends Exception> void forEach(
			UnsafeConsumer<Company, E> unsafeConsumer, List<Company> companies)
		throws E {

		forEach(
			unsafeConsumer, (__, e) -> ReflectionUtil.throwException(e),
			companies);
	}

	public static <E extends Exception> void forEachCompanyId(
			UnsafeConsumer<Long, E> unsafeConsumer)
		throws E {

		forEachCompanyId(unsafeConsumer, _getCompanyIds());
	}

	public static <E extends Exception> void forEachCompanyId(
		UnsafeConsumer<Long, E> unsafeConsumer,
		BiConsumer<Long, E> biConsumer) {

		forEachCompanyId(unsafeConsumer, biConsumer, _getCompanyIds());
	}

	public static <E extends Exception> void forEachCompanyId(
		UnsafeConsumer<Long, E> unsafeConsumer, BiConsumer<Long, E> biConsumer,
		long[] companyIds) {

		long currentCompanyId = CompanyThreadLocal.getCompanyId();

		try {
			for (long companyId : companyIds) {
				CompanyThreadLocal.setCompanyId(companyId);

				try {
					unsafeConsumer.accept(companyId);
				}
				catch (Exception exception) {
					biConsumer.accept(companyId, (E)exception);
				}
			}
		}
		finally {
			CompanyThreadLocal.setCompanyId(currentCompanyId);
		}
	}

	@SuppressWarnings("all")
	public static <E extends Exception> void forEachCompanyId(
			UnsafeConsumer<Long, E> unsafeConsumer, long[] companyIds)
		throws E {

		forEachCompanyId(
			unsafeConsumer, (__, e) -> ReflectionUtil.throwException(e),
			companyIds);
	}

	private static long[] _getCompanyIds() {
		try {
			return PortalUtil.getCompanyIds();
		}
		catch (NullPointerException nullPointerException) {
			return _getCompanyIdsBySQL();
		}
	}

	private static long[] _getCompanyIdsBySQL() {
		List<Long> companyIds = new ArrayList<>();

		try (Connection con = DataAccess.getConnection();
			PreparedStatement ps = con.prepareStatement(
				"select companyId from Company where system_ = ?")) {

			ps.setBoolean(1, false);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					long companyId = rs.getLong("companyId");

					companyIds.add(companyId);
				}
			}
		}
		catch (SQLException sqlException) {
			throw new SystemException(sqlException);
		}

		return ArrayUtil.toArray(companyIds.toArray(new Long[0]));
	}

}