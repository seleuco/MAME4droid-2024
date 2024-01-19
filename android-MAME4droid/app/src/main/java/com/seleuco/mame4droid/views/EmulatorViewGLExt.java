/*
 * This file is part of MAME4droid.
 *
 * Copyright (C) 2024 David Valdeita (Seleuco)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 * Linking MAME4droid statically or dynamically with other modules is
 * making a combined work based on MAME4droid. Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 * In addition, as a special exception, the copyright holders of MAME4droid
 * give you permission to combine MAME4droid with free software programs
 * or libraries that are released under the GNU LGPL and with code included
 * in the standard release of MAME under the MAME License (or modified
 * versions of such code, with unchanged license). You may copy and
 * distribute such a system following the terms of the GNU GPL for MAME4droid
 * and the licenses of the other code concerned, provided that you include
 * the source code of that other code when and as the GNU GPL requires
 * distribution of source code.
 *
 * Note that people who make modified versions of MAME4idroid are not
 * obligated to grant this special exception for their modified versions; it
 * is their choice whether to do so. The GNU General Public License
 * gives permission to release a modified version without this exception;
 * this exception also makes it possible to release a modified version
 * which carries forward this exception.
 *
 * MAME4droid is dual-licensed: Alternatively, you can license MAME4droid
 * under a MAME license, as set out in http://mamedev.org/
 */

package com.seleuco.mame4droid.views;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;

import com.seleuco.mame4droid.MAME4droid;
import com.seleuco.mame4droid.helpers.DialogHelper;
import com.seleuco.mame4droid.helpers.PrefsHelper;
import com.seleuco.mame4droid.input.InputHandler;
import com.seleuco.mame4droid.input.TouchController;

public class EmulatorViewGLExt extends EmulatorViewGL implements android.view.View.OnSystemUiVisibilityChangeListener {

    protected int mLastSystemUiVis;

    private boolean volumeChanges = false;

    public void setMAME4droid(MAME4droid mm) {

        if (mm == null) {
            setOnSystemUiVisibilityChangeListener(null);
            return;
        }
        super.setMAME4droid(mm);
        setNavVisibility(true);
        setOnSystemUiVisibilityChangeListener(this);
    }

    public EmulatorViewGLExt(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    Runnable mNavHider = new Runnable() {
        @Override
        public void run() {
            volumeChanges = false;
            setNavVisibility(false);
        }
    };

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);

        System.out.println("onWindowVisibilityChanged");

        // When we become visible, we show our navigation elements briefly
        // before hiding them.
        if (mm == null)
            return;
        if (mm.getPrefsHelper().getNavBarMode() == PrefsHelper.PREF_NAVBAR_IMMERSIVE) {
            setNavVisibility(false);
            //getHandler().postDelayed(mNavHider, 2000);
        } else
            getHandler().postDelayed(mNavHider, 3000);
    }

    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        // Detect when we go out of low-profile mode, to also go out
        // of full screen.  We only do this when the low profile mode
        // is changing from its last state, and turning off.

        System.out.println("onSystemUiVisibilityChange");
        if ((visibility & SYSTEM_UI_FLAG_HIDE_NAVIGATION) == SYSTEM_UI_FLAG_HIDE_NAVIGATION)
            System.out.println("SYSTEM_UI_FLAG_HIDE_NAVIGATION");
        else
            System.out.println("NO SYSTEM_UI_FLAG_HIDE_NAVIGATION");
        if ((visibility & SYSTEM_UI_FLAG_FULLSCREEN) == SYSTEM_UI_FLAG_FULLSCREEN)
            System.out.println("SYSTEM_UI_FLAG_FULLSCREEN");
        else
            System.out.println("NO SYSTEM_UI_FLAG_FULLSCREEN");


        int diff = mLastSystemUiVis ^ visibility;
        mLastSystemUiVis = visibility;

        if ((diff & SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0
                && (visibility & SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
            setNavVisibility(true);

            if (DialogHelper.savedDialog == DialogHelper.DIALOG_NONE && mm.getPrefsHelper().getNavBarMode() != PrefsHelper.PREF_NAVBAR_IMMERSIVE && !volumeChanges)
                mm.showDialog(DialogHelper.DIALOG_FULLSCREEN);
        } else if ((diff & SYSTEM_UI_FLAG_LOW_PROFILE) != 0
                && (visibility & SYSTEM_UI_FLAG_LOW_PROFILE) == 0) {
            setNavVisibility(true);
        }

    }

    void setNavVisibility(boolean visible) {
        if (mm == null) return;
        int newVis = 0;
        boolean full = mm.getInputHandler().getTouchController().getState() == TouchController.STATE_SHOWING_NONE;

        if (full || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && mm.getPrefsHelper().getNavBarMode() == PrefsHelper.PREF_NAVBAR_IMMERSIVE)) {
            newVis = SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | SYSTEM_UI_FLAG_LAYOUT_STABLE;
        } else {
            newVis = SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | SYSTEM_UI_FLAG_LAYOUT_STABLE;
        }

        if (!visible) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && mm.getPrefsHelper().getNavBarMode() == PrefsHelper.PREF_NAVBAR_IMMERSIVE) {
                newVis |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            } else if (full) {
                newVis |= SYSTEM_UI_FLAG_LOW_PROFILE | SYSTEM_UI_FLAG_FULLSCREEN
                        | SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            } else {
                newVis |= SYSTEM_UI_FLAG_LOW_PROFILE | SYSTEM_UI_FLAG_FULLSCREEN;
            }
        }

        // If we are now visible, schedule a timer for us to go invisible.
        if (visible) {
            Handler h = getHandler();
            if (h != null) {
                h.removeCallbacks(mNavHider);
                h.postDelayed(mNavHider, mm.getPrefsHelper().getNavBarMode() == PrefsHelper.PREF_NAVBAR_IMMERSIVE ? 1000 : 3000);
            }
        }

        // Set the new desired visibility.
        setSystemUiVisibility(newVis);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN || event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {
            volumeChanges = true;

            Handler h = getHandler();
            if (h != null) {
                h.removeCallbacks(mNavHider);
                h.postDelayed(mNavHider, 4000);
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {

        //System.out.println("onWindowFocusChanged:"+hasWindowFocus);
        if (hasWindowFocus)
            if (mm.getPrefsHelper().getNavBarMode() == PrefsHelper.PREF_NAVBAR_IMMERSIVE) {
                setNavVisibility(false);
                //getHandler().postDelayed(mNavHider, 2000);
            } else
                getHandler().postDelayed(mNavHider, 3000);

        super.onWindowFocusChanged(hasWindowFocus);
    }

}
