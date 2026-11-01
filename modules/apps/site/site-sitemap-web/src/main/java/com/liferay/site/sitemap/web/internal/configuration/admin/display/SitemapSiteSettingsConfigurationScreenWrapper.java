/**
 * SPDX-FileCopyrightText: (c) 2023 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.site.sitemap.web.internal.configuration.admin.display;

import com.liferay.configuration.admin.display.ConfigurationScreen;
import com.liferay.configuration.admin.display.ConfigurationScreenWrapper;
import com.liferay.configuration.admin.util.ConfigurationFilterStringUtil;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HashMapDictionary;
import com.liferay.portal.kernel.util.HashMapDictionaryBuilder;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.site.configuration.manager.SitemapConfigurationManager;
import com.liferay.site.settings.configuration.admin.display.SiteSettingsConfigurationScreenContributor;
import com.liferay.site.settings.configuration.admin.display.SiteSettingsConfigurationScreenFactory;
import com.liferay.site.sitemap.web.internal.display.context.SitemapGroupConfigurationDisplayContext;

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
public class SitemapSiteSettingsConfigurationScreenWrapper
	extends ConfigurationScreenWrapper {

	@Override
	protected ConfigurationScreen getConfigurationScreen() {
		return _siteSettingsConfigurationScreenFactory.create(
			new SitemapSiteSettingsConfigurationScreenContributor());
	}

	private Dictionary<String, Object> _getConfigurationProperties(Group group)
		throws Exception {

		Configuration[] configurations = _configurationAdmin.listConfigurations(
			ConfigurationFilterStringUtil.getGroupScopedFilterString(
				group.getCompanyId(), group.getGroupId(),
				"com.liferay.site.internal.configuration." +
					"SitemapGroupConfiguration",
				null));

		if (ArrayUtil.isEmpty(configurations)) {
			return new HashMapDictionary<>();
		}

		return configurations[0].getProperties();
	}

	@Reference
	private ConfigurationAdmin _configurationAdmin;

	@Reference
	private GroupLocalService _groupLocalService;

	@Reference
	private Language _language;

	@Reference(target = "(osgi.web.symbolicname=com.liferay.site.sitemap.web)")
	private ServletContext _servletContext;

	@Reference
	private SiteSettingsConfigurationScreenFactory
		_siteSettingsConfigurationScreenFactory;

	@Reference
	private SitemapConfigurationManager _sitemapConfigurationManager;

	private class SitemapSiteSettingsConfigurationScreenContributor
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
				"includeCategories",
				GetterUtil.getBoolean(
					configurationProperties.get("includeCategories"), true)
			).put(
				"includePages",
				GetterUtil.getBoolean(
					configurationProperties.get("includePages"), true)
			).put(
				"includeWebContent",
				GetterUtil.getBoolean(
					configurationProperties.get("includeWebContent"), true)
			).build();
		}

		@Override
		public String getCategoryKey() {
			return "seo";
		}

		@Override
		public String getJspPath() {
			return "/configuration/sitemap_group_configuration.jsp";
		}

		@Override
		public String getKey() {
			return "sitemap-group-configuration";
		}

		@Override
		public String getName(Locale locale) {
			return _language.get(locale, "xml-sitemap");
		}

		@Override
		public String getSaveMVCActionCommandName() {
			return "/site_sitemap/save_group_configuration";
		}

		@Override
		public ServletContext getServletContext() {
			return _servletContext;
		}

		@Override
		public void importProperties(
				Dictionary<String, Object> properties, Serializable scopePK)
			throws Exception {

			_sitemapConfigurationManager.saveSitemapGroupConfiguration(
				(long)scopePK,
				GetterUtil.getBoolean(
					properties.get("includeCategories"), true),
				GetterUtil.getBoolean(properties.get("includePages"), true),
				GetterUtil.getBoolean(
					properties.get("includeWebContent"), true));
		}

		@Override
		public boolean isVisible(Group group) {
			return true;
		}

		@Override
		public void setAttributes(
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse) {

			SiteSettingsConfigurationScreenContributor.super.setAttributes(
				httpServletRequest, httpServletResponse);

			httpServletRequest.setAttribute(
				SitemapGroupConfigurationDisplayContext.class.getName(),
				new SitemapGroupConfigurationDisplayContext(
					_sitemapConfigurationManager,
					(ThemeDisplay)httpServletRequest.getAttribute(
						WebKeys.THEME_DISPLAY)));
		}

	}

}