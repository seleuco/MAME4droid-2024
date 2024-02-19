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

import static android.content.res.Configuration.KEYBOARD_QWERTY;

import android.graphics.Color;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.seleuco.mame4droid.Emulator;
import com.seleuco.mame4droid.MAME4droid;
import com.seleuco.mame4droid.widgets.WarnWidget;

public class Keyboard implements IController {

	protected long last_soft_key_time = -1;
	protected long last_soft_key_code = -1;

	protected boolean isKeyboardEnabled = false;

	public boolean isKeyboardEnabled() {return isKeyboardEnabled;}

	protected MAME4droid mm = null;

    public void setMAME4droid(MAME4droid value) {
        mm = value;
    }

	public boolean isKeyboardConnected() {
		return mm.getResources().getConfiguration().keyboard == KEYBOARD_QWERTY && mm.getPrefsHelper().isKeyboardEnabled();
	}

    public boolean handleKeyboard(int keyCode, KeyEvent event){

		//Log.d("TECLA", "device=" + event.getDeviceId()+ " " + event.getDevice().isVirtual() + " "+keyCode + " " + event.getAction() + " " + event.getDisplayLabel() + " " + event.getUnicodeChar() + " " + event.getNumber());

		if(event.getDevice().isVirtual() && !mm.getPrefsHelper().isVirtualKeyboardEnabled())
			return false;

		if(event.getDevice().isVirtual()) {//SOFT KEYBOARD, HACK TO AVOID CONTINUOUS DOWN/UP on VKEYBOARDS
			//Log.d("TECLA", "TIME=" + event.getEventTime());
			//final int wait_time = 25;
			final int wait_time = 45;
			if(event.getAction() == KeyEvent.ACTION_DOWN && last_soft_key_time==-1)
			{
				last_soft_key_time = event.getEventTime();
				last_soft_key_code = event.getKeyCode();
				//Log.d("TECLA", "DOWN time=" + event.getEventTime());
			}
			else if(last_soft_key_code == event.getKeyCode()) {//Esperamos wait time entre pulsaciones. Solo vale para una tecla.
				if(event.getEventTime() - last_soft_key_time < wait_time) {
					try {
						Thread.sleep((last_soft_key_time + wait_time) - event.getEventTime());
					} catch (InterruptedException e) {
					}
				}
				last_soft_key_time = -1;
				//Log.d("TECLA", "UP time=" + event.getEventTime());
			}
		}

		boolean handle_keyboard = event.getDevice().isVirtual() || this.isKeyboardConnected();

		int res = 0;

		if(handle_keyboard) {
			res = Emulator.setKeyData(event.getKeyCode(), event.getAction() == KeyEvent.ACTION_DOWN ? Emulator.KEY_DOWN : Emulator.KEY_UP,
				(char) event.getUnicodeChar());
		}

		boolean handled = res == 1;

		if(handled)
		{
			if(!event.getDevice().isVirtual() && !isKeyboardEnabled)
			{
				isKeyboardEnabled = true;
				CharSequence text = "Keyboard is enabled!";
				new WarnWidget.WarnWidgetHelper(mm, text.toString(), 3, Color.GREEN, true);

				mm.getMainHelper().updateMAME4droid();
				mm.getInputHandler().resetInput(true);
			}
		}

			/*
			if(event.getKeyCode()==KeyEvent.KEYCODE_TAB)//avoid system tab.
				return true;
			*/
		return handled;
	}

}
