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

package com.seleuco.mame4droid.input;

import android.graphics.Color;
import android.view.MotionEvent;
import android.widget.Toast;

import com.seleuco.mame4droid.Emulator;
import com.seleuco.mame4droid.MAME4droid;
import com.seleuco.mame4droid.widgets.WarnWidget;

public class Mouse implements IController {

	protected boolean isMouseEnabled = false;
	public boolean isEnabled() {
		return isMouseEnabled;
	}

	protected MAME4droid mm = null;

    public void setMAME4droid(MAME4droid value) {
        mm = value;
    }

	public boolean handleMouse(MotionEvent event) {

		//Log.d("MOUSEB", event.toString());

		if (!isMouseEnabled) {
			isMouseEnabled = true;
			CharSequence text = "Mouse is enabled!";
			new WarnWidget.WarnWidgetHelper(mm, text.toString(), 3, Color.GREEN, true);

			mm.getMainHelper().updateMAME4droid();
			mm.getInputHandler().resetInput(true);
		}

		float cx = event.getX();
		float cy = event.getY();
		int aBtn = event.getActionButton();
		int action = event.getAction();

		if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {

			float x = event.getX();
			for (int i = 0; i < event.getHistorySize(); i++) {
				x += event.getHistoricalX(i);
			}

			float y = event.getY();
			for (int i = 0; i < event.getHistorySize(); i++) {
				y += event.getHistoricalY(i);
			}

			Emulator.setMouseData(0,Emulator.MOUSE_MOVE, 0, x, y);
		}

		//int pressedButtons = event.getButtonState();

		if (aBtn == MotionEvent.BUTTON_PRIMARY) {
			Emulator.setMouseData(0, action == MotionEvent.ACTION_BUTTON_PRESS ? Emulator.MOUSE_BTN_DOWN : Emulator.MOUSE_BTN_UP, 1, -1, -1);
		}
		if (aBtn == MotionEvent.BUTTON_SECONDARY || aBtn == MotionEvent.BUTTON_BACK) {
			Emulator.setMouseData(0, action == MotionEvent.ACTION_BUTTON_PRESS ? Emulator.MOUSE_BTN_DOWN : Emulator.MOUSE_BTN_UP, 2, -1, -1);
		}
		if (aBtn == MotionEvent.BUTTON_TERTIARY || aBtn == MotionEvent.BUTTON_FORWARD) {
			Emulator.setMouseData(0, action == MotionEvent.ACTION_BUTTON_PRESS ? Emulator.MOUSE_BTN_DOWN : Emulator.MOUSE_BTN_UP, 3, -1, -1);
		}

		//Log.d("RATON", " cx="+cx+" cy="+cy+" aBtn="+aBtn+" act"+action);
		return true;
	}

}
