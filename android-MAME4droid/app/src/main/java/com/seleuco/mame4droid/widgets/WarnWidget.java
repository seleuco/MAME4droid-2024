package com.seleuco.mame4droid.widgets;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.seleuco.mame4droid.MAME4droid;
import com.seleuco.mame4droid.R;

public class WarnWidget {

	public static class WarnWidgetHelper extends Thread{
		WarnWidget warnWidget;
		int time;

		public WarnWidgetHelper(MAME4droid mm, String msg,int time,int color, boolean bottom){
			warnWidget = new WarnWidget(mm, "", msg,color,bottom,false);
			this.time = time;
			warnWidget.init();
			this.start();
		}

		@Override
		public void run() {
			try {
				Thread.sleep(1000*time);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			warnWidget.end();
		}
	}

	protected MAME4droid mm;

	protected String title = null;
	protected String initMsg = null;
	protected int color;
	protected long initTime;
	protected long lastTime;
	protected TextView textView = null;
	protected LinearLayout parent = null;
	protected boolean init = false;
	protected boolean added = false;
	protected boolean lockOrientation = true;
	protected boolean bottom = false;

	protected int orientation;

	public WarnWidget(MAME4droid mm, String title, String initMsg,int color,boolean bottom, boolean lock) {
	   this.mm = mm;
	   this.title = title;
	   this.initMsg = initMsg;
	   this.color = color;
	   this.bottom = bottom;
		this.lockOrientation = lock;
	}

	public void init(){

		initTime = lastTime = System.currentTimeMillis();

		init = true;

		if(this.lockOrientation) {
			orientation = mm.getMainHelper().getScreenOrientation();
			mm.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
		}

		mm.runOnUiThread(new Runnable() {
			public void run() {

				FrameLayout frame = mm.findViewById(R.id.EmulatorFrame);

				textView = new TextView(mm);

				parent = new LinearLayout(mm);
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
				parent.setLayoutParams(params);

				float px = 50 * ((float) mm.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);

				params.setMargins((int)px,(int)px,(int)px,(int)px);
				parent.setOrientation(LinearLayout.HORIZONTAL);

				if(bottom) {
					parent.setGravity(Gravity.BOTTOM | Gravity.CENTER);
					//parent.setPadding(0,0,0,100);
				}
				else
					parent.setGravity(Gravity.CENTER);

				textView.setBackgroundResource(R.drawable.border_shape);
				textView.setTextColor(color);
				textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);



				parent.addView(textView);
				frame.addView(parent);

				textView.setText(title+" "+initMsg);

				added = true;
			}
		});
	}
	public void notifyText(String msg){
		long currTime = System.currentTimeMillis();

		if(currTime - lastTime > 40 && textView !=null) {
			mm.runOnUiThread(new Runnable() {
				public void run() {
					try {
						textView.setText(msg);

					}catch(NullPointerException e){}
					lastTime = System.currentTimeMillis();
				}
			});
		}
	}

	public void end() {

		if (!init) return;

		while (!added) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
			}
		}

		mm.runOnUiThread(new Runnable() {
			public void run() {

				FrameLayout frame = mm.findViewById(R.id.EmulatorFrame);
				frame.removeView(parent);
				textView = null;
				parent = null;
				if(lockOrientation)
				   mm.setRequestedOrientation(orientation);
			}
		});

	}
}
