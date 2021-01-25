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

package com.liferay.commerce.account.internal.upgrade.v3_0_0;

import com.liferay.account.model.AccountEntry;
import com.liferay.account.service.AccountEntryLocalServiceUtil;
import com.liferay.commerce.account.model.CommerceAccount;
import com.liferay.commerce.account.model.impl.CommerceAccountImpl;
import com.liferay.portal.kernel.dao.orm.ActionableDynamicQuery;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.TypedModel;
import com.liferay.portal.kernel.model.WorkflowDefinitionLink;
import com.liferay.portal.kernel.model.WorkflowInstanceLink;
import com.liferay.portal.kernel.service.ClassNameLocalServiceUtil;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;
import com.liferay.portal.kernel.service.ResourceLocalServiceUtil;
import com.liferay.portal.kernel.service.WorkflowDefinitionLinkLocalServiceUtil;
import com.liferay.portal.kernel.service.WorkflowInstanceLinkLocalServiceUtil;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.function.UnaryOperator;

/**
 * @author Drew Brokke
 */
public class CommerceAccountUpgradeProcess extends UpgradeProcess {

	@Override
	protected void doUpgrade() throws Exception {
		String selectCommerceAccountSQL =
			"select * from CommerceAccount order by commerceAccountId asc";

		String deleteCommerceAccountSQL =
			"delete from CommerceAccount where commerceAccountId = ?";

		try (Statement selectStatement = connection.createStatement();
			PreparedStatement deleteStatement = connection.prepareStatement(
				deleteCommerceAccountSQL)) {

			ResultSet rs = selectStatement.executeQuery(
				selectCommerceAccountSQL);

			while (rs.next()) {
				long accountEntryId = rs.getLong("commerceAccountId");

				AccountEntry accountEntry =
					AccountEntryLocalServiceUtil.createAccountEntry(
						accountEntryId);

				long companyId = rs.getLong("companyId");

				accountEntry.setCompanyId(companyId);

				long userId = rs.getLong("userId");

				accountEntry.setUserId(userId);

				accountEntry.setUserName(rs.getString("userName"));
				accountEntry.setCreateDate(rs.getTimestamp("createDate"));
				accountEntry.setModifiedDate(rs.getTimestamp("modifiedDate"));
				accountEntry.setDefaultBillingAddressId(
					rs.getLong("defaultBillingAddressId"));
				accountEntry.setDefaultShippingAddressId(
					rs.getLong("defaultShippingAddressId"));
				accountEntry.setParentAccountEntryId(
					rs.getLong("parentCommerceAccountId"));
				accountEntry.setEmailAddress(rs.getString("email"));
				accountEntry.setLogoId(rs.getLong("logoId"));
				accountEntry.setName(rs.getString("name"));
				accountEntry.setTaxIdNumber(rs.getString("taxId"));
				accountEntry.setType(
					CommerceAccountImpl.toAccountEntryType(rs.getInt("type_")));
				accountEntry.setStatus(
					CommerceAccountImpl.toAccountEntryStatus(
						rs.getBoolean("active_")));

				accountEntry.setExternalReferenceCode(
					rs.getString("externalReferenceCode"));

				AccountEntryLocalServiceUtil.addAccountEntry(accountEntry);

				ResourceLocalServiceUtil.addResources(
					companyId, 0, userId, AccountEntry.class.getName(),
					accountEntryId, false, false, false);

				deleteStatement.setLong(1, accountEntryId);

				deleteStatement.executeUpdate();
			}
		}

		long accountEntryClassNameId = ClassNameLocalServiceUtil.getClassNameId(
			AccountEntry.class);
		long commerceAccountClassNameId =
			ClassNameLocalServiceUtil.getClassNameId(CommerceAccount.class);

		for (String tableName : _COMMERCE_TABLE_NAMES) {
			try (PreparedStatement preparedStatement =
					connection.prepareStatement(
						"update " + tableName +
							" set classNameId = ? where classNameId = ?")) {

				preparedStatement.setLong(1, accountEntryClassNameId);
				preparedStatement.setLong(2, commerceAccountClassNameId);

				preparedStatement.executeUpdate();
			}
		}

		_updateClassNameId(
			GroupLocalServiceUtil.getActionableDynamicQuery(),
			accountEntryClassNameId, commerceAccountClassNameId,
			typedModel -> GroupLocalServiceUtil.updateGroup((Group)typedModel));
		_updateClassNameId(
			WorkflowDefinitionLinkLocalServiceUtil.getActionableDynamicQuery(),
			accountEntryClassNameId, commerceAccountClassNameId,
			typedModel ->
				WorkflowDefinitionLinkLocalServiceUtil.
					updateWorkflowDefinitionLink(
						(WorkflowDefinitionLink)typedModel));
		_updateClassNameId(
			WorkflowInstanceLinkLocalServiceUtil.getActionableDynamicQuery(),
			accountEntryClassNameId, commerceAccountClassNameId,
			typedModel ->
				WorkflowInstanceLinkLocalServiceUtil.updateWorkflowInstanceLink(
					(WorkflowInstanceLink)typedModel));
	}

	private void _updateClassNameId(
			ActionableDynamicQuery actionableDynamicQuery, long newClassNameId,
			long oldClassNameId, UnaryOperator<TypedModel> updateFunction)
		throws Exception {

		actionableDynamicQuery.setAddCriteriaMethod(
			dynamicQuery -> dynamicQuery.add(
				RestrictionsFactoryUtil.eq("classNameId", oldClassNameId)));
		actionableDynamicQuery.setPerformActionMethod(
			(TypedModel typedModel) -> {
				typedModel.setClassNameId(newClassNameId);

				updateFunction.apply(typedModel);
			});

		actionableDynamicQuery.performActions();
	}

	private static final String[] _COMMERCE_TABLE_NAMES = {
		"CommerceAccountGroupRel", "CommerceAddress",
		"CommerceAddressRestriction", "CommerceChannelRel",
		"CommerceDiscountRel", "CommerceNotificationQueueEntry",
		"CommercePriceModifierRel"
	};

}