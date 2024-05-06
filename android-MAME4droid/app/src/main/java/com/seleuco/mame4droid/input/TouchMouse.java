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
import com.seleuco.mame4droid.helpers.DialogHelper;

public class TouchMouse implements IController {
	protected int mouse_pid = -1;
	protected float mouse_prev_x =  0;
	protected float mouse_prev_y =  0;
	protected float mouse_init_x =  0;
	protected float mouse_init_y =  0;
	protected long mouse_millis =  0;
	//protected int mouse_btn_pressed = 0;

	protected boolean mouse_btn1_pressed = false;
	protected boolean mouse_btn2_pressed = false;
	protected boolean mouse_btn1_cancelled = false;
	protected boolean mouse_btn1_pending = false;
	protected long mouse_btn1_ellapsed = 0;
	protected long mouse_btn2_ellapsed = 0;

	protected MAME4droid mm = null;

    public void setMAME4droid(MAME4droid value) {
        mm = value;
    }

    public int getMousePid() {
        return mouse_pid;
    }

	public void handleTouchMouse(View v, MotionEvent event) {
		int pid = 0;
		int action = event.getAction();
		int actionEvent = action & MotionEvent.ACTION_MASK;

		int pointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
		pid = event.getPointerId(pointerIndex);

		//dumpEvent(event);

		if (actionEvent == MotionEvent.ACTION_UP ||
			actionEvent == MotionEvent.ACTION_POINTER_UP ||
			actionEvent == MotionEvent.ACTION_CANCEL) {

			//Log.d("touch", "event ---> mouse_pid:"+mouse_pid+ " pid: "+pid);

			if (pid == mouse_pid) {

				mouse_pid = -1;

				float cx = mouse_prev_x - mouse_init_x;
				float cy = mouse_prev_y - mouse_init_y;

				if (System.currentTimeMillis() - mouse_millis < 250 && Math.abs(cx) <= 4 && Math.abs(cy) <= 4) {
					Emulator.setMouseData(0, Emulator.MOUSE_BTN_DOWN, 1, -1, -1);
					//Log.d("MOUSE", "event main BTN DOWN 1");
					Thread t = new Thread(new Runnable() {
						public void run() {
							try {
								Thread.sleep(60);
							} catch (InterruptedException e) {
							}
							Emulator.setMouseData(0, Emulator.MOUSE_BTN_UP, 1, -1, -1);
							//Log.d("MOUSE", "event main BTN UP 1");
						}
					});
					t.start();
				}

				mouse_prev_x = 0;
				mouse_prev_y = 0;
			}

			//if (actionEvent == MotionEvent.ACTION_POINTER_UP ||
			//actionEvent == MotionEvent.ACTION_CANCEL) {

			//Log.d("touch", "event ---> mouse_btn1_pending:"+mouse_btn1_pending+ " mouse_btn1_pressed:"+mouse_btn1_pressed);

			if ((mouse_btn1_pending || mouse_btn1_pressed) && event.getPointerCount()<=2) {
				Thread t = new Thread(new Runnable() {
					public void run() {
						while (mouse_btn1_pending)
							try {
								Thread.sleep(10);
							} catch (InterruptedException e) {
							}
						if (mouse_btn1_pressed) {
							Emulator.setMouseData(0, Emulator.MOUSE_BTN_UP, 1, -1, -1);
							mouse_btn1_pressed = false;
							//Log.d("MOUSE", "event BTN UP 1 ms: " + (System.currentTimeMillis() - mouse_btn1_ellapsed) + " + 25");
						}
					}
				});
				t.start();

			}
			if (mouse_btn2_pressed) {
				Emulator.setMouseData(0, Emulator.MOUSE_BTN_UP, 2, -1, -1);
				mouse_btn2_pressed = false;
				//Log.d("MOUSE", "event BTN UP 2 ms:" + (System.currentTimeMillis() - mouse_btn2_ellapsed));
			}

			//}

		} else { //MOVE/DOWN/POINTERDOWN

			if (mouse_pid != -1 && actionEvent == MotionEvent.ACTION_POINTER_DOWN) {

				//Emulator.setMouseData(0,Emulator.MOUSE_BTN_DOWN,1,-1,-1);
				int numpointers = event.getPointerCount();

				long elapsed = System.currentTimeMillis() - mouse_millis;

				if (numpointers == 2 && !mouse_btn1_pending && !mouse_btn1_pressed && elapsed > 150) {
					mouse_btn1_cancelled = false;
					mouse_btn1_pending = true;
					Thread t = new Thread(new Runnable() {
						public void run() {
							try {
								Thread.sleep(25);
							} catch (InterruptedException e) {
							}
							mouse_btn1_ellapsed = System.currentTimeMillis();
							mouse_btn1_pressed = !mouse_btn1_cancelled;
							if (mouse_btn1_pressed) {
								Emulator.setMouseData(0, Emulator.MOUSE_BTN_DOWN, 1, -1, -1);
								//Log.d("MOUSE", "event BTN DOWN 1");
							}
							mouse_btn1_pending = false;
						}
					});
					t.start();
				} else if (numpointers == 3 && !mouse_btn2_pressed /*&& elapsed > 150*/) {
					mouse_btn1_cancelled = true;
					mouse_btn2_pressed = true;
					mouse_btn2_ellapsed = System.currentTimeMillis();
					Emulator.setMouseData(0, Emulator.MOUSE_BTN_DOWN, 2, -1, -1);
					//Log.d("MOUSE", "event BTN DOWN 2");
				}
			}

			for (int i = 0; i < event.getPointerCount(); i++) {

				int pointerId = event.getPointerId(i);

				float x = event.getX(i);
				float y = event.getY(i);

				if (mouse_pid == -1) {
					mouse_pid = pointerId;
					mouse_init_x = x;
					mouse_init_y = y;
					mouse_prev_x = x;
					mouse_prev_y = y;
					mouse_millis = System.currentTimeMillis();
				} else if (mouse_pid == pointerId) {

					float cx = x - mouse_prev_x;
					mouse_prev_x = x;
					cx = cx;

					float cy = y - mouse_prev_y;
					mouse_prev_y = y;
					cy = cy;

					Emulator.setMouseData(0, Emulator.MOUSE_MOVE, 0, cx, cy);
				}

			}

		}
	}

}
