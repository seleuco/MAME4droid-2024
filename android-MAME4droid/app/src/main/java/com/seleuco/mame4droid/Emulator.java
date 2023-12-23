/*
 * This file is part of MAME4droid.
 *
 * Copyright (C) 2015 David Valdeita (Seleuco)
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

package com.seleuco.mame4droid;

import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Toast;

import com.seleuco.mame4droid.helpers.DialogHelper;
import com.seleuco.mame4droid.helpers.PrefsHelper;
import com.seleuco.mame4droid.input.TouchController;
import com.seleuco.mame4droid.views.EmulatorViewGL;

import java.io.File;
import java.nio.ByteBuffer;

public class Emulator {

	//gets
	final static public int IN_MENU = 1;
	final static public int IN_GAME = 2;
	final static public int NUMBTNS = 3;
	final static public int NUMWAYS = 4;
	final static public int IS_LIGHTGUN = 5;

	//sets
	final static public int EXIT_GAME = 1;

	final static public int EXIT_PAUSE = 2;
    final static public int SHOW_FPS = 3;

	final static public int AUTO_FRAMESKIP = 4;
	final static public int CHEATS = 5;
	final static public int SKIP_GAMEINFO = 6;

	final static public int DISABLE_DRC = 7;

	final static public int DRC_USE_C = 8;

	final static public int SIMPLE_UI = 9;

    final static public int PAUSE = 11;
    final static public int SOUND_VALUE = 13;

    final static public int AUTOSAVE = 16;
    final static public int SAVESTATE = 17;
    final static public int LOADSTATE = 18;

    final static public int EMU_RESOLUTION = 20;
    final static public int DOUBLE_BUFFER = 23;
    final static public int PXASP1 = 24;

    final static public int VBEAM2X = 34;
    final static public int VFLICKER = 36;
    final static public int SOUND_OPTIMAL_FRAMES = 48;
    final static public int SOUND_OPTIMAL_SAMPLERATE = 49;
    final static public int SOUND_ENGINE = 50;

    final static public int MOUSE = 60;
    final static public int REFRESH = 61;
    final static public int USING_SAF = 62;
    final static public int SAVESATES_IN_ROM_PATH = 63;

	final static public int WARN_ON_EXIT = 64;

	final static public int IS_MOUSE = 65;

	final static public int KEYBOARD = 66;

	final static public int ONE_PROCESSOR = 67;
	final static public int NODEADZONEANDSAT = 68;

	//set str
    final static public int SAF_PATH = 1;
    final static public int ROM_NAME = 2;
    final static public int VERSION = 3;
	final static public int OVERLAY_EFECT = 4;

	//get str
	final static public int MAME_VERSION = 1;

	//KEYS ACTIONS
	final static public int KEY_DOWN = 1;
	final static public int KEY_UP = 2;

	//MOUSE ACTIONS
	final static public int MOUSE_MOVE = 1;
	final static public int MOUSE_BTN_DOWN = 2;
	final static public int MOUSE_BTN_UP = 3;


    private static MAME4droid mm = null;

    private static boolean isEmulating = false;

    public static boolean isEmulating() {
        return isEmulating;
    }

    private static Object lock1 = new Object();

    private static ByteBuffer screenBuff = null;

    private static boolean emuFiltering = false;

    public static boolean isEmuFiltering() {
        return emuFiltering;
    }

	public static void setEmuFiltering(boolean value) {
		emuFiltering = value;
	}

	private static Paint debugPaint = new Paint();

    private static Matrix mtx = new Matrix();

    private static int window_width = 320;

    public static int getWindow_width() {
        return window_width;
    }

    private static int window_height = 240;

    public static int getWindow_height() {
        return window_height;
    }

    private static int emu_width = 320;
    private static int emu_height = 240;


    private static AudioTrack audioTrack = null;

    private static boolean isDebug = false;
    private static int videoRenderMode = PrefsHelper.PREF_RENDER_GL;

    private static boolean inMenu = false;
    private static boolean oldInMenu = false;

    public static boolean isInGame() {
        return Emulator.getValue(Emulator.IN_GAME) == 1;
    }

    public static boolean isInMenu() {
        return inMenu;
    }

	public static boolean isInGameButNotInMenu() {
		return isInGame() && !isInMenu();
	}

	private static boolean saveorload = false;
	public static void setSaveorload(boolean value){saveorload=value;}
	public static boolean isSaveorload(){return saveorload;};

    private static boolean needsRestart = false;

    public static void setNeedRestart(boolean value) {
        needsRestart = value;
    }

    public static boolean isRestartNeeded() {
        return needsRestart;
    }

    private static boolean warnResChanged = false;

    public static boolean isWarnResChanged() {
        return warnResChanged;
    }

    public static void setWarnResChanged(boolean warnResChanged) {
        Emulator.warnResChanged = warnResChanged;
    }

    private static boolean paused = true;

    public static boolean isPaused() {
        return paused;
    }

    private static boolean portraitFull = false;

    public static boolean isPortraitFull() {
        return portraitFull;
    }

    public static void setPortraitFull(boolean portraitFull) {
        Emulator.portraitFull = portraitFull;
    }

    static {
        try {
            System.loadLibrary("mame4droid-jni");
        } catch (java.lang.Error e) {
            e.printStackTrace();
        }

        debugPaint.setARGB(255, 255, 255, 255);
        debugPaint.setStyle(Style.STROKE);
        debugPaint.setTextSize(16);
    }

    public static int getEmulatedWidth() {
        return emu_width;
    }

    public static int getEmulatedHeight() {
        return emu_height;
    }

    public static boolean isDebug() {
        return isDebug;
    }

    public static void setDebug(boolean isDebug) {
        Emulator.isDebug = isDebug;
    }

    public static int getVideoRenderMode() {
        return Emulator.videoRenderMode;
    }

    public static void setVideoRenderMode(int videoRenderMode) {
        Emulator.videoRenderMode = videoRenderMode;
    }

    public static Paint getDebugPaint() {
        return debugPaint;
    }

    public static Matrix getMatrix() {
        return mtx;
    }

    //synchronized
    public static ByteBuffer getScreenBuffer() {
        return screenBuff;
    }

    public static void setMAME4droid(MAME4droid mm) {
        Emulator.mm = mm;
    }

    //VIDEO
    public static void setWindowSize(int w, int h) {

        //System.out.println("window size "+w+" "+h);

        window_width = w;
        window_height = h;

        if (videoRenderMode == PrefsHelper.PREF_RENDER_GL)
            return;

        mtx.setScale((float) (window_width / (float) emu_width), (float) (window_height / (float) emu_height));
    }

    //synchronized
    static void bitblt(ByteBuffer sScreenBuff) {

        //Log.d("Thread Video", "fuera lock");
        synchronized (lock1) {
            try {
                //Log.d("Thread Video", "dentro lock");
                screenBuff = sScreenBuff;
                Emulator.inMenu = Emulator.getValue(Emulator.IN_MENU) == 1;

                if (inMenu != oldInMenu) {

					if(!inMenu && isSaveorload())
						setSaveorload(false);

                    final View v = mm.getInputView();
                    if (v != null) {
                        mm.runOnUiThread(new Runnable() {
                            public void run() {
                                v.invalidate();
                            }
                        });
                    }
                }
                oldInMenu = inMenu;

                if (videoRenderMode == PrefsHelper.PREF_RENDER_GL) {
                    ((EmulatorViewGL) mm.getEmuView()).requestRender();
                } else {
                    Log.e("Thread Video", "Renderer not supported.");
                }
                //Log.d("Thread Video", "fin lock");

            } catch (/*Throwable*/NullPointerException t) {
                Log.getStackTraceString(t);
                t.printStackTrace();
            }
        }
    }

    //synchronized
    static public void changeVideo(final int newWidth, final int newHeight) {

		Log.d("Thread Video", "changeVideo emu_width:"+emu_width+" emu_height: "+emu_height+" newWidth:"+newWidth+" newHeight: "+newHeight);
        synchronized (lock1) {

            mm.getInputHandler().resetInput();

            warnResChanged = emu_width != newWidth || emu_height != newHeight;

            //if(emu_width!=newWidth || emu_height!=newHeight)
            //{
            emu_width = newWidth;
            emu_height = newHeight;

            mtx.setScale((float) (window_width / (float) emu_width), (float) (window_height / (float) emu_height));

            if (videoRenderMode == PrefsHelper.PREF_RENDER_GL) {
                GLRenderer r = (GLRenderer) ((EmulatorViewGL) mm.getEmuView()).getRender();
                if (r != null) r.changedEmulatedSize();
            } else {
				Log.e("Thread Video", "Error renderer not supported");
            }

            mm.getMainHelper().updateEmuValues();

            mm.runOnUiThread(new Runnable() {
                public void run() {

                    //Toast.makeText(mm, "changeVideo newWidth:"+newWidth+" newHeight:"+newHeight+" newVisWidth:"+newVisWidth+" newVisHeight:"+newVisHeight,Toast.LENGTH_SHORT).show();
                    mm.overridePendingTransition(0, 0);
					/*
                    if (warnResChanged && videoRenderMode == PrefsHelper.PREF_RENDER_GL && Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR1)
                        mm.getEmuView().setVisibility(View.INVISIBLE);
					*/

                    mm.getMainHelper().updateMAME4droid();
                    if (mm.getEmuView().getVisibility() != View.VISIBLE)
                        mm.getEmuView().setVisibility(View.VISIBLE);
                }
            });
            //}
        }

    }

	static public void initInput() {
		Log.d("initInput","initInput isInGame:"+isInGame()+" isInMenu:"+isInMenu());
		mm.runOnUiThread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				if ( /*Emulator.getValue(Emulator.IN_GAME) == 1 && Emulator.getValue(Emulator.IN_MENU) == 0
					&&*/ (((mm.getPrefsHelper().isTouchLightgun() || mm.getPrefsHelper().isTouchGameMouse())
					&& mm.getInputHandler()
					.getTouchController().getState() != TouchController.STATE_SHOWING_NONE) || mm
					.getPrefsHelper().isTiltSensorEnabled())) {

					CharSequence text = "";
					if(mm.getPrefsHelper().isTiltSensorEnabled())
						text = "Tilt sensor is enabled!";
					else if(mm.getPrefsHelper().isTouchLightgun())
						text = "Touch lightgun is enabled!";
					else if(mm.getPrefsHelper().isTouchGameMouse())
						text = "Touch mouse is enabled!";

					int duration = Toast.LENGTH_SHORT;
					Toast toast = Toast.makeText(mm, text, duration);
					toast.show();
					Log.d("initInput","virtual device: "+text);
				}

				mm.getMainHelper().updateMAME4droid();
			}
		});
	}

    //SOUND
    static public void initAudio(int freq, boolean stereo) {
        int sampleFreq = freq;

        int channelConfig = stereo ? AudioFormat.CHANNEL_CONFIGURATION_STEREO : AudioFormat.CHANNEL_CONFIGURATION_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

        int bufferSize = AudioTrack.getMinBufferSize(sampleFreq, channelConfig, audioFormat);

        if (mm.getPrefsHelper().getSoundEngine() == PrefsHelper.PREF_SNDENG_AUDIOTRACK_HIGH)
            bufferSize += bufferSize / 4;

        //System.out.println("Buffer Size "+bufferSize);

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleFreq,
                channelConfig,
                audioFormat,
                bufferSize,
                AudioTrack.MODE_STREAM);

        audioTrack.play();
    }

    public static void endAudio() {
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
        }
        audioTrack = null;
    }

    public static void writeAudio(byte[] b, int sz) {
        //System.out.println("Envio "+sz+" "+audioTrack);
        if (audioTrack != null) {
			audioTrack.write(b, 0, sz);
        }
    }

    //LIVE CYCLE
    public static void pause() {
        //Log.d("EMULATOR", "PAUSE");

        if (isEmulating) {
            //pauseEmulation(true);
            Emulator.setValue(Emulator.PAUSE, 1);
            paused = true;
        }

        if (audioTrack != null) {
            try {
                audioTrack.pause();
            } catch (Throwable e) {
            }
        }

        try {
            Thread.sleep(60);//ensure threads stop
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public static void resume() {
        //Log.d("EMULATOR", "RESUME");

        if (isRestartNeeded())
            return;

        if (audioTrack != null)
            audioTrack.play();

        if (isEmulating) {
            Emulator.setValue(Emulator.PAUSE, 0);
            Emulator.setValue(Emulator.EXIT_PAUSE, 1);
            paused = false;
        }
    }

    //EMULATOR
    public static void emulate(final String libPath, final String resPath) {

        //Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

        if (isEmulating) return;

        Thread t = new Thread(new Runnable() {

            public void run() {

                boolean extROM = false;
                isEmulating = true;
				Size sz = mm.getMainHelper().getWindowSize();
                init(libPath, resPath, Math.max(sz.getWidth(),sz.getHeight()), Math.min(sz.getWidth(),sz.getHeight()));
                final String versionName = mm.getMainHelper().getVersion();
                Emulator.setValueStr(Emulator.VERSION, versionName);
                Emulator.setValue(Emulator.USING_SAF, mm.getSAFHelper().isUsingSAF() ? 1 : 0);
                Intent intent = mm.getIntent();
                String action = intent.getAction();
                //Uri pkg = null;
                String fileName = null;
                String path = null;
                boolean delete = false;
                if (Intent.ACTION_VIEW.equals(action)) {
                    //android.os.Debug.waitForDebugger();
                    //pkg = mm.getReferrer();
                    //System.out.println("PKG: "+pkg.getHost());

                    Uri _uri = intent.getData();
                    //System.out.println("URI: "+_uri.toString());
                    boolean error = false;

                    //String filePath = null;

                    //android.os.Debug.waitForDebugger();
                    Log.d("", "URI = " + _uri);
                    try {
                        if (_uri != null && "content".equalsIgnoreCase(_uri.getScheme())) {
                            //mm.safHelper.setURI(null);//disable SAF.
							//Log.d("SAF","Disabling SAF");
                            fileName = mm.getMainHelper().getFileName(_uri);
                            String state = Environment.getExternalStorageState();

                            if (Environment.MEDIA_MOUNTED.equals(state)) {
                                //path = mm.getExternalCacheDir().getPath();
                                path = mm.getPrefsHelper().getInstallationDIR()+"roms";
                                File f = new File(path+"/"+fileName);
                                if(!f.exists()) {
                                    java.io.InputStream input = mm.getContentResolver().openInputStream(_uri);
                                    error = mm.getMainHelper().copyFile(input, path, fileName);
                                    delete = true;
                                }
                            } else
                                error = true;
                        } else {
                            String filePath = _uri.getPath();
                            java.io.File f = new java.io.File(filePath);
                            fileName = f.getName();
                            path = f.getAbsolutePath().substring(0, f.getAbsolutePath().lastIndexOf(File.separator));
                        }
                    } catch (Exception e) {
                        error = true;
                    }

                    if (error) {
                        mm.runOnUiThread(new Runnable() {
                            public void run() {
                                mm.getDialogHelper().setInfoMsg("Error opening file...");
                                mm.showDialog(DialogHelper.DIALOG_INFO);
                            }
                        });
                    } else {

                        Emulator.setValueStr(Emulator.ROM_NAME, fileName);
						if (mm.getPrefsHelper().getROMsDIR() != null && mm.getPrefsHelper().getROMsDIR().length() != 0)
						    Emulator.setValueStr(Emulator.SAF_PATH, mm.getPrefsHelper().getROMsDIR());
                        System.out.println("XX name: " + fileName);
                        System.out.println("XX path: " + path);
                        extROM = true;
                        final String name = fileName;
                        mm.runOnUiThread(new Runnable() {
                            public void run() {
                                //Toast.makeText(mm, Html.fromHtml("<background color='#0000FF' ><b>" + "MAME4droid (0.139) " + versionName + ".<br>Launching: " + name + "</b></font>"), Toast.LENGTH_LONG).show();
                                Toast.makeText(mm, "Launching: " + name + "\nMAME4droid 2024 " + versionName, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                } else {
                    if (mm.getPrefsHelper().getROMsDIR() != null && mm.getPrefsHelper().getROMsDIR().length() != 0)
                        Emulator.setValueStr(Emulator.SAF_PATH, mm.getPrefsHelper().getROMsDIR());
                }

                mm.getMainHelper().updateEmuValues();

				runT();

                if (extROM) {
					/*
					if (pkg != null && "com.digdroid.alman.dig".equals(pkg.getHost())) {
						PackageManager pm = mm.getPackageManager();
						Intent intent2 = pm.getLaunchIntentForPackage("com.digdroid.alman.dig");
						mm.startActivity(intent2);
					}*/
                    if (delete) {
                        java.io.File f = new java.io.File(path, fileName);
                        f.delete();
                    }
                }
				mm.runOnUiThread(new Runnable() {
					public void run() {
						if (android.os.Build.VERSION.SDK_INT >= 21) {
							mm.finishAndRemoveTask();
						} else
							mm.finish();
						android.os.Process.killProcess(android.os.Process.myPid());
					}
				});
            }
        }, "emulatorNativeMain-Thread");


        if (mm.getPrefsHelper().getMainThreadPriority() == PrefsHelper.LOW) {
            t.setPriority(Thread.MIN_PRIORITY);
        } else if (mm.getPrefsHelper().getMainThreadPriority() == PrefsHelper.NORMAL) {
            t.setPriority(Thread.NORM_PRIORITY);
        } else
            t.setPriority(Thread.MAX_PRIORITY);

        t.start();
    }

    public static int getValue(int key) {
        return getValue(key, 0);
    }

    public static String getValueStr(int key) {
        return getValueStr(key, 0);
    }

    public static void setValue(int key, int value) {
        setValue(key, 0, value);
    }

    public static void setValueStr(int key, String value) {
        setValueStr(key, 0, value);
    }

    static int safOpenFile(String pathName, String mode) {
        //System.out.println("-->Llaman a safOpenFile en java "+pathName+" "+mode);

        String file = "";

        String romPath = mm.getPrefsHelper().getROMsDIR();
        if (pathName.startsWith(romPath))
            file = pathName.substring(romPath.length() + 1, pathName.length());

        if(file.equals(""))
            return -1;

        //System.out.println("File with path "+file);

        return mm.getSAFHelper().openUriFd("/" + file, mode);
    }

    static int safReadDir(String dirName, int reload) {
        //System.out.println("Llaman a safReadDir en java "+dirName);

        //boolean res = mm.getSAFHelper().listUriFiles(reload == 1);

		String dirSAF = "";

		String romPath = mm.getPrefsHelper().getROMsDIR();
		if (dirName.startsWith(romPath)) {
			dirSAF = dirName.substring(romPath.length(), dirName.length());
			if(!dirSAF.startsWith("/")) dirSAF = "/"+dirSAF;
			if(!dirSAF.endsWith("/")) dirSAF = dirSAF +"/";
		}

		return mm.getSAFHelper().readDir(dirSAF);
    }

    static String safGetNextDirEntry(int dirId) {
        //System.out.println("Llaman a safGetNextDirEntry en java "+dirId);

        return mm.getSAFHelper().getNextDirName(dirId);
    }

    static void safCloseDir(int dirId) {
        //System.out.println("Llaman a safCloseDir en java "+dirId);

		mm.getSAFHelper().closeDir(dirId);
    }

    //native
    protected static native void init(String libPath, String resPath, int nativeWidth, int nativeHeight);

    protected static native void runT();

    //protected static native void runVideoT();

    synchronized public static native void setDigitalData(int i, long data);

    synchronized public static native void setAnalogData(int i, float v1, float v2);

    public static native int getValue(int key, int i);

    public static native String getValueStr(int key, int i);

    public static native void setValue(int key, int i, int value);

    public static native void setValueStr(int key, int i, String value);

	public static native int setKeyData(int keyCode, int keyAction, char keyChar);

	public static native int setMouseData(int i, int mouseAction,int button, float cx, float cy);

}
