/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.search.initializers;

import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.jahia.services.content.nodetypes.initializers.ChoiceListValue;
import org.jahia.services.content.nodetypes.initializers.ModuleChoiceListInitializer;
import org.jahia.services.render.BundleView;
import org.jahia.services.render.RenderService;
import org.jahia.services.render.View;
import org.jahia.services.search.SearchServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.*;

/**
 * Choicelist for dynamic templates
 *
 * @author kevan
 *
 */
public class DynamicTemplatesChoiceListInitalizerImpl implements ModuleChoiceListInitializer {

    private static Logger logger = LoggerFactory.getLogger(DynamicTemplatesChoiceListInitalizerImpl.class);

    public final static String  DYNAMIC_TEMPLATES_SEPARATOR = " -> ";

    /**
     * The choice list initializer unique key.
     */
    private String key;

    @Override
    public List<ChoiceListValue> getChoiceListValues(ExtendedPropertyDefinition declaringPropertyDefinition, String param, List<ChoiceListValue> values, Locale locale, Map<String, Object> context) {
        if (context == null) {
            return new ArrayList<>();
        }

        JCRNodeWrapper node = (JCRNodeWrapper) context.get("contextNode");
        JCRNodeWrapper parentNode = (JCRNodeWrapper) context.get("contextParent");
        JCRSiteNode site = null;

        SortedSet<View> filteredViews = new TreeSet<>();
        SortedSet<View> allViews;

        try {
            if (node != null) {
                site = node.getResolveSite();
            }
            if (site == null && parentNode != null) {
                site = parentNode.getResolveSite();
            }

            // get all the views possible for jnt:searchResults on current site
             allViews = RenderService.getInstance().getViewsSet(
                    NodeTypeRegistry.getInstance().getNodeType("jnt:searchResults"), site, "html");

        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
            return new ArrayList<>();
        }

        if (!allViews.isEmpty()) {
            for (View view : allViews) {
                // only get views for jnt_searchResults
                if ((((BundleView) view).getResource()).startsWith("/jnt_searchResults/")) {

                    // only get visible views for the context
                    HashMap<String, Object> map = new HashMap<String, Object>();
                    fillProperties(map, view.getDefaultProperties());
                    fillProperties(map, view.getProperties());
                    boolean isStudio = site != null && site.getPath().startsWith("/modules");
                    if (isViewVisible(view.getKey(), param, map, isStudio)) {
                        filteredViews.add(view);
                    }
                }
            }
        }

        // build values
        List<ChoiceListValue> vs = new ArrayList<>();
        List<String> providers = SearchServiceImpl.getInstance().getAvailableProviders();
        for (String provider : providers) {
            for (View view : filteredViews) {
                String value = provider + DYNAMIC_TEMPLATES_SEPARATOR + view.getKey();
                vs.add(new ChoiceListValue(value, value));
            }
        }
        Collections.sort(vs);
        return vs;
    }

    private boolean isViewVisible(String viewKey, String param, HashMap<String, Object> viewProperties, boolean isStudio) {
        final Object visible = viewProperties.get(View.VISIBLE_KEY);
        final Object type = viewProperties.get(View.TYPE_KEY);
        return !View.VISIBLE_FALSE.equals(visible)
                && (!View.VISIBLE_STUDIO_ONLY.equals(visible) || isStudio)
                && ((type == null && StringUtils.isEmpty(param)) || param.equals(type))
                && !viewKey.startsWith("wrapper.")
                && !viewKey.contains("hidden.");
    }

    private void fillProperties(HashMap<String, Object> map, Properties properties) {
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            map.put(entry.getKey().toString(), entry.getValue());
        }
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public void setKey(String key) {
        this.key = key;
    }
}
