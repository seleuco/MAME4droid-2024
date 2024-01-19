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

import java.util.ArrayList;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.seleuco.mame4droid.Emulator;
import com.seleuco.mame4droid.MAME4droid;
import com.seleuco.mame4droid.R;
import com.seleuco.mame4droid.helpers.PrefsHelper;
import com.seleuco.mame4droid.input.ControlCustomizer;
import com.seleuco.mame4droid.input.IController;
import com.seleuco.mame4droid.input.InputHandler;
import com.seleuco.mame4droid.input.InputValue;
import com.seleuco.mame4droid.input.TiltSensor;
import com.seleuco.mame4droid.input.TouchController;

public class InputView extends ImageView {

    protected MAME4droid mm = null;
    protected Bitmap bmp = null;
    protected Paint pnt = new Paint();
    protected Rect rsrc = new Rect();
    protected Rect rdst = new Rect();
    protected Rect rclip = new Rect();
    protected int ax = 0;
    protected int ay = 0;
    protected float dx = 1;
    protected float dy = 1;

    static BitmapDrawable stick_images[] = null;
    static BitmapDrawable btns_images[][] = null;

    public void setMAME4droid(MAME4droid mm) {
        this.mm = mm;
        if (mm == null) return;

        if (stick_images == null) {
            stick_images = new BitmapDrawable[9];
            stick_images[IController.STICK_DOWN] = (BitmapDrawable) mm.getResources().getDrawable(R.drawable.dpad_down);
            stick_images[IController.STICK_DOWN_LEFT] = (BitmapDrawable) mm.getResources().getDrawable(R.drawable.dpad_down_left);
            stick_images[IController.STICK_DOWN_RIGHT] = (BitmapDrawable) mm.getResources().getDrawable(R.drawable.dpad_down_right);
            stick_images[IController.STICK_LEFT] = (BitmapDrawable) mm.getResources().getDrawable(R.drawable.dpad_left);
            stick_images[IController.STICK_NONE] = (BitmapDrawable) mm.getResources().getDrawable(R.drawable.dpad_none);
            stick_images[IController.STICK_RIGHT] = (BitmapDrawable) mm.getResources().getDrawable(R.drawable.dpad_right);
            stick_images[IController.STICK_UP] = (BitmapDrawable) mm.getResources().getDrawable(R.drawable.dpad_up);
            stick_images[IController.STICK_UP_LEFT] = (BitmapDrawable) mm.getResources().getDrawable(R.drawable.dpad_up_left);
            stick_images[IController.STICK_UP_RIGHT] = (BitmapDrawable) mm.getResources().getDrawable(R.drawable.dpad_up_right);
        }

        if (btns_images == null) {
            btns_images = new BitmapDrawable[IController.NUM_BUTTONS][2];

            btns_images[IController.BTN_A][IController.BTN_NO_PRESS_STATE]
                    = (BitmapDrawable) mm.getResources().getDrawable(R.drawable.button_a);
            btns_images[IController.BTN_A][IController.BTN_PRESS_STATE]
                    = (BitmapDrawable) mm.getResources().getDrawable(R.drawable.button_a_press);

            btns_images[IController.BTN_B][IController.BTN_NO_PRESS_STATE]
                    = (BitmapDrawable) mm.getResources().getDrawable(R.drawable.button_b);
            btns_images[IController.BTN_B][IController.BTN_PRESS_STATE]
                    = (BitmapDrawable) mm.getResources().getDrawable(R.drawable.button_b_press);

            btns_images[IController.BTN_C][IController.BTN_NO_PRESS_STATE]
                    = (BitmapDrawable) mm.getResources().getDrawable(R.drawable.button_c);
            btns_images[IController.BTN_C][IController.BTN_PRESS_STATE]
                    = (BitmapDrawable) mm.getResources().getDrawable(R.drawable.button_c_press);

            btns_images[IController.BTN_D][IController.BTN_NO_PRESS_STATE]
                    = (BitmapDrawable) mm.getResources().getDrawable(R.drawable.button_d);
            btns_images[IController.BTN_D][IController.BTN_PRESS_STATE]
                    = (BitmapDrawable) mm.getResources().getDrawable(R.drawable.button_d_press);

            btns_images[IController.BTN_E][IController.BTN_NO_PRESS_STATE]
                    = (BitmapDrawable) mm.getResources().getDrawable(R.drawable.button_e);
            btns_images[IController.BTN_E][IController.BTN_PRESS_STATE]
                    = (BitmapDrawable) mm.getResources().getDrawable(R.drawable.button_e_press);

            btns_images[IController.BTN_F][IController.BTN_NO_PRESS_STATE]
                    = (BitmapDrawable) mm.getResources().getDrawable(R.drawable.button_f);
            btns_images[IController.BTN_F][IController.BTN_PRESS_STATE]
                    = (BitmapDrawable) mm.getResources().getDrawable(R.drawable.button_f_press);

            btns_images[IController.BTN_EXIT][IController.BTN_NO_PRESS_STATE]
                    = (BitmapDrawable) mm.getResources().getDrawable(R.drawable.button_exit);
            btns_images[IController.BTN_EXIT][IController.BTN_PRESS_STATE]
                    = (BitmapDrawable) mm.getResources().getDrawable(R.drawable.button_exit_press);

            btns_images[IController.BTN_OPTION][IController.BTN_NO_PRESS_STATE]
                    = (BitmapDrawable) mm.getResources().getDrawable(R.drawable.button_option);
            btns_images[IController.BTN_OPTION][IController.BTN_PRESS_STATE]
                    = (BitmapDrawable) mm.getResources().getDrawable(R.drawable.button_option_press);

            btns_images[IController.BTN_START][IController.BTN_NO_PRESS_STATE]
                    = (BitmapDrawable) mm.getResources().getDrawable(R.drawable.button_start);
            btns_images[IController.BTN_START][IController.BTN_PRESS_STATE]
                    = (BitmapDrawable) mm.getResources().getDrawable(R.drawable.button_start_press);

            btns_images[IController.BTN_COIN][IController.BTN_NO_PRESS_STATE]
                    = (BitmapDrawable) mm.getResources().getDrawable(R.drawable.button_coin);
            btns_images[IController.BTN_COIN][IController.BTN_PRESS_STATE]
                    = (BitmapDrawable) mm.getResources().getDrawable(R.drawable.button_coin_press);
        }
    }

    public InputView(Context context) {
        super(context);
        init();
    }

    public InputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public InputView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    protected void init() {
        pnt.setARGB(255, 255, 255, 255);
        //p.setTextSize(25);
        pnt.setStyle(Style.STROKE);

        pnt.setARGB(255, 255, 255, 255);
        pnt.setTextSize(16);

        this.setFocusable(true);
        this.setFocusableInTouchMode(true);
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        if (drawable != null) {
            BitmapDrawable bmpdrw = (BitmapDrawable) drawable;
            bmp = bmpdrw.getBitmap();
        } else {
            bmp = null;
        }

        super.setImageDrawable(drawable);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        if (mm == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int widthSize = 1;
        int heightSize = 1;

        if (mm.getMainHelper().getscrOrientation() == Configuration.ORIENTATION_LANDSCAPE) {

            /*if(!mm.getPrefsHelper().isNotchUsed())
            {
                widthSize = mm.getWindowManager().getDefaultDisplay().getWidth();
                heightSize = mm.getWindowManager().getDefaultDisplay().getHeight();
            }
            else {*/
                widthSize = MeasureSpec.getSize(widthMeasureSpec);
                heightSize = MeasureSpec.getSize(heightMeasureSpec);
            //}

        } else {
            int w = 1;//320;
            int h = 1;//240;

            if (mm != null && mm.getInputHandler().getTouchController().getMainRect() != null) {
                w = mm.getInputHandler().getTouchController().getMainRect().width();
                h = mm.getInputHandler().getTouchController().getMainRect().height();
            }

            if (w == 0) w = 1;
            if (h == 0) h = 1;

            float desiredAspect = (float) w / (float) h;

            widthSize = mm.getWindowManager().getDefaultDisplay().getWidth();
            heightSize = (int) (widthSize / desiredAspect);
        }

        setMeasuredDimension(widthSize, heightSize);
    }

    public void updateImages() {
        ArrayList<InputValue> data = mm.getInputHandler().getTouchController().getAllInputData();

        if (data == null) return;

        for (int i = 0; i < data.size(); i++) {
            InputValue v = data.get(i);
            if (v.getType() == TouchController.TYPE_STICK_IMG) {

                for (int j = 0; j < stick_images.length; j++) {
                    stick_images[j].setBounds(v.getRect());
                    stick_images[j].setAlpha(mm.getInputHandler().getTouchController().getOpacity());
                }
            } else if (v.getType() == TouchController.TYPE_BUTTON_IMG) {
                btns_images[v.getValue()][IController.BTN_PRESS_STATE].setBounds(v.getRect());
                btns_images[v.getValue()][IController.BTN_PRESS_STATE].setAlpha(mm.getInputHandler().getTouchController().getOpacity());
                btns_images[v.getValue()][IController.BTN_NO_PRESS_STATE].setBounds(v.getRect());
                btns_images[v.getValue()][IController.BTN_NO_PRESS_STATE].setAlpha(mm.getInputHandler().getTouchController().getOpacity());
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {

        super.onSizeChanged(w, h, oldw, oldh);

        int bw = 1;
        int bh = 1;

        if (mm != null && mm.getInputHandler().getTouchController().getMainRect() != null) {
            bw = mm.getInputHandler().getTouchController().getMainRect().width();
            bh = mm.getInputHandler().getTouchController().getMainRect().height();
        }

        if (bw == 0) bw = 1;
        if (bh == 0) bh = 1;

        float desiredAspect = (float) bw / (float) bh;

        int tmp = (int) ((float) w / desiredAspect);
        if (tmp <= h) {
            ax = 0;
            ay = (h - tmp) / 2;
            h = tmp;
        } else {
            tmp = (int) ((float) h * desiredAspect);
            ay = 0;
            ax = (w - tmp) / 2;
            w = tmp;
        }

        dx = (float) w / (float) bw;
        dy = (float) h / (float) bh;

        if (mm == null || mm.getInputHandler() == null)
            return;

        mm.getInputHandler().getTouchController().setFixFactor(ax, ay, dx, dy);

        updateImages();

        //mm.getDialogHelper().setInfoMsg("w:"+w+"h:"+h);
        //mm.showDialog(DialogHelper.DIALOG_INFO);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if (bmp != null)
            super.onDraw(canvas);

        if (mm == null) return;

        ArrayList<InputValue> data = mm.getInputHandler().getTouchController().getAllInputData();

        if (data == null) return;

        for (int i = 0; i < data.size(); i++) {
            InputValue v = data.get(i);
            BitmapDrawable d = null;
            canvas.getClipBounds(rclip);
			boolean handled = mm.getInputHandler().getTouchController().isHandledTouchItem(v);
			if(!handled) {continue;}
            if (v.getType() == TouchController.TYPE_STICK_IMG && rclip.intersect(v.getRect())) {
                 d = stick_images[mm.getInputHandler().getTouchController().getStick_state()];
            } else if (v.getType() == TouchController.TYPE_ANALOG_RECT && rclip.intersect(v.getRect())) {
                 mm.getInputHandler().getTouchStick().draw(canvas);
            } else if (v.getType() == TouchController.TYPE_BUTTON_IMG && rclip.intersect(v.getRect())) {
                 d = btns_images[v.getValue()][mm.getInputHandler().getTouchController().getBtnStates()[v.getValue()]];
            }

            if (d != null) {
                //d.setBounds(v.getRect());
                d.draw(canvas);
            }
        }

        if (ControlCustomizer.isEnabled())
            mm.getInputHandler().getControlCustomizer().draw(canvas);

        if (Emulator.isDebug()) {
            ArrayList<InputValue> ids = mm.getInputHandler().getTouchController().getAllInputData();
            Paint p2 = new Paint();
            p2.setARGB(255, 255, 255, 255);
            p2.setStyle(Style.STROKE);

            for (int i = 0; i < ids.size(); i++) {
                InputValue v = ids.get(i);
                Rect r = v.getRect();
                if (r != null) {

                    if (v.getType() == TouchController.TYPE_BUTTON_RECT)
                        canvas.drawRect(r, p2);
                    else if (mm.getPrefsHelper().getControllerType() == PrefsHelper.PREF_DIGITAL_DPAD && v.getType() == TouchController.TYPE_STICK_RECT)
                        canvas.drawRect(r, p2);
                    else if (mm.getPrefsHelper().getControllerType() != PrefsHelper.PREF_DIGITAL_DPAD && v.getType() == TouchController.TYPE_ANALOG_RECT)
                        canvas.drawRect(r, p2);
                }
            }

            p2.setTextSize(30);
            if (mm.getInputHandler().getTiltSensor().isEnabled() && TiltSensor.str != null)
                canvas.drawText(TiltSensor.str, 100, 150, p2);
        }
    }
}
