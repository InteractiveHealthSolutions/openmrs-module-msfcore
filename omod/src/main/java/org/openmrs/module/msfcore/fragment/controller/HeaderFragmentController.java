/*
 * The contents of this file are subject to the OpenMRS Public License Version
 * 1.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://license.openmrs.org
 * 
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * 
 * Copyright (C) OpenMRS, LLC. All Rights Reserved.
 */
package org.openmrs.module.msfcore.fragment.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openmrs.api.context.Context;
import org.openmrs.module.appframework.domain.Extension;
import org.openmrs.module.appframework.service.AppFrameworkService;
import org.openmrs.module.appui.AppUiConstants;
import org.openmrs.module.appui.AppUiExtensions;
import org.openmrs.module.msfcore.api.MSFCoreService;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentModel;

/**
 * cloned from
 * https://github.com/openmrs/openmrs-module-appui/blob/master/omod/src/main/
 * java/org/openmrs/module/appui/fragment/controller/HeaderFragmentController.
 * java
 */
public class HeaderFragmentController {

    // RA-592: don't use PrivilegeConstants.VIEW_LOCATIONS
    private static final String GET_LOCATIONS = "Get Locations";
    private static final String VIEW_LOCATIONS = "View Locations";

    public void controller(@SpringBean AppFrameworkService appFrameworkService, FragmentModel fragmentModel,
                    @SpringBean("msfCoreService") MSFCoreService msfCoreService, HttpServletRequest request, HttpServletResponse response,
                    UiUtils ui) throws IOException {
        if (Context.getAuthenticatedUser().isSuperUser() && !request.getRequestURI().contains("msfcore/configuration")
                        && !msfCoreService.configured()) {
            response.sendRedirect(ui.pageLink("msfcore", "configuration"));
        } else {
            try {
                Context.addProxyPrivilege(GET_LOCATIONS);
                Context.addProxyPrivilege(VIEW_LOCATIONS);
                fragmentModel.addAttribute("loginLocations", appFrameworkService.getLoginLocations());

                List<Extension> exts = appFrameworkService.getExtensionsForCurrentUser(AppUiExtensions.HEADER_CONFIG_EXTENSION);
                Extension lowestOrderExtension = getLowestOrderExtenstion(exts);
                Map<String, Object> configSettings = lowestOrderExtension.getExtensionParams();
                fragmentModel.addAttribute("configSettings", configSettings);
                List<Extension> userAccountMenuItems = appFrameworkService
                                .getExtensionsForCurrentUser(AppUiExtensions.HEADER_USER_ACCOUNT_MENU_ITEMS_EXTENSION);
                fragmentModel.addAttribute("userAccountMenuItems", userAccountMenuItems);
            } finally {
                Context.removeProxyPrivilege(GET_LOCATIONS);
                Context.removeProxyPrivilege(VIEW_LOCATIONS);
            }
        }
    }

    public Extension getLowestOrderExtenstion(List<Extension> exts) {
        Extension lowestOrderExtension = exts.size() > 0 ? exts.get(0) : null;
        for (Extension ext : exts) {
            if (lowestOrderExtension.getOrder() > ext.getOrder()) {
                lowestOrderExtension = ext;
            }
        }
        return lowestOrderExtension;
    }

    public void logout(HttpServletRequest request) throws IOException {
        Context.logout();
        request.getSession().invalidate();
        request.getSession().setAttribute(AppUiConstants.SESSION_ATTRIBUTE_MANUAL_LOGOUT, "true");
    }

}
