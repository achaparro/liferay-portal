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

package com.liferay.dynamic.data.mapping.internal.upgrade.v3_2_3;

import com.liferay.portal.kernel.dao.jdbc.AutoBatchPreparedStatementUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.StringUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Rodrigo Paulino
 */
public class UpgradeDDMFormFieldValidation extends UpgradeProcess {

	public UpgradeDDMFormFieldValidation(JSONFactory jsonFactory) {
		_jsonFactory = jsonFactory;
	}

	@Override
	protected void doUpgrade() throws Exception {
		try (PreparedStatement ps1 = connection.prepareStatement(
				"select structureId, definition from DDMStructure where " +
					"classNameId = ? ");
			PreparedStatement ps2 =
				AutoBatchPreparedStatementUtil.concurrentAutoBatch(
					connection,
					"update DDMStructure set definition = ? where " +
						"structureId = ?");
			PreparedStatement ps3 = connection.prepareStatement(
				"select structureVersionId, definition from " +
					"DDMStructureVersion where structureId = ?");
			PreparedStatement ps4 =
				AutoBatchPreparedStatementUtil.concurrentAutoBatch(
					connection,
					"update DDMStructureVersion set definition = ? where " +
						"structureVersionId = ?")) {

			ps1.setLong(
				1,
				PortalUtil.getClassNameId(
					"com.liferay.dynamic.data.mapping.model.DDMFormInstance"));

			try (ResultSet rs = ps1.executeQuery()) {
				while (rs.next()) {
					String definition = rs.getString("definition");

					ps2.setString(1, _upgradeDefinition(definition));

					long structureId = rs.getLong("structureId");

					ps2.setLong(2, structureId);

					ps2.addBatch();

					ps3.setLong(1, structureId);

					try (ResultSet rs2 = ps3.executeQuery()) {
						while (rs2.next()) {
							definition = rs2.getString("definition");

							ps4.setString(1, _upgradeDefinition(definition));

							long structureVersionId = rs2.getLong(
								"structureVersionId");

							ps4.setLong(2, structureVersionId);

							ps4.addBatch();
						}
					}
				}
			}

			ps2.executeBatch();

			ps4.executeBatch();
		}
	}

	private String _replaceName(
		String expression, String fieldName, String regex) {

		Pattern pattern = Pattern.compile(regex);

		Matcher matcher = pattern.matcher(expression);

		matcher.find();

		String oldFieldName = matcher.group(1);

		return StringUtil.replace(expression, oldFieldName, fieldName);
	}

	private String _upgradeDefinition(String definition)
		throws PortalException {

		JSONObject jsonObject = _jsonFactory.createJSONObject(definition);

		_upgradeFields(jsonObject.getJSONArray("fields"));

		return jsonObject.toJSONString();
	}

	private String _upgradeExpression(String expression, String fieldName) {
		if (expression.matches("NOT\\(contains\\((.+), \"(.*)\"\\)\\)")) {
			expression = _replaceName(
				expression, fieldName, "NOT\\(contains\\((.+), \"(.*)\"\\)\\)");
		}
		else if (expression.matches("contains\\((.+), \"(.*)\"\\)")) {
			expression = _replaceName(
				expression, fieldName, "contains\\((.+), \"(.*)\"\\)");
		}
		else if (expression.matches("isURL\\((.+)\\)")) {
			expression = _replaceName(expression, fieldName, "isURL\\((.+)\\)");
		}

		if (expression.matches("isEmailAddress\\((.+)\\)")) {
			expression = _replaceName(
				expression, fieldName, "isEmailAddress\\((.+)\\)");
		}

		if (expression.matches("match\\((.+), \"(.*)\"\\)")) {
			expression = _replaceName(
				expression, fieldName, "match\\((.+), \"(.*)\"\\)");
		}

		if (expression.matches("(.+)<(\\d+\\.?\\d*)?")) {
			expression = _replaceName(
				expression, fieldName, "(.+)<(\\d+\\.?\\d*)?");
		}

		if (expression.matches("(.+)<=(\\d+\\.?\\d*)?")) {
			expression = _replaceName(
				expression, fieldName, "(.+)<=(\\d+\\.?\\d*)?");
		}

		if (expression.matches("(.+)==(\\d+\\.?\\d*)?")) {
			expression = _replaceName(
				expression, fieldName, "(.+)==(\\d+\\.?\\d*)?");
		}

		if (expression.matches("(.+)>(\\d+\\.?\\d*)?")) {
			expression = _replaceName(
				expression, fieldName, "(.+)>(\\d+\\.?\\d*)?");
		}

		if (expression.matches("(.+)>=(\\d+\\.?\\d*)?")) {
			expression = _replaceName(
				expression, fieldName, "(.+)>=(\\d+\\.?\\d*)?");
		}

		return expression;
	}

	private void _upgradeFields(JSONArray fieldsJSONArray) {
		for (int i = 0; i < fieldsJSONArray.length(); i++) {
			JSONObject jsonObject = fieldsJSONArray.getJSONObject(i);

			String name = jsonObject.getString("name");

			JSONObject validationJSONObject = jsonObject.getJSONObject(
				"validation");

			if (validationJSONObject != null) {
				JSONObject expressionJSONObject =
					validationJSONObject.getJSONObject("expression");

				String value = expressionJSONObject.getString("value");

				expressionJSONObject.put(
					"value", _upgradeExpression(value, name));
			}

			JSONArray nestedFieldsJSONArray = jsonObject.getJSONArray(
				"nestedFields");

			if (nestedFieldsJSONArray != null) {
				_upgradeFields(nestedFieldsJSONArray);
			}
		}
	}

	private final JSONFactory _jsonFactory;

}