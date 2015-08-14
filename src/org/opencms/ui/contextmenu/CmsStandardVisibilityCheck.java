/*
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (c) Alkacon Software GmbH (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.ui.contextmenu;

import static org.opencms.ui.contextmenu.CmsVisibilityCheckFlag.deleted;
import static org.opencms.ui.contextmenu.CmsVisibilityCheckFlag.inproject;
import static org.opencms.ui.contextmenu.CmsVisibilityCheckFlag.notdeleted;
import static org.opencms.ui.contextmenu.CmsVisibilityCheckFlag.notnew;
import static org.opencms.ui.contextmenu.CmsVisibilityCheckFlag.notonline;
import static org.opencms.ui.contextmenu.CmsVisibilityCheckFlag.notunchangedfile;
import static org.opencms.ui.contextmenu.CmsVisibilityCheckFlag.roleeditor;
import static org.opencms.ui.contextmenu.CmsVisibilityCheckFlag.rolewpuser;
import static org.opencms.ui.contextmenu.CmsVisibilityCheckFlag.writepermisssion;
import static org.opencms.workplace.explorer.menu.CmsMenuItemVisibilityMode.VISIBILITY_ACTIVE;
import static org.opencms.workplace.explorer.menu.CmsMenuItemVisibilityMode.VISIBILITY_INVISIBLE;

import org.opencms.file.CmsObject;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.main.CmsException;
import org.opencms.main.OpenCms;
import org.opencms.security.CmsPermissionSet;
import org.opencms.security.CmsRole;
import org.opencms.workplace.explorer.CmsResourceUtil;
import org.opencms.workplace.explorer.menu.CmsMenuItemVisibilityMode;
import org.opencms.workplace.explorer.menu.Messages;

import java.util.Set;

import com.google.common.collect.Sets;

/**
 * Standard visibility check implementation.<p>
 *
 * Instances of this class are configured with a set of flags, each of which corresponds to a check to perform which
 * may cause the context menu item to be hidden or deactivated.<p>
 */
public final class CmsStandardVisibilityCheck extends A_CmsSimpleVisibilityCheck {

    /** Default visibility check for 'edit-like' operations on resources. */
    public static final CmsStandardVisibilityCheck DEFAULT = new CmsStandardVisibilityCheck(
        roleeditor,
        notonline,
        notdeleted,
        writepermisssion);

    /** Visibility check for the undo function. */
    public static final CmsStandardVisibilityCheck UNDO = new CmsStandardVisibilityCheck(
        notunchangedfile,
        notnew,
        roleeditor,
        notonline,
        notdeleted,
        writepermisssion);

    /** Visibility check for undelete option. */
    public static final CmsStandardVisibilityCheck UNDELETE = new CmsStandardVisibilityCheck(
        roleeditor,
        notonline,
        deleted,
        writepermisssion);

    /** The set of flags. */
    private Set<CmsVisibilityCheckFlag> m_flags = Sets.newHashSet();

    /**
     * Creates a new instance using the given flags.<p>
     *
     * Note that the order of the flags does not matter; the checks corresponding to the flags are performed in a fixed order.
     *
     * @param flags the flags indicating which checks to perform
     */
    public CmsStandardVisibilityCheck(CmsVisibilityCheckFlag... flags) {
        for (CmsVisibilityCheckFlag flag : flags) {
            m_flags.add(flag);
        }
    }

    /**
     * Helper method to make checking for a flag very short (character count).<p>
     *
     * @param flag the flag to check
     *
     * @return true if this instance was configured with the given flag
     */
    public boolean flag(CmsVisibilityCheckFlag flag) {

        return m_flags.contains(flag);
    }

    /**
     * @see org.opencms.ui.contextmenu.A_CmsSimpleVisibilityCheck#getSingleVisibility(org.opencms.file.CmsObject, org.opencms.file.CmsResource)
     */
    @Override
    public CmsMenuItemVisibilityMode getSingleVisibility(CmsObject cms, CmsResource resource) {

        CmsResourceUtil resUtil = new CmsResourceUtil(cms, resource);

        if (flag(roleeditor) && !OpenCms.getRoleManager().hasRole(cms, CmsRole.EDITOR)) {
            return VISIBILITY_INVISIBLE;
        }

        if (flag(rolewpuser) && !OpenCms.getRoleManager().hasRole(cms, CmsRole.WORKPLACE_USER)) {
            return VISIBILITY_INVISIBLE;
        }

        if (flag(notonline) && cms.getRequestContext().getCurrentProject().isOnlineProject()) {
            return VISIBILITY_INVISIBLE;
        }

        if (flag(notunchangedfile) && resource.isFile() && resUtil.getResource().getState().isUnchanged()) {
            return VISIBILITY_INVISIBLE;
        }

        if (flag(notnew) && resource.getState().isNew()) {
            CmsMenuItemVisibilityMode.VISIBILITY_INACTIVE.addMessageKey(
                Messages.GUI_CONTEXTMENU_TITLE_INACTIVE_NEW_UNCHANGED_0);
        }

        if (flag(inproject) && !resUtil.isInsideProject() && !resUtil.getProjectState().isLockedForPublishing()) {
            return VISIBILITY_INVISIBLE;
        }

        if (flag(writepermisssion)) {
            try {
                if (!resUtil.isEditable()
                    || !cms.hasPermissions(
                        resUtil.getResource(),
                        CmsPermissionSet.ACCESS_WRITE,
                        false,
                        CmsResourceFilter.ALL)) {
                    return CmsMenuItemVisibilityMode.VISIBILITY_INACTIVE.addMessageKey(
                        Messages.GUI_CONTEXTMENU_TITLE_INACTIVE_PERM_WRITE_0);
                }
            } catch (CmsException e) {
                // error checking permissions, disable entry completely
                return CmsMenuItemVisibilityMode.VISIBILITY_INVISIBLE;
            }
        }

        if (flag(notdeleted) && resUtil.getResource().getState().isDeleted()) {
            return CmsMenuItemVisibilityMode.VISIBILITY_INACTIVE.addMessageKey(
                Messages.GUI_CONTEXTMENU_TITLE_INACTIVE_DELETED_0);
        }

        if (flag(deleted) && !resource.getState().isDeleted()) {
            return CmsMenuItemVisibilityMode.VISIBILITY_INVISIBLE;
        }

        return VISIBILITY_ACTIVE;
    }
}