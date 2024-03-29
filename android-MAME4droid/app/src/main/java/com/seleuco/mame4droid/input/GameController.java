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
import android.util.Log;
import android.util.SparseIntArray;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Toast;

import com.seleuco.mame4droid.Emulator;
import com.seleuco.mame4droid.MAME4droid;
import com.seleuco.mame4droid.helpers.DialogHelper;
import com.seleuco.mame4droid.helpers.MainHelper;
import com.seleuco.mame4droid.helpers.PrefsHelper;
import com.seleuco.mame4droid.widgets.WarnWidget;

import java.util.Arrays;

public class GameController implements IController {

	protected static Boolean fakeID = false;

	protected static final int[] emulatorInputValues = {
		UP_VALUE,
		DOWN_VALUE,
		LEFT_VALUE,
		RIGHT_VALUE,
		A_VALUE,
		B_VALUE,
		C_VALUE,
		D_VALUE,
		E_VALUE,
		F_VALUE,
		G_VALUE,
		H_VALUE,
		COIN_VALUE,
		START_VALUE,
		EXIT_VALUE,
		OPTION_VALUE
		///
	};

	public static int[] defaultKeyMapping = {
		makeKeyCodeWithDeviceID(1,KeyEvent.KEYCODE_DPAD_UP),
		makeKeyCodeWithDeviceID(1,KeyEvent.KEYCODE_DPAD_DOWN),
		makeKeyCodeWithDeviceID(1,KeyEvent.KEYCODE_DPAD_LEFT),
		makeKeyCodeWithDeviceID(1,KeyEvent.KEYCODE_DPAD_RIGHT),
		makeKeyCodeWithDeviceID(1,KeyEvent.KEYCODE_BUTTON_B),
		makeKeyCodeWithDeviceID(1,KeyEvent.KEYCODE_BUTTON_A),
		makeKeyCodeWithDeviceID(1,KeyEvent.KEYCODE_BUTTON_X),
		makeKeyCodeWithDeviceID(1,KeyEvent.KEYCODE_BUTTON_Y),
		makeKeyCodeWithDeviceID(1,KeyEvent.KEYCODE_BUTTON_L1),
		makeKeyCodeWithDeviceID(1,KeyEvent.KEYCODE_BUTTON_R1),
		makeKeyCodeWithDeviceID(1,KeyEvent.KEYCODE_BUTTON_L2),
		makeKeyCodeWithDeviceID(1,KeyEvent.KEYCODE_BUTTON_R2),
		makeKeyCodeWithDeviceID(1,KeyEvent.KEYCODE_BUTTON_THUMBR),
		makeKeyCodeWithDeviceID(1,KeyEvent.KEYCODE_BUTTON_THUMBL),
		KeyEvent.KEYCODE_BACK,
		KeyEvent.KEYCODE_MENU,
		//////
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		//////
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		//////
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
	};

	public static int[] keyMapping = new int[emulatorInputValues.length * 4];

	static protected int MAX_DEVICES = 4;
	static protected int MAX_KEYS = 250;
	protected float MY_PI = 3.14159265f;

	protected int[] oldinput = new int[MAX_DEVICES], newinput = new int[MAX_DEVICES];

	public static int[] deviceIDs = new int[MAX_DEVICES];
	//public  static int id = 0;

	boolean joystickMotion = false;

	protected int[][] deviceMappings = new int[MAX_KEYS][MAX_DEVICES];

	protected static SparseIntArray banDev = new SparseIntArray(50);

	final public float rad2degree(float r) {
		return ((r * 180.0f) / MY_PI);
	}

	protected MAME4droid mm = null;

    public void setMAME4droid(MAME4droid value) {
        mm = value;
		if(mm==null)return;

		fakeID = mm.getPrefsHelper().isFakeID();

		int[] ids = InputDevice.getDeviceIds();
		for (int j : ids) {
			InputDevice id = InputDevice.getDevice(j);
			if (id != null) {
				System.out.println("name: " + id.getName());
				System.out.println(id.toString());
			}
		}

		resetAutodetected();
    }

	public static void resetAutodetected() {
		//id = 0;
		Arrays.fill(deviceIDs, -1);
		banDev.clear();
	}

	public static int getGamePadId(InputDevice id) {
		int iDeviceId = 0;
		int iControllerNumber = 0;

		try {
			if (!fakeID)
				iDeviceId = id.getId();
			else
				iDeviceId = 0;
		} catch (Exception ignored) {
		}

		if (!fakeID) {
			iControllerNumber = id.getControllerNumber();
			if (iControllerNumber > 0)
				iDeviceId = iControllerNumber;
		}
		return iDeviceId;
	}

	public static int makeKeyCodeWithDeviceID(InputDevice id, int iKeyCode) {
		int padid = 0;
		try {
			padid = getGamePadId(id);
		} catch (Exception ignored) {
		}

		return makeKeyCodeWithDeviceID(padid, iKeyCode);
	}

	public static int makeKeyCodeWithDeviceID(int iDeviceId, int iKeyCode) {
		int iRet = 0;

		//iRet = ((iDeviceId * 1000) + iKeyCode);//type 1

		//type 2
		iRet = iDeviceId;
		iRet = iRet << 16;
		iRet |= iKeyCode;
		//type 2 end

		return iRet;
	}

	public static void getInfoFromKeyCodeWithDeviceID(int iKeyCode, int[] iArrRet) {
		int iDeviceIdRet = 0;
		int iKeyCodeRet = 0;

		//type 1
		/*iDeviceIdRet = iKeyCode / 1000;
		iKeyCodeRet = iKeyCode % 1000;
		*/
		//type 1 end

		//type 2
		iDeviceIdRet = iKeyCode >> 16;
		iKeyCodeRet = iKeyCode & 0xFFFF;
		//type 2 end

		iArrRet[0] = iDeviceIdRet;
		iArrRet[1] = iKeyCodeRet;
	}

	public static int getDeviceIdFromKeyCodeWithDeviceID(int iKeyCode) {
		//return  iKeyCode / 1000; //type 1
		return iKeyCode >> 16; //type 2
	}

	public static int getKeyCodeFromKeyCodeWithDeviceID(int iKeyCode) {
		//return  iKeyCode % 1000;  //type 1
		return iKeyCode & 0xFFFF; //type 2
	}

	protected void setContollerData(int i, KeyEvent event, int data, int[]digital_data) {
		int action = event.getAction();
		if (action == KeyEvent.ACTION_DOWN)
			digital_data[i] |= data;
		else if (action == KeyEvent.ACTION_UP)
			digital_data[i] &= ~data;
	}

	protected boolean handleControllerKey(int value, KeyEvent event, int []digital_data) {

		int v = emulatorInputValues[value % emulatorInputValues.length];

		if (v == EXIT_VALUE) {
			if (event.getAction() == KeyEvent.ACTION_UP) {
				/*
				if (Emulator.isInMenu()) {
					Emulator.setValue(Emulator.EXIT_GAME, 1);
					try {
						Thread.sleep(InputHandler.PRESS_WAIT);
					} catch (InterruptedException ignored) {
					}
					Emulator.setValue(Emulator.EXIT_GAME, 0);
				} else if (!Emulator.isInGame()) {
					mm.showDialog(DialogHelper.DIALOG_EXIT);
				} else {
				*/
					Emulator.setValue(Emulator.EXIT_GAME, 1);
					try {
						Thread.sleep(InputHandler.PRESS_WAIT);
					} catch (InterruptedException e) {
					}
					Emulator.setValue(Emulator.EXIT_GAME, 0);
				//}
			}
		} else if (v == OPTION_VALUE ) {
			if (event.getAction() == KeyEvent.ACTION_UP && !Emulator.isInOptions()) {
				Emulator.setInOptions(true);
				mm.showDialog(DialogHelper.DIALOG_OPTIONS);
			}
		} else {
			int i = value / emulatorInputValues.length;
			setContollerData(i, event, v,digital_data);
			mm.getInputHandler().fixTiltCoin();
			Emulator.setDigitalData(i, digital_data[i]);
		}

		return true;
	}

	public boolean handleGameController(int keyCode, KeyEvent event,int[]digital_data){

		boolean manageDevice = true;
		int dev = -1;

		if (!mm.getPrefsHelper().isContollerAutodetect()) {
			manageDevice = false;
		}

		if(manageDevice)
		   dev = getDevice(event.getDevice(), true);

		//System.out.println(event.getDevice().getName()+" "+dev+" "+" "+event.getKeyCode());
		//System.out.println("IME:"+Settings.Secure.getString(mm.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD));

		if (dev == -1) {//no detected
			manageDevice = false;
		}

		if(!manageDevice) {

			if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
				handleControllerKey(14, event, digital_data);
				return true;
			}

			if (event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
				handleControllerKey(15, event, digital_data);
				return true;
			}
//TODO ver en android tv START y SELEC
			if ((event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_START || event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER
				|| event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_SELECT) && !Emulator.isInGame() && mm.getMainHelper().isAndroidTV()) {
				handleControllerKey(15, event, digital_data);
				return true;
			}

			int value = -1;
			for (int i = 0; i < keyMapping.length; i++) {
				//if(keyMapping[i]==keyCode)
				if (keyMapping[i] == makeKeyCodeWithDeviceID(event.getDevice(), keyCode))
					value = i;
			}

			//if(value >=0 && value <=13)
			if (value != -1)
				if (handleControllerKey(value, event, digital_data)) return true;

			return false;
		}
		else
		{
			int v = deviceMappings[event.getKeyCode()][dev];

			if (v != -1) {
				if (v == EXIT_VALUE) {
					if (event.getAction() == KeyEvent.ACTION_UP) {
						//if (Emulator.isInMenu()) {
							Emulator.setValue(Emulator.EXIT_GAME, 1);
							try {
								Thread.sleep(InputHandler.PRESS_WAIT);
							} catch (InterruptedException ignored) {
							}
							Emulator.setValue(Emulator.EXIT_GAME, 0);
						/*
						} else if (!Emulator.isInGame()) {
							mm.showDialog(DialogHelper.DIALOG_EXIT);
						} else {
							Emulator.setValue(Emulator.EXIT_GAME, 1);
							try {
								Thread.sleep(InputHandler.PRESS_WAIT);
							} catch (InterruptedException ignored) {
							}
							Emulator.setValue(Emulator.EXIT_GAME, 0);
						}
						 */
					}
				} else if (v == OPTION_VALUE) {
					if (event.getAction() == KeyEvent.ACTION_UP  && !Emulator.isInOptions()) {
						Emulator.setInOptions(true);
						mm.showDialog(DialogHelper.DIALOG_OPTIONS);
					}
				} else {
					int action = event.getAction();
					if (action == KeyEvent.ACTION_DOWN) {
						digital_data[dev] |= v;
					} else if (action == KeyEvent.ACTION_UP)
						digital_data[dev] &= ~v;

					mm.getInputHandler().fixTiltCoin();

					Emulator.setDigitalData(dev, digital_data[dev]);
				}
				return true;
			}

			return false;
		}
	}

	protected float processAxis(InputDevice.MotionRange range, float axisvalue) {
		float absaxisvalue = Math.abs(axisvalue);
		float deadzone = range.getFlat();
		//System.out.println("deadzone: "+deadzone);
		//deadzone = Math.max(deadzone, 0.2f);
		if (absaxisvalue <= deadzone) {
			return 0.0f;
		}
		float nomralizedvalue;
		if (axisvalue < 0.0f) {
			nomralizedvalue = absaxisvalue / range.getMin();
		} else {
			nomralizedvalue = absaxisvalue / range.getMax();
		}

		return nomralizedvalue;
	}

	final public float getAxisValue(int axis, MotionEvent event, int historyPos) {
		float value = 0.0f;
		InputDevice device = event.getDevice();
		if (device != null) {
			InputDevice.MotionRange range = device.getMotionRange(axis, event.getSource());
			if (range != null) {
				float axisValue;

				if (historyPos >= 0) {
					axisValue = event.getHistoricalAxisValue(axis, historyPos);
				} else {
					axisValue = event.getAxisValue(axis);
				}
				value = this.processAxis(range, axisValue);
				//System.out.print("x: "+x);
			}
		}
		return value;
	}

	final public float getAngle(float x, float y) {
		float ang = rad2degree((float) Math.atan(y / x));
		ang -= 90.0f;
		if (x < 0.0f)
			ang -= 180.0f;
		ang = Math.abs(ang);
		return ang;
	}

	final public float getMagnitude(float x, float y) {
		return (float) Math.sqrt((x * x) + (y * y));
	}

	protected boolean processJoystickInput(MotionEvent event, int historyPos, int[]digital_data) {

		int ways = mm.getPrefsHelper().getStickWays();
		if (ways == -1) ways = Emulator.getValue(Emulator.NUMWAYS);
		boolean b = Emulator.isInGameButNotInMenu();

		int dev = getDevice(event.getDevice(), false);

		int iDeviceId = 0;
		try {
			iDeviceId = getGamePadId(event.getDevice());
		} catch (Exception ignored) {
		}

		if (dev == -1) { //no autodetectado
			for (int i = 0; i < MAX_DEVICES; i++) {
				if (iDeviceId == getDeviceIdFromKeyCodeWithDeviceID(keyMapping[i * emulatorInputValues.length])) // select each devices input settings first item UP_VALUE dpad setting not applicable seperate
				{
					dev = i;
					break;
				}
			}
		}

		int joy = dev != -1 ? dev : 0;

		newinput[joy] = 0;

		float deadZone = 0.2f;

		switch (mm.getPrefsHelper().getGamepadDZ()) {
			case 1:
				deadZone = 0.01f;
				break;
			case 2:
				deadZone = 0.15f;
				break;
			case 3:
				deadZone = 0.2f;
				break;
			case 4:
				deadZone = 0.3f;
				break;
			case 5:
				deadZone = 0.5f;
				break;
		}

		//System.out.println("DEAD ZONE IS "+deadZone);

		float x = 0.0f;
		float y = 0.0f;
		float mag = 0.0f;

		for (int i = 0; i < 2; i++) {
			if (i == 0 &&  mm.getInputHandler().getTiltSensor().isEnabled() && Emulator.isInGameButNotInMenu())
				continue;

			if (i == 0) {
				x = getAxisValue(MotionEvent.AXIS_X, event, historyPos);
				y = getAxisValue(MotionEvent.AXIS_Y, event, historyPos);
			} else {
				x = getAxisValue(MotionEvent.AXIS_HAT_X, event, historyPos);
				y = getAxisValue(MotionEvent.AXIS_HAT_Y, event, historyPos);
			}

			mag = getMagnitude(x, y);

			if (mag >= deadZone) {
				if (dev == -1) {
					dev = getDevice(event.getDevice(), true);
					if (dev != -1) {
						joy = dev;
						newinput[joy] = 0;
					}
				}

				if (i == 0) {
					/*
					Emulator.setAnalogData(joy, x, y * -1.0f);
					if (Emulator.isInGame())
						continue;*/
					if (mm.getPrefsHelper().getControllerType() != PrefsHelper.PREF_DIGITAL_STICK) {
						//Log.d("TRIGGER","x:"+x+" y:"+y);
						Emulator.setAnalogData(joy, x, y * -1.0f);
						continue;
					}
				}

				float v = getAngle(x, y);

				if (ways == 2 && b) {
					if (v < 180) {
						newinput[joy] |= RIGHT_VALUE;
					} else if (v >= 180) {
						newinput[joy] |= LEFT_VALUE;
					}
				} else if (ways == 4 || !b) {
					if (v >= 315 || v < 45) {
						newinput[joy] |= DOWN_VALUE;
					} else if (v >= 45 && v < 135) {
						newinput[joy] |= RIGHT_VALUE;
					} else if (v >= 135 && v < 225) {
						newinput[joy] |= UP_VALUE;
					} else if (v >= 225 && v < 315) {
						newinput[joy] |= LEFT_VALUE;
					}
				} else {
					if (v >= 330 || v < 30) {
						newinput[joy] |= DOWN_VALUE;
					} else if (v >= 30 && v < 60) {
						newinput[joy] |= DOWN_VALUE;
						newinput[joy] |= RIGHT_VALUE;
					} else if (v >= 60 && v < 120) {
						newinput[joy] |= RIGHT_VALUE;
					} else if (v >= 120 && v < 150) {

						newinput[joy] |= RIGHT_VALUE;
						newinput[joy] |= UP_VALUE;
					} else if (v >= 150 && v < 210) {
						newinput[joy] |= UP_VALUE;
					} else if (v >= 210 && v < 240) {
						newinput[joy] |= UP_VALUE;
						newinput[joy] |= LEFT_VALUE;
					} else if (v >= 240 && v < 300) {
						newinput[joy] |= LEFT_VALUE;
					} else if (v >= 300 && v < 330) {
						newinput[joy] |= LEFT_VALUE;
						newinput[joy] |= DOWN_VALUE;
					}
				}
			} else {
				if (i == 0) {
					Emulator.setAnalogData(joy, 0, 0);
				}
			}
		}

		if (!mm.getPrefsHelper().isDisabledRightStick() && Emulator.isInGame()) {

			x = getAxisValue(MotionEvent.AXIS_Z, event, historyPos);
			y = getAxisValue(MotionEvent.AXIS_RZ, event, historyPos) * -1;

			mag = getMagnitude(x, y);

			if (mag >= deadZone) {

				float v = getAngle(x, y);

				if (v >= 330 || v < 30) {
					newinput[joy] |= D_VALUE;
				} else if (v >= 30 && v < 60) {
					newinput[joy] |= D_VALUE;
					newinput[joy] |= A_VALUE;
				} else if (v >= 60 && v < 120) {
					newinput[joy] |= A_VALUE;
				} else if (v >= 120 && v < 150) {
					newinput[joy] |= A_VALUE;
					newinput[joy] |= B_VALUE;
				} else if (v >= 150 && v < 210) {
					newinput[joy] |= B_VALUE;
				} else if (v >= 210 && v < 240) {
					newinput[joy] |= B_VALUE;
					newinput[joy] |= C_VALUE;
				} else if (v >= 240 && v < 300) {
					newinput[joy] |= C_VALUE;
				} else if (v >= 300 && v < 330) {
					newinput[joy] |= C_VALUE;
					newinput[joy] |= D_VALUE;
				}
			}
		}

		x = getAxisValue(MotionEvent.AXIS_GAS, event, historyPos);
		//Log.d("TRIGGER","x:"+ x);
		if (x >= 0.35f) {
			//nothing
		}
		y = getAxisValue(MotionEvent.AXIS_BRAKE, event, historyPos);
		//System.out.println("y:"+y);
		if (y >= 0.35f) {
            //nothing
		}
		//Log.d("TRIGGER","y:"+y);
		Emulator.setAnalogData(joy+4, (x * 2.0f) -1.0f, (y * 2.0f) -1.0f);

		digital_data[joy] &= ~(oldinput[joy] & ~newinput[joy]);
		digital_data[joy] |= newinput[joy];

		mm.getInputHandler().fixTiltCoin();

		Emulator.setDigitalData(joy, digital_data[joy]);

		oldinput[joy] = newinput[joy];

		return true;
	}

	public boolean genericMotion(MotionEvent event,int[]digital_data ) {
/*
		if (!mm.getPrefsHelper().isContollerAutodetect()) {
			return false;
		}
*/
		if (((event.getSource() & (InputDevice.SOURCE_CLASS_JOYSTICK | InputDevice.SOURCE_GAMEPAD)) == 0)
			|| (event.getAction() != MotionEvent.ACTION_MOVE)) {
			return false;
		}

		if(!joystickMotion) {
			int dev = getDevice(event.getDevice(), true);
			if(dev==-1) { //joystick generico no autodetectado
				CharSequence text = "Detected generic controller. You should map it on settings! ";
				new WarnWidget.WarnWidgetHelper(mm, text.toString(), 3, Color.YELLOW, true);
				mm.getMainHelper().updateMAME4droid();
			}
			joystickMotion = true;
		}

		int historySize = event.getHistorySize();
		for (int i = 0; i < historySize; i++) {
			processJoystickInput(event, i,digital_data);
		}

		return processJoystickInput(event, -1,digital_data);
	}

	protected int getDevice(InputDevice device, boolean detect) {

		if (!mm.getPrefsHelper().isContollerAutodetect())
			return -1;

		if (device == null)
			return -1;
		//dav
		if (device.getId() == -1)
			return -1;
		///
		for (int i = 0; i < MAX_DEVICES; i++) {
			if (deviceIDs[i] == device.getId())
				return i;
		}

		//clean dissconected devices
		int[] ids = InputDevice.getDeviceIds();
		for (int i = 0; i < MAX_DEVICES; i++) {
			boolean found = false;
			for (int j = 0; j < ids.length && !found; j++) {
				found = deviceIDs[i] == ids[j];
			}
			if (!found) {
				deviceIDs[i] = -1;
				banDev.clear();
			}
		}

		if (detect)
			return detectDevice(device);
		else
			return -1;
	}

	protected void mapDPAD(int id) {
		deviceMappings[KeyEvent.KEYCODE_DPAD_UP][id] = UP_VALUE;
		deviceMappings[KeyEvent.KEYCODE_DPAD_DOWN][id] = DOWN_VALUE;
		deviceMappings[KeyEvent.KEYCODE_DPAD_LEFT][id] = LEFT_VALUE;
		deviceMappings[KeyEvent.KEYCODE_DPAD_RIGHT][id] = RIGHT_VALUE;
	}

	protected void mapL1R1(int id) {
		deviceMappings[KeyEvent.KEYCODE_BUTTON_L1][id] = E_VALUE;
		deviceMappings[KeyEvent.KEYCODE_BUTTON_R1][id] = F_VALUE;
	}

	protected void mapTHUMBS(int id) {
		deviceMappings[KeyEvent.KEYCODE_BUTTON_THUMBL][id] = START_VALUE;
		deviceMappings[KeyEvent.KEYCODE_BUTTON_THUMBR][id] = COIN_VALUE;
	}

	protected void mapSelectStart(int id) {
		deviceMappings[KeyEvent.KEYCODE_BUTTON_SELECT][id] = EXIT_VALUE;
		deviceMappings[KeyEvent.KEYCODE_BUTTON_START][id] = OPTION_VALUE;
	}

	protected int detectDevice(InputDevice device) {

		boolean detected = false;

		int id = -1;
		for (int i = 0; i < MAX_DEVICES && id == -1; i++) {
			if (deviceIDs[i] == -1)
				id = i;
		}

		if (id == -1)
			return -1;

		if (device == null || banDev == null)
			return -1;

		if (banDev.get(device.getId()) == 1)
			return -1;

		final String name = device.getName();

		if (Emulator.isDebug()) {
			String msg = "Detected input device: " + name;
			new WarnWidget.WarnWidgetHelper(mm, msg, 3, Color.GREEN, true);
		}

		CharSequence desc = "";

		if (name.contains("PLAYSTATION(R)3") || name.indexOf("Dualshock3") != -1
			|| name.contains("Sixaxis") || name.contains("Gasia,Co")
		) {

			//deviceMappings[KeyEvent.KEYCODE_BUTTON_B][id] = Y_VALUE;
			//deviceMappings[KeyEvent.KEYCODE_BUTTON_X][id] = X_VALUE;
			//deviceMappings[KeyEvent.KEYCODE_BUTTON_A][id] = A_VALUE;
			//deviceMappings[KeyEvent.KEYCODE_BUTTON_Y][id] = B_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_B][id] = A_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_A][id] = B_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_X][id] = C_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_Y][id] = D_VALUE;

			mapDPAD(id);
			mapL1R1(id);
			mapTHUMBS(id);
			mapSelectStart(id);

			//deviceMappings[KeyEvent.KEYCODE_BACK][id] = SELECT_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BACK][id] = EXIT_VALUE;

			desc = "Sixaxis";

			detected = true;
		} else if (name.contains("Gamepad 0") || name.contains("Gamepad 1") //Sixaxis Controller
			|| name.contains("Gamepad 2")) {

			deviceMappings[KeyEvent.KEYCODE_BUTTON_B][id] = A_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_X][id] = C_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_A][id] = B_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_Y][id] = D_VALUE;

			mapDPAD(id);
			mapL1R1(id);
			mapTHUMBS(id);
			mapSelectStart(id);

			desc = "Gamepad";

			detected = true;
		} else if (name.contains("nvidia_joypad") || name.contains("NVIDIA Controller")) {

			deviceMappings[KeyEvent.KEYCODE_BUTTON_A][id] = B_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_B][id] = A_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_X][id] = C_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_Y][id] = D_VALUE;

			mapL1R1(id);
			mapTHUMBS(id);

			//deviceMappings[KeyEvent.KEYCODE_BACK][id] = SELECT_VALUE;
			//deviceMappings[KeyEvent.KEYCODE_BUTTON_START] [id]= START_VALUE;


			deviceMappings[KeyEvent.KEYCODE_BUTTON_START][id] = OPTION_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BACK][id] = EXIT_VALUE;

			detected = true;

			desc = "NVIDIA Shield";
		} else if (name.contains("ipega Extending")) {

			deviceMappings[KeyEvent.KEYCODE_BUTTON_A][id] = B_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_B][id] = A_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_X][id] = C_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_Y][id] = D_VALUE;

			mapL1R1(id);
			mapTHUMBS(id);

			deviceMappings[KeyEvent.KEYCODE_BUTTON_START][id] = OPTION_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_SELECT][id] = EXIT_VALUE;

			detected = true;

			desc = "Ipega Extending Game";
		}
		//else if (name.indexOf("X-Box 360")!=-1 || name.indexOf("X-Box")!=-1
		//		   || name.indexOf("Xbox 360 Wireless Receiver")!=-1 || name.indexOf("Xbox Wireless")!=-1 ){
		else if (name.contains("X-Box") || name.contains("Xbox")) {


			deviceMappings[KeyEvent.KEYCODE_BUTTON_A][id] = B_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_B][id] = A_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_X][id] = C_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_Y][id] = D_VALUE;

			mapDPAD(id);
			mapL1R1(id);
			mapTHUMBS(id);
			mapSelectStart(id);

			deviceMappings[KeyEvent.KEYCODE_BACK][id] = EXIT_VALUE;

			desc = "XBox";

			detected = true;
		} else if (name.contains("Logitech") && name.contains("Dual Action")) {

			deviceMappings[KeyEvent.KEYCODE_BUTTON_A][id] = C_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_B][id] = D_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_X][id] = B_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_Y][id] = A_VALUE;

			mapL1R1(id);
			mapTHUMBS(id);
			mapSelectStart(id);

			desc = "Dual Action";

			detected = true;
		} else if (name.contains("Logitech") && name.contains("RumblePad 2")) {

			deviceMappings[KeyEvent.KEYCODE_BUTTON_2][id] = B_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_1][id] = C_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_3][id] = A_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_4][id] = D_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_9][id] = COIN_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_10][id] = START_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_5][id] = E_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_6][id] = F_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_11][id] = OPTION_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_12][id] = EXIT_VALUE;

			desc = "Rumblepad 2";

			detected = true;

		} else if (name.contains("Logitech") && name.contains("Precision")) {

			deviceMappings[KeyEvent.KEYCODE_BUTTON_2][id] = B_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_1][id] = C_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_3][id] = A_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_4][id] = D_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_5][id] = E_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_6][id] = F_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_7][id] = OPTION_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_8][id] = EXIT_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_9][id] = COIN_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_10][id] = START_VALUE;

			desc = "Logitech Precision";

			detected = true;
		} else if (name.contains("TTT THT Arcade console 2P USB Play")) {

			deviceMappings[KeyEvent.KEYCODE_BUTTON_1][id] = C_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_2][id] = D_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_5][id] = B_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_6][id] = A_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_3][id] = E_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_7][id] = F_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_4][id] = OPTION_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_8][id] = COIN_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_9][id] = START_VALUE;

			desc = "TTT THT Arcade";

			detected = true;
		} else if (name.contains("TOMMO NEOGEOX Arcade Stick")) {

			mapDPAD(id);

			deviceMappings[KeyEvent.KEYCODE_BUTTON_A][id] = B_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_B][id] = A_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_X][id] = C_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_C][id] = D_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_R2][id] = COIN_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_L2][id] = START_VALUE;

			desc = "TOMMO Neogeo X Arcade";

			detected = true;
		} else if (name.contains("Onlive Wireless Controller")) {

			mapDPAD(id);

			deviceMappings[KeyEvent.KEYCODE_BUTTON_Y][id] = D_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_X][id] = C_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_A][id] = B_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_B][id] = A_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_L1][id] = E_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_R1][id] = F_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_L1][id] = COIN_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BACK][id] = START_VALUE;

			desc = "Onlive Wireless";

			detected = true;
		} else if (name.contains("MadCatz") && name.contains("PC USB Wired Stick")) {

			deviceMappings[KeyEvent.KEYCODE_BUTTON_A][id] = C_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_B][id] = B_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_C][id] = A_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_X][id] = D_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_Y][id] = E_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_Z][id] = E_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_L1][id] = E_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_R1][id] = F_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_L2][id] = COIN_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_R2][id] = START_VALUE;

			desc = "Madcatz PC USB Stick";

			detected = true;
		} else if (name.contains("Logicool") && name.contains("RumblePad 2")) {

			deviceMappings[KeyEvent.KEYCODE_BUTTON_B][id] = B_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_C][id] = A_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_X][id] = D_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_A][id] = C_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_Y][id] = E_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_Z][id] = F_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_L1][id] = OPTION_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_R1][id] = EXIT_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_L2][id] = COIN_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_R2][id] = START_VALUE;

			desc = "Logicool Rumblepad 2";

			detected = true;
		} else if (name.contains("Zeemote") && name.contains("Steelseries free")) {

			deviceMappings[KeyEvent.KEYCODE_BUTTON_A][id] = B_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_B][id] = A_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_Y][id] = D_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_X][id] = C_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_MODE][id] = COIN_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_START][id] = START_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_L1][id] = E_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_R1][id] = F_VALUE;

			desc = "Zeemote Steelseries";

			detected = true;
		} else if (name.contains("HuiJia  USB GamePad")) {

			deviceMappings[KeyEvent.KEYCODE_BUTTON_3][id] = B_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_4][id] = C_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_2][id] = A_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_1][id] = D_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_7][id] = E_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_8][id] = F_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_9][id] = COIN_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_10][id] = START_VALUE;

			desc = "Huijia USB SNES";

			detected = true;
		} else if (name.contains("Smartjoy Family Super Smartjoy 2")) {

			deviceMappings[KeyEvent.KEYCODE_BUTTON_3][id] = B_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_4][id] = C_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_2][id] = A_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_1][id] = D_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_7][id] = E_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_8][id] = F_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_5][id] = COIN_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_6][id] = START_VALUE;

			desc = "Super Smartjoy";

			detected = true;
		} else if (name.contains("Jess Tech Dual Analog Rumble Pad")) {

			deviceMappings[KeyEvent.KEYCODE_BUTTON_3][id] = B_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_1][id] = C_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_4][id] = A_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_2][id] = D_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_5][id] = E_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_7][id] = F_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_6][id] = OPTION_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_8][id] = EXIT_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_11][id] = COIN_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_12][id] = START_VALUE;

			//desc = "Super Smartjoy";

			detected = true;
		} else if (name.contains("Microsoft") && name.contains("Dual Strike")) {

			deviceMappings[KeyEvent.KEYCODE_BUTTON_4][id] = B_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_2][id] = C_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_3][id] = A_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_1][id] = D_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_7][id] = E_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_8][id] = F_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_9][id] = OPTION_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_6][id] = COIN_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_5][id] = START_VALUE;

			desc = "MS Dual Strike";

			detected = true;
		} else if (name.contains("Microsoft") && name.contains("SideWinder")) {

			deviceMappings[KeyEvent.KEYCODE_BUTTON_A][id] = B_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_X][id] = C_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_B][id] = A_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_Y][id] = D_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_L1][id] = E_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_R1][id] = F_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_Z][id] = OPTION_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_C][id] = EXIT_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_11][id] = COIN_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_12][id] = START_VALUE;

			desc = "MS Sidewinder";

			detected = true;
		} else if (name.contains("WiseGroup") &&
			(name.contains("JC-PS102U") || name.contains("TigerGame")) ||
			name.contains("Game Controller Adapter") || name.contains("Dual USB Joypad") ||
			name.contains("Twin USB Joystick")
		) {

			deviceMappings[KeyEvent.KEYCODE_BUTTON_13][id] = UP_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_15][id] = DOWN_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_16][id] = LEFT_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_14][id] = RIGHT_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_4][id] = C_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_1][id] = D_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_3][id] = B_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_2][id] = A_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_7][id] = E_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_8][id] = F_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_5][id] = OPTION_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_6][id] = EXIT_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_10][id] = COIN_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_9][id] = START_VALUE;

			desc = "PlayStation2";

			detected = true;
		} else if (name.contains("MOGA") || name.contains("Moga")) {

			mapDPAD(id);

			deviceMappings[KeyEvent.KEYCODE_BUTTON_A][id] = B_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_X][id] = C_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_B][id] = A_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_Y][id] = D_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_L1][id] = E_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_R1][id] = F_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_SELECT][id] = COIN_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_START][id] = START_VALUE;

			desc = "MOGA";

			detected = true;
		} else if (name.contains("OUYA Game Controller")) {

			mapDPAD(id);

			deviceMappings[KeyEvent.KEYCODE_BUTTON_Y][id] = D_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_X][id] = C_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_B][id] = A_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_A][id] = B_VALUE;

			deviceMappings[KeyEvent.KEYCODE_MENU][id] = OPTION_VALUE;

			mapL1R1(id);
			//mapL2R2(id);
			mapTHUMBS(id);

			desc = "OUYA";

			detected = true;
		} else if (name.contains("DragonRise")) {

			mapDPAD(id);

			deviceMappings[KeyEvent.KEYCODE_BUTTON_2][id] = A_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_3][id] = B_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_4][id] = C_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_1][id] = D_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_5][id] = E_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_6][id] = F_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_7][id] = COIN_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_8][id] = START_VALUE;

			desc = "DragonRise";

			detected = true;
		} else if (name.contains("Thrustmaster T Mini")) {

			mapDPAD(id);

			deviceMappings[KeyEvent.KEYCODE_BUTTON_A][id] = C_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_X][id] = D_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_B][id] = B_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_C][id] = A_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_Y][id] = E_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_Z][id] = F_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_L2][id] = OPTION_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_R1][id] = EXIT_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_R2][id] = COIN_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_L2][id] = START_VALUE;

			desc = "Thrustmaster T Mini";

			detected = true;
		} else if (name.contains("ADC joystick")) {

			deviceMappings[KeyEvent.KEYCODE_BUTTON_B][id] = B_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_A][id] = A_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_Y][id] = C_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_X][id] = D_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_L2][id] = E_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_R2][id] = F_VALUE;

			mapDPAD(id);
			mapL1R1(id);

			deviceMappings[KeyEvent.KEYCODE_BUTTON_SELECT][id] = COIN_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_START][id] = START_VALUE;

			desc = "JXD S7800";
			detected = true;
		} else if (name.contains("Green Throttle Atlas")) {

			deviceMappings[KeyEvent.KEYCODE_BUTTON_A][id] = B_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_B][id] = A_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_X][id] = C_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_Y][id] = D_VALUE;

			mapDPAD(id);
			mapL1R1(id);
			mapTHUMBS(id);
			mapSelectStart(id);

			deviceMappings[KeyEvent.KEYCODE_BACK][id] = EXIT_VALUE;

			desc = "Green Throttle";
			detected = true;
		} else if (name.contains("joy_key") && mm.getMainHelper().getDeviceDetected() == MainHelper.DEVICE_AGAMEPAD2) {

			deviceMappings[KeyEvent.KEYCODE_BUTTON_B][id] = A_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_X][id] = C_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_A][id] = B_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_Y][id] = D_VALUE;

			deviceMappings[KeyEvent.KEYCODE_BUTTON_L2][id] = E_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_R2][id] = F_VALUE;

			mapDPAD(id);
			mapL1R1(id);

			deviceMappings[KeyEvent.KEYCODE_BUTTON_SELECT][id] = COIN_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_START][id] = START_VALUE;

			desc = "Archos Gamepad 2";
			detected = true;
		} else if (name.contains("NYKO PLAYPAD") ||
			(name.contains("Broadcom Bluetooth HID") && mm.getMainHelper().getDeviceDetected() == MainHelper.DEVICE_SHIELD)) {

			deviceMappings[KeyEvent.KEYCODE_BUTTON_A][id] = B_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_B][id] = A_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_X][id] = C_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BUTTON_Y][id] = D_VALUE;

			mapL1R1(id);
			mapTHUMBS(id);

			deviceMappings[KeyEvent.KEYCODE_BUTTON_START][id] = OPTION_VALUE;
			deviceMappings[KeyEvent.KEYCODE_BACK][id] = EXIT_VALUE;

			detected = true;

			desc = "NYKO PLAYPAD";
		} else if (name.contains("BSP-D8")) {


				deviceMappings[KeyEvent.KEYCODE_BUTTON_A][id] = B_VALUE;
				deviceMappings[KeyEvent.KEYCODE_BUTTON_B][id] = A_VALUE;
				deviceMappings[KeyEvent.KEYCODE_BUTTON_X][id] = C_VALUE;
				deviceMappings[KeyEvent.KEYCODE_BUTTON_Y][id] = D_VALUE;

				mapDPAD(id);
				mapL1R1(id);
				mapTHUMBS(id);

				//mapSelectStart(id);
			    deviceMappings[KeyEvent.KEYCODE_BUTTON_SELECT][id] = OPTION_VALUE ;
			    deviceMappings[KeyEvent.KEYCODE_BUTTON_START][id] = EXIT_VALUE;

				deviceMappings[KeyEvent.KEYCODE_BACK][id] = EXIT_VALUE;

				desc = "BSP-D8";

				detected = true;
		}

		//JOYPAD_B = X_VALUE
		//JOYPAD_Y = A_VALUE
		//JOYPAD_A = B_VALUE
		//JOYPAD_X = Y_VALUE

		if (detected) {
			System.out.println("Controller detected: " + device.getName());
			deviceIDs[id] = device.getId();
			id++;
			if (id == 1)
				mm.getMainHelper().updateMAME4droid();

			CharSequence text = "Detected " + desc + " controller as P" + id;
			new WarnWidget.WarnWidgetHelper(mm, text.toString(), 3, Color.GREEN, true);

			return id - 1;
		} else {
			banDev.append(device.getId(), 1);
		}

		return -1;
	}

	public boolean isEnabled(){
		int numDevs = 0;
		for (int i = 0; i < MAX_DEVICES; i++) {
			if (deviceIDs[i] != -1)
				numDevs++;
		}
		return numDevs != 0 || joystickMotion==true;
	}

}
