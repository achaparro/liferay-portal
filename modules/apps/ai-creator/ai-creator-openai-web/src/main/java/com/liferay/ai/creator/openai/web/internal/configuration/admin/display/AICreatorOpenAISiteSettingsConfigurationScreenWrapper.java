/**
 * SPDX-FileCopyrightText: (c) 2023 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.ai.creator.openai.web.internal.configuration.admin.display;

import com.liferay.ai.creator.openai.configuration.AICreatorOpenAIGroupConfiguration;
import com.liferay.ai.creator.openai.configuration.manager.AICreatorOpenAIConfigurationManager;
import com.liferay.ai.creator.openai.web.internal.display.context.AICreatorOpenAIGroupConfigurationDisplayContext;
import com.liferay.configuration.admin.display.ConfigurationScreen;
import com.liferay.configuration.admin.display.ConfigurationScreenWrapper;
import com.liferay.configuration.admin.util.ConfigurationFilterStringUtil;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HashMapDictionary;
import com.liferay.portal.kernel.util.HashMapDictionaryBuilder;
import com.liferay.site.settings.configuration.admin.display.SiteSettingsConfigurationScreenContributor;
import com.liferay.site.settings.configuration.admin.display.SiteSettingsConfigurationScreenFactory;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.Serializable;

import java.util.Dictionary;
import java.util.Locale;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Lourdes Fernández Besada
 */
@Component(service = ConfigurationScreen.class)
public class AICreatorOpenAISiteSettingsConfigurationScreenWrapper
	extends ConfigurationScreenWrapper {

	@Override
	protected ConfigurationScreen getConfigurationScreen() {
		return _siteSettingsConfigurationScreenFactory.create(
			new AICreatorOpenAISiteSettingsConfigurationScreenContributor());
	}

	private Dictionary<String, Object> _getConfigurationProperties(Group group)
		throws Exception {

		Configuration[] configurations = _configurationAdmin.listConfigurations(
			ConfigurationFilterStringUtil.getGroupScopedFilterString(
				group.getCompanyId(), group.getGroupId(),
				AICreatorOpenAIGroupConfiguration.class.getName(), null));

		if (ArrayUtil.isEmpty(configurations)) {
			return new HashMapDictionary<>();
		}

		return configurations[0].getProperties();
	}

	@Reference
	private AICreatorOpenAIConfigurationManager
		_aiCreatorOpenAIConfigurationManager;

	@Reference
	private ConfigurationAdmin _configurationAdmin;

	@Reference
	private GroupLocalService _groupLocalService;

	@Reference
	private Language _language;

	@Reference(
		target = "(osgi.web.symbolicname=com.liferay.ai.creator.openai.web)"
	)
	private ServletContext _servletContext;

	@Reference
	private SiteSettingsConfigurationScreenFactory
		_siteSettingsConfigurationScreenFactory;

	private class AICreatorOpenAISiteSettingsConfigurationScreenContributor
		implements SiteSettingsConfigurationScreenContributor {

		@Override
		public Dictionary<String, Object> exportProperties(Serializable scopePK)
			throws Exception {

			Dictionary<String, Object> configurationProperties =
				_getConfigurationProperties(
					_groupLocalService.getGroup((long)scopePK));

			if (configurationProperties.isEmpty()) {
				return configurationProperties;
			}

			return HashMapDictionaryBuilder.<String, Object>put(
				"enableChatGPTToCreateContent",
				GetterUtil.getBoolean(
					configurationProperties.get("enableChatGPTToCreateContent"),
					true)
			).put(
				"enableDALLEToCreateImages",
				GetterUtil.getBoolean(
					configurationProperties.get("enableDALLEToCreateImages"),
					true)
			).build();
		}

		@Override
		public String getCategoryKey() {
			return "ai-creator";
		}

		@Override
		public String getJspPath() {
			return "/configuration/openai_group_configuration.jsp";
		}

		@Override
		public String getKey() {
			return "ai-creator-openai-group-configuration";
		}

		@Override
		public String getName(Locale locale) {
			return _language.get(locale, "openai");
		}

		@Override
		public String getSaveMVCActionCommandName() {
			return "/site_settings/save_group_configuration";
		}

		@Override
		public ServletContext getServletContext() {
			return _servletContext;
		}

		@Override
		public void importProperties(
				Dictionary<String, Object> properties, Serializable scopePK)
			throws Exception {

			Dictionary<String, Object> configurationProperties =
				_getConfigurationProperties(
					_groupLocalService.getGroup((long)scopePK));

			_aiCreatorOpenAIConfigurationManager.
				saveAICreatorOpenAIGroupConfiguration(
					(long)scopePK,
					GetterUtil.getString(
						properties.get("apiKey"),
						GetterUtil.getString(
							configurationProperties.get("apiKey"))),
					GetterUtil.getBoolean(
						properties.get("enableChatGPTToCreateContent"), true),
					GetterUtil.getBoolean(
						properties.get("enableDALLEToCreateImages"), true));
		}

		@Override
		public void setAttributes(
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse) {

			httpServletRequest.setAttribute(
				AICreatorOpenAIGroupConfigurationDisplayContext.class.getName(),
				new AICreatorOpenAIGroupConfigurationDisplayContext(
					_aiCreatorOpenAIConfigurationManager, httpServletRequest));
		}

	}

}