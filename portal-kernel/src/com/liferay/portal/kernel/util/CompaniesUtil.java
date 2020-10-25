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
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.service.CompanyLocalServiceUtil;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * @author Alberto Chaparro
 * @author Carlos Sierra Andrés
 */
public class CompaniesUtil {

	public static <E extends Exception> void run(
			UnsafeConsumer<Company, E> unsafeConsumer)
		throws E {

		run(unsafeConsumer, CompanyLocalServiceUtil.getCompanies());
	}

	public static <E extends Exception> void run(
		UnsafeConsumer<Company, E> unsafeConsumer,
		BiConsumer<Company, E> biConsumer) {

		run(unsafeConsumer, biConsumer, CompanyLocalServiceUtil.getCompanies());
	}

	public static <E extends Exception> void run(
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
	public static <E extends Exception> void run(
			UnsafeConsumer<Company, E> unsafeConsumer, List<Company> companies)
		throws E {

		run(
			unsafeConsumer, (__, e) -> ReflectionUtil.throwException(e),
			companies);
	}

	public static <E extends Exception> void runCompanyIds(
			UnsafeConsumer<Long, E> unsafeConsumer)
		throws E {

		runCompanyIds(unsafeConsumer, _getCompanyIds());
	}

	public static <E extends Exception> void runCompanyIds(
		UnsafeConsumer<Long, E> unsafeConsumer,
		BiConsumer<Long, E> biConsumer) {

		runCompanyIds(unsafeConsumer, biConsumer, _getCompanyIds());
	}

	public static <E extends Exception> void runCompanyIds(
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
	public static <E extends Exception> void runCompanyIds(
			UnsafeConsumer<Long, E> unsafeConsumer, long[] companyIds)
		throws E {

		runCompanyIds(
			unsafeConsumer, (__, e) -> ReflectionUtil.throwException(e),
			companyIds);
	}

	@SuppressWarnings("all")
	public static <T, E extends Exception> Stream<T> runCompanyIds(
			UnsafeFunction<Long, T, E> unsafeFunction)
		throws E {

		return runCompanyIds(
			unsafeFunction, (__, e) -> ReflectionUtil.throwException(e),
			Arrays.stream(
				_getCompanyIds()
			).boxed());
	}

	public static <T, E extends Exception> Stream<T> runCompanyIds(
		UnsafeFunction<Long, T, E> unsafeFunction,
		BiConsumer<Long, E> biConsumer) {

		return runCompanyIds(
			unsafeFunction, biConsumer,
			Arrays.stream(
				_getCompanyIds()
			).boxed());
	}

	public static <T, E extends Exception> Stream<T> runCompanyIds(
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

	private static long[] _getCompanyIds() {
		List<Company> companies = CompanyLocalServiceUtil.getCompanies(false);

		Stream<Company> stream = companies.stream();

		return stream.mapToLong(
			Company::getCompanyId
		).toArray();
	}

}