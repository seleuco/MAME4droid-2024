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

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.seleuco.mame4droid.Emulator;
import com.seleuco.mame4droid.MAME4droid;
import com.seleuco.mame4droid.helpers.MainHelper;

public class TouchLightgun implements IController {

	protected int lightgun_pid = -1;

	protected long millis_pressed = 0;
	protected boolean press_on = false;

	protected MAME4droid mm = null;

    public void setMAME4droid(MAME4droid value) {
        mm = value;
    }

	public int getLightgun_pid(){
		return lightgun_pid;
	}

	public void reset() {
		lightgun_pid = -1;
	}

	public void handleTouchLightgun(View v, MotionEvent event,int [] digital_data) {
		int pid = 0;
		int action = event.getAction();
		int actionEvent = action & MotionEvent.ACTION_MASK;

		int pointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
		pid = event.getPointerId(pointerIndex);

		if (actionEvent == MotionEvent.ACTION_UP ||
			actionEvent == MotionEvent.ACTION_POINTER_UP ||
			actionEvent == MotionEvent.ACTION_CANCEL) {
			if (pid == lightgun_pid) {
				millis_pressed = 0;
				press_on = false;
				lightgun_pid = -1;
				//Emulator.setAnalogData(4, 0, 0);
				digital_data[0] &= ~A_VALUE;
				digital_data[0] &= ~B_VALUE;
			} else {
				if(!press_on)
				   digital_data[0] &= ~B_VALUE;
				else
				   digital_data[0] &= ~A_VALUE;
			}

			Emulator.setDigitalData(0, digital_data[0]);
		} else {
			for (int i = 0; i < event.getPointerCount(); i++) {

				int pointerId = event.getPointerId(i);

				final int[] location = {0, 0};
				v.getLocationOnScreen(location);
				int x = (int) event.getX(i) + location[0];
				int y = (int) event.getY(i) + location[1];

				//System.out.println("x:"+event.getX(i)+" y:"+event.getY(i)+" nx:"+x+" ny:"+y+" l0:"+location[0]+" l1:"+location[1]);

				mm.getEmuView().getLocationOnScreen(location);
				x -= location[0];
				y -= location[1];

				//Log.d("LIGHTGUN"," x:"+ x+" "+" y:"+y);

				float x1 = mm.getEmuView().getWidth();
				float y1 = 	mm.getEmuView().getHeight();

				float xf = (float) (x - x1 / 2) / (float) (x1 / 2);
				float yf = (float) (y - y1 / 2) / (float) (y1 / 2);

				//System.out.println("nx2:"+x+" ny2:"+y+" l0:"+location[0]+" l1:"+location[1]+" xf:"+xf+" yf:"+yf);

				if (lightgun_pid == -1)
					lightgun_pid = pointerId;

				if (lightgun_pid == pointerId) {

					if(!press_on)
					{
						if (mm.getPrefsHelper().isBottomReload()) {
							if (yf > 0.90)
								yf = 1.1f;
						}

						if (!mm.getInputHandler().getTiltSensor().isEnabled()) {
							Log.d("LIGHTGUN","POS F1 x:"+ xf+" "+" y:"+-yf);
							Emulator.setAnalogData(8, xf, -yf);
						}

						if ((digital_data[0] & B_VALUE) == 0)
							digital_data[0] |= A_VALUE;

						if(mm.getPrefsHelper().isLightgunLongPress()) {
							int wait = 125;
							if(mm.getMainHelper().getDeviceDetected()== MainHelper.DEVICE_METAQUEST)
								wait = 300;
							if (millis_pressed == 0) {
								millis_pressed = System.currentTimeMillis();
							} else if (System.currentTimeMillis() - millis_pressed > wait && !press_on) {
								press_on = true;
								digital_data[0] |= B_VALUE;
								digital_data[0] &= ~A_VALUE;
							}
						}
					}
				} else {
					if(!press_on) {
						digital_data[0] &= ~A_VALUE;
						digital_data[0] |= B_VALUE;
					}
					else
					{
						if (!mm.getInputHandler().getTiltSensor().isEnabled()) {
							//Log.d("LIGHTGUN","POS F2 x:"+ xf+" "+" y:"+-yf);
							Emulator.setAnalogData(8, xf, -yf);
						}

						digital_data[0] |= A_VALUE;
					}
				}
			}
			Emulator.setDigitalData(0, digital_data[0]);
		}
	}

}
