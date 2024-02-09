/*
 * This file is part of MAME4droid.
 *
 * Copyright (C) 2023 David Valdeita (Seleuco)
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

package com.seleuco.mame4droid.helpers;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.DocumentsContract;
import android.util.Log;

import com.seleuco.mame4droid.MAME4droid;
import com.seleuco.mame4droid.widgets.WarnWidget;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

class DirEnt{
	private static int lastId = 1;
	DirEnt(){this.id = lastId++;}
	int id = 0;
	int fileNameIdx = 0;
	ArrayList fileNames = null;
}

public class SAFHelper {

    /*static*/ protected MAME4droid mm = null;

	static Uri uri = null;
    static protected Hashtable<String, String> fileIDs = null; //hago estatico para evitar reloads si la actividad se recrea
    static protected Hashtable<String,ArrayList<String>> dirFiles = null;

	protected Hashtable<Integer,DirEnt> openDirs = new Hashtable<Integer, DirEnt>();

	protected WarnWidget pw = null;

	// protected ArrayList<String> fileNames = null;
    //int idxCurName = 0;

    public void setURI(String uriStr) {
		Log.d("SAF","set SAF uri:"+uriStr);
        if (uriStr == null)
            uri = null;
        else
            uri = Uri.parse(uriStr);
    }

    public SAFHelper(MAME4droid value) {
        mm = value;
    }

	public ArrayList<String> getRomsFileNames(){
		if (dirFiles == null) {//safety
			listUriFiles(true);
		}

		return dirFiles.get("/");
	}

	public int readDir(String dirName) {
		int res = 0;

		if (dirFiles == null) {//safety
			listUriFiles(true);
		}

	 	ArrayList<String> folderFiles = dirFiles.get(dirName);
		if(folderFiles!=null)
		{
			DirEnt ent = new DirEnt();
			ent.fileNames = folderFiles;
			openDirs.put(ent.id,ent);
			res = ent.id;
		}
		return res;
	}

	public int closeDir(int id) {
		int res = 0;

		if(openDirs!=null) {
			DirEnt dirEnt = openDirs.get(id);
			if (dirEnt != null) {
				openDirs.remove(id);
				res = 1;
			}
		}
		else res = 1;

		return res;
	}

    public String getNextDirName(int id) {
		String name = null;

		if(openDirs!=null) {
			DirEnt dirEnt = openDirs.get(id);
			if (dirEnt != null) {
				if (dirEnt.fileNameIdx < dirEnt.fileNames.size()) {
					name = (String) dirEnt.fileNames.get(dirEnt.fileNameIdx);
					dirEnt.fileNameIdx++;
				}
			}
		}
        return name;
    }

    public int openUriFd(String pathName, String flags) {
        //System.out.println("openRomUriFd "+pathName+" "+flags);

        if (fileIDs == null) {//safety
            //return -1;
            listUriFiles(true);
        }

        String fileid = null;

        fileid = (String) fileIDs.get(pathName);

        if (fileid == null && flags.contains("w")) {
            String mimeType = "application/octet-stream";
            try {
                String path = "";
                String name = pathName;
                int i = pathName.lastIndexOf("/");
                if (i != -1) {
                    name = pathName.substring(i + 1, pathName.length());
                    path = pathName.substring(0, i + 1);
                }

                String docId = retrieveDirId(path);

                if (docId != null) {
                    Uri dirUri = DocumentsContract.buildDocumentUriUsingTree(uri, docId);

                    Uri docUri = DocumentsContract.createDocument(mm.getContentResolver(), dirUri, mimeType, name);
                    fileid = DocumentsContract.getDocumentId(docUri);
                    fileIDs.put(pathName, fileid);
					dirFiles.get(path).add(name);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (fileid != null) {
            final Uri fileUri = DocumentsContract.buildDocumentUriUsingTree(uri, fileid);
            try {
                return mm.getContentResolver().openFileDescriptor(fileUri, flags).detachFd();
            } catch (Exception e) {
                return -1;
            }
        }
        return -1;
    }

    private String retrieveDirId(String path) {

        if (path == null || path.isEmpty())
            return null;

        String id = (String) fileIDs.get(path);
        if (id != null || path.equals("/")) {
            //System.out.println("Encuentro id para "+path+" "+id);
            return id;
        } else {
            String newPath = path;
            String dirName = "";
            int i = path.substring(0, path.length() - 1).lastIndexOf("/");
            if (i == -1) return null;
            newPath = path.substring(0, i + 1);
            dirName = path.substring(i + 1, path.length() - 1);

            //System.out.println("newPath "+newPath);
            //System.out.println("dirName "+dirName);

            id = retrieveDirId(newPath);
            if (id != null) {
                try {
                    Uri parentDirUri = DocumentsContract.buildDocumentUriUsingTree(uri, id);
                    Uri newDirUri = DocumentsContract.createDocument(mm.getContentResolver(), parentDirUri, DocumentsContract.Document.MIME_TYPE_DIR, dirName);
                    id = DocumentsContract.getDocumentId(newDirUri);
                    fileIDs.put(path, id);
					ArrayList<String> newFolderFiles = new ArrayList<String>();
					dirFiles.put(path,newFolderFiles);
                    return id;
                } catch (Exception e) {
                    //e.printStackTrace();
                    return null;
                }
            }
        }
        return null;
    }

    public boolean listUriFiles(Boolean reload) {

		pw = new WarnWidget(mm,"Caching SAF files."," Reading, please wait...", Color.WHITE,false,false);
		pw.init();

        if (fileIDs != null && !reload) return true;

        fileIDs = new Hashtable<String, String>();
		dirFiles = new Hashtable<String,ArrayList<String>>();

        if (uri == null) {
            Log.e("SAF", "SAF URI NOT SET!!!");
			throw new RuntimeException("SAF URI NOT SET!!!");
            //return true;
        }

        String id = DocumentsContract.getTreeDocumentId(uri);
        fileIDs.put("/", id);
		ArrayList<String> files = new ArrayList<String>();
		dirFiles.put("/",files);

        System.out.println("path " + pathFromDocumentUri(uri));
        System.out.println("tree document id " + id);

        boolean res =  listUriFilesRecursive(files,   uri, "", 0);

		pw.end();

		return res;
    }

    private boolean listUriFilesRecursive(ArrayList<String> folderFiles, Uri _uri, String path, int p) {
        if (p == 6) return true;

        //System.out.println("Uri "+_uri);
        //System.out.println("Tree document id "+DocumentsContract.getTreeDocumentId(_uri));
        final Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(_uri, p == 0 ? DocumentsContract.getTreeDocumentId(_uri) : DocumentsContract.getDocumentId(_uri));
        //System.out.println("childrenUri "+childrenUri);
        Cursor c = null;
        try {
            c = mm.getContentResolver().query(childrenUri,
                    new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE},
                    null, null, null);

            if (c == null)
                return false;

            while (c.moveToNext()) {
                final String documentId = c.getString(0);
                final String displayName = c.getString(1);
                final String mimeType = c.getString(2);
                final boolean isDir = mimeType.equals(DocumentsContract.Document.MIME_TYPE_DIR);
                final Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(_uri, documentId);
                final String filepath = path + "/" + displayName;
                //System.out.println(documentId+ " "+displayName+" "+" "+mimeType+" "+isDir+" "+documentUri);
                if (!isDir) {
                    fileIDs.put(filepath, documentId);
                    //System.out.println(documentId+ " "+filepath+" "+" "+mimeType+" "+documentUri);
					folderFiles.add(displayName);

					if(pw!=null) {
						pw.notifyText("Caching: " + displayName);
					}
                } else {
					String dirPath = filepath + "/";
					ArrayList<String> newFolderFiles = new ArrayList<String>();
                    fileIDs.put(dirPath, documentId);
					dirFiles.put(dirPath,newFolderFiles);
                    //System.out.println(documentId+ " "+filepath+" "+" "+mimeType+" "+documentUri);
                    listUriFilesRecursive(newFolderFiles, documentUri, filepath, p + 1);
                }
            }

            return true;
        } catch (Exception e) {
            System.out.println("listUriFiles exception:" + e.toString());

            if (p == 0) {
                e.printStackTrace();
                mm.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mm.getDialogHelper().setInfoMsg(
                                "MAME4droid doesn't have permission to read the roms files on " + mm.getPrefsHelper().getROMsDIR() +
                                        ".\n\nGive permissions again or select another ROMs folder, both in MAME4droid menu 'Options -> Settings -> General -> Change ROMs path'.");
                        mm.showDialog(DialogHelper.DIALOG_INFO);
                    }//public void run() {
                });
            }

            return false;
        } finally {
            closeQuietly(c);
        }
    }

    StorageVolume findVolume(String name) {
        final StorageManager sm = (StorageManager) mm.getSystemService(Context.STORAGE_SERVICE);
        if (name.equalsIgnoreCase("primary"))
            return sm.getPrimaryStorageVolume();
        for (final StorageVolume vol : sm.getStorageVolumes()) {
            final String uuid = vol.getUuid();
            if (uuid != null && uuid.equalsIgnoreCase(name))
                return vol;
        }
        return null;
    }

    String pathFromDocumentUri(Uri uri) {
        final List<String> pathSegment = uri.getPathSegments();

        //for(String s : pathSegment) { System.out.println("path segment: " + s); }

        if (pathSegment.size() < 2)
            return null;

        final String[] split = pathSegment.get(1).split(":");

        String tmp = null;

        if (split.length == 2) {

            final StorageVolume vol = findVolume(split[0]);
            if (vol == null) {
                return null;
            }

            try {
                File f = vol.getDirectory(); //Cuidado produce NoSuchMethodExcption en Android10 en algunos devices. Edirectory where this volume is mounted, or null if the volume is not currently mounted.
                if (f != null) {
                    tmp = f.getAbsolutePath(); //SDK > 29
                }
            } catch (Throwable e) {
                try {
                    Method method = vol.getClass().getMethod("getPath");//SDK 29
                    if (method != null) {
                        tmp = (String) method.invoke(vol);
                    }
                } catch (Exception ee) {
                }
                //e.printStackTrace();
                //tmp = "/"+vol.getDescription(mm);
            }
            if (tmp != null)
                tmp += "/" + split[1];
        } else if (split.length == 1) {
            if (!split[0].startsWith("/")) {
                tmp = "/" + split[0];
            } else
                tmp = split[0];
        }

        return tmp;
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }
}
