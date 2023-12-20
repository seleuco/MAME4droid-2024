package com.seleuco.mame4droid.progress;

import android.app.ProgressDialog;

import com.seleuco.mame4droid.MAME4droid;

public class ProgressWidget {

	protected MAME4droid mm;

	protected String title = null;
	protected String initMsg = null;
	protected long initTime;
	protected long lastTime;
	protected ProgressDialog progressDialog = null;
	protected boolean init = false;

	protected int atLeastTime = 0;

	public ProgressWidget(MAME4droid mm, String title, String initMsg, int atLeastTime){
		this.mm = mm;
		this.title = title;
		this.initMsg = initMsg;
		this.atLeastTime = atLeastTime;
	}

	public ProgressWidget(MAME4droid mm, String title, String initMsg){
		this(mm,title,initMsg,0);
	}
	public void init(){

		initTime = lastTime = System.currentTimeMillis();

		init = true;
		mm.runOnUiThread(new Runnable() {
			public void run() {
				progressDialog = progressDialog = ProgressDialog.show(mm, title,
					initMsg, true, false);
			}
		});
	}
	public void notifyText(String msg){
		long currTime = System.currentTimeMillis();

		if(currTime - initTime < atLeastTime)
			return;

		if(currTime - lastTime > 40 && progressDialog !=null) {
			mm.runOnUiThread(new Runnable() {
				public void run() {
					try {
						progressDialog.setMessage(msg);
					}catch(NullPointerException e){}
					lastTime = System.currentTimeMillis();
				}
			});
		}
	}

	public void end() {

		if(!init) return;

		while (progressDialog == null) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
			}
		}

		if(progressDialog.isShowing())
		{

			long lastTime = System.currentTimeMillis();
			if(lastTime - initTime < atLeastTime) //at least show this time
			{
				try {
					Thread.sleep(atLeastTime - ( lastTime - initTime));
				} catch (InterruptedException e) {
				}
			}
			progressDialog.dismiss();
		}

		progressDialog = null;
	}
}
