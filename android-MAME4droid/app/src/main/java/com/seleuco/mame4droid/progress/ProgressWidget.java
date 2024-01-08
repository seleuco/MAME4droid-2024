package com.seleuco.mame4droid.progress;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.seleuco.mame4droid.MAME4droid;
import com.seleuco.mame4droid.R;

public class ProgressWidget {

	protected MAME4droid mm;

	protected String title = null;
	protected String initMsg = null;
	protected long initTime;
	protected long lastTime;
	protected TextView textView = null;
	protected LinearLayout parent = null;
	protected boolean init = false;
	protected boolean added = false;

	protected int orientation;

	public ProgressWidget(MAME4droid mm, String title, String initMsg){
		this.mm = mm;
		this.title = title;
		this.initMsg = initMsg;
	}

	public void init(){

		initTime = lastTime = System.currentTimeMillis();

		init = true;

		orientation = mm.getRequestedOrientation();

		mm.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

		mm.runOnUiThread(new Runnable() {
			public void run() {

				FrameLayout frame = mm.findViewById(R.id.EmulatorFrame);

				textView = new TextView(mm);

				parent = new LinearLayout(mm);
				parent.setLayoutParams(
					new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
				parent.setOrientation(LinearLayout.HORIZONTAL);
				parent.setGravity(Gravity.CENTER);

				textView.setBackgroundResource(R.drawable.border_shape);
				textView.setTextColor(Color.WHITE);

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
				mm.setRequestedOrientation(orientation);
			}
		});

	}
}
