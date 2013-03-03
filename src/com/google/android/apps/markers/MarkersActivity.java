/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.markers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;

import org.dsandler.apps.markers.R;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.slidingmenu.lib.SlidingMenu;
import com.slidingmenu.lib.SlidingMenu.CanvasTransformer;

public class MarkersActivity extends SherlockFragmentActivity 
{
    final static int LOAD_IMAGE = 1000;

    private static final String TAG = "Markers";
    private static final boolean DEBUG = true;

    public static final String IMAGE_SAVE_DIRNAME = "Drawings";
    public static final String IMAGE_TEMP_DIRNAME = IMAGE_SAVE_DIRNAME + "/.temporary";
    public static final String WIP_FILENAME = "temporary.png";
    
    public static final String PREF_LAST_TOOL = "tool";
    public static final String PREF_LAST_TOOL_TYPE = "tool_type";
    public static final String PREF_LAST_COLOR = "color";

    private boolean mJustLoadedImage = false;

    private Slate mSlate;
    public ColorButtonView mColorButton;
    private ColorDialogFragment mColorDialog;
    public int mColor;

    private View mDebugButton;
    
    private SlidingMenu menu;
    private final Paint paint = new Paint();
	private final ColorMatrix matrix = new ColorMatrix();	
	private static boolean sHackReady;
	private static boolean sHackAvailable;
	private static Field sRecreateDisplayList;
	private static Method sGetDisplayList;

    
    private Dialog mMenuDialog;

    private SharedPreferences mPrefs;

    private LinkedList<String> mDrawingsToScan = new LinkedList<String>();

    protected MediaScannerConnection mMediaScannerConnection;
    private String mPendingShareFile;
    private MediaScannerConnectionClient mMediaScannerClient = 
            new MediaScannerConnection.MediaScannerConnectionClient() {
                @Override
                public void onMediaScannerConnected() {
                    if (DEBUG) Log.v(TAG, "media scanner connected");
                    scanNext();
                }
                
                private void scanNext() {
                    synchronized (mDrawingsToScan) {
                        if (mDrawingsToScan.isEmpty()) {
                            mMediaScannerConnection.disconnect();
                            return;
                        }
                        String fn = mDrawingsToScan.removeFirst();
                        mMediaScannerConnection.scanFile(fn, "image/png");
                    }
                }
        
                @Override
                public void onScanCompleted(String path, Uri uri) {
                    if (DEBUG) Log.v(TAG, "File scanned: " + path);
                    synchronized (mDrawingsToScan) {
                        if (path.equals(mPendingShareFile)) {
                            Intent sendIntent = new Intent(Intent.ACTION_SEND);
                            sendIntent.setType("image/png");
                            sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
                            startActivity(Intent.createChooser(sendIntent, "Send drawing to:"));
                            mPendingShareFile = null;
                        }
                        scanNext();
                    }
                }
            };



/*
    @Override
    public Object onRetainNonConfigurationInstance() {
    	((ViewGroup)mSlate.getParent()).removeView(mSlate);
        return mSlate;
    }should be ok as long as the orientation is fixed. If not the slate needs to be in a
     fragment and use setRetainInstance on it*/
    
    public static interface ViewFunc {
        public void apply(View v);
    }
    public static void descend(ViewGroup parent, ViewFunc func) {
        for (int i=0; i<parent.getChildCount(); i++) {
            final View v = parent.getChildAt(i);
            if (v instanceof ViewGroup) {
                descend((ViewGroup) v, func);
            } else {
                func.apply(v);
            }
        }
    }


    @Override
    public void onCreate(Bundle icicle) {
	super.onCreate(icicle);

	final Window win = getWindow();
	WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
	lp.copyFrom(win.getAttributes());
	lp.format = PixelFormat.RGBA_8888;
	// win.setBackgroundDrawableResource(R.drawable.transparent);
	ColorDrawable cd = new ColorDrawable(Color.WHITE);
	win.setBackgroundDrawable(cd);

	win.setAttributes(lp);
	// win.requestFeature(Window.FEATURE_NO_TITLE);

	setContentView(R.layout.main);

	mColorButton = (ColorButtonView) this.findViewById(R.id.colorbutton);
	// mSlate = (Slate) getLastNonConfigurationInstance();
	if (mSlate == null) {
	    mSlate = new Slate(this);
	    mSlate.setDrawingBackground(Color.WHITE); // TODO to be configurable in the project creation wizard

	    // Load the old buffer if necessary
	    if (!mJustLoadedImage) {
		loadDrawing(WIP_FILENAME, true);
	    } else {
		mJustLoadedImage = false;
	    }
	}
	final ViewGroup slateContainer = ((ViewGroup) findViewById(R.id.slate));
	slateContainer.addView(mSlate, 0);

	mMediaScannerConnection = new MediaScannerConnection(
		MarkersActivity.this, mMediaScannerClient);

	if (icicle != null) {
	    onRestoreInstanceState(icicle);
	}

	menu = new SlidingMenu(this);
	menu.setMode(SlidingMenu.LEFT);
	menu.setShadowDrawable(R.drawable.shadow);
	menu.setShadowWidthRes(R.dimen.shadow_width);
	menu.setBehindOffsetRes(R.dimen.slidingmenu_offset);
	menu.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
	menu.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);
	menu.setFadeEnabled(false);
	menu.setMenu(R.layout.sliding_menu);
	menu.setTouchModeMarginThreshold(getResources().getDimensionPixelOffset(R.dimen.toolbar_width));
	

	menu.setBehindCanvasTransformer(new CanvasTransformer() {
	    @Override
	    public void transformCanvas(Canvas canvas, float percentOpen) {
		boolean API_17 = Build.VERSION.SDK_INT >= 17;
		boolean API_16 = Build.VERSION.SDK_INT == 16;

		if (API_16) {
		    prepareLayerHack();
		}
		// add invalidate spot
		manageLayers(percentOpen);
		updateColorFilter(percentOpen);
		updatePaint(API_17, API_16);
	    }
	});

	loadSettings();

	setPenType(0); // place holder params until they are replaced by the new UI
	setPenColor(mColor);
	mSlate.setPenSize(1, 40);

    }
//-----------------------
// Sliding menu methods :
    
    @TargetApi(17)
    private void updatePaint(boolean API_17, boolean API_16) {
	View backView = menu.getMenu();
	if (API_17) {
	    backView.setLayerPaint(paint);
	} else {
	    if (API_16) {
		if (sHackAvailable) {
		    try {
			sRecreateDisplayList.setBoolean(backView, true);
			sGetDisplayList.invoke(backView, (Object[]) null);
		    } catch (IllegalArgumentException e) {
		    } catch (IllegalAccessException e) {
		    } catch (InvocationTargetException e) {
		    }
		} else {
		    // This solution is slow
		    menu.getMenu().invalidate();
		}
	    }

	    // API level < 16 doesn't need the hack above, but the invalidate is
	    // required
	    ((View) backView.getParent()).postInvalidate(backView.getLeft(),
		    backView.getTop(), backView.getRight(),
		    backView.getBottom());
	}
    }

    private void updateColorFilter(float percentOpen) {
	matrix.setSaturation(percentOpen);
	ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
	paint.setColorFilter(filter);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void manageLayers(float percentOpen) {
	boolean layer = percentOpen > 0.0f && percentOpen < 1.0f;
	int layerType = layer ? View.LAYER_TYPE_HARDWARE : View.LAYER_TYPE_NONE;

	if (Build.VERSION.SDK_INT >= 11
		&& layerType != menu.getContent().getLayerType()) {
	    menu.getContent().setLayerType(layerType, null);
	    menu.getMenu().setLayerType(layerType,
		    Build.VERSION.SDK_INT <= 16 ? paint : null);
	}
    }

    private static void prepareLayerHack() {
	if (!sHackReady) {
	    try {
		sRecreateDisplayList = View.class
			.getDeclaredField("mRecreateDisplayList");
		sRecreateDisplayList.setAccessible(true);

		sGetDisplayList = View.class.getDeclaredMethod(
			"getDisplayList", (Class<?>) null);
		sGetDisplayList.setAccessible(true);

		sHackAvailable = true;
	    } catch (NoSuchFieldException e) {
	    } catch (NoSuchMethodException e) {
	    }
	    sHackReady = true;
	}
    }

//-----------------------

    public void showColorPicker(View view) {
	if (mColorDialog == null) {
	    mColorDialog = new ColorDialogFragment();
	}
	mColorDialog.mOldColor = mColor;
	mColorDialog.show(getSupportFragmentManager(), "colorPicker");
    }

    
    private void loadSettings() {
        mPrefs = getPreferences(MODE_PRIVATE);
        this.mColor = mPrefs.getInt(MarkersActivity.PREF_LAST_COLOR, Color.BLACK);
        mColorButton.setColor(mColor);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
       MenuInflater inflater = getSupportMenuInflater();
       inflater.inflate(R.menu.markers_activity, (Menu) menu);
       return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(
	    com.actionbarsherlock.view.MenuItem item) {
	switch (item.getItemId()) {
	case R.id.menu_back:
	    clickUndo(null);
	    break;
	case R.id.menu_clear:
	    clickClear();
	    break;
	case R.id.menu_share:
	    clickShare(null);
	    break;
	}
	return true;
    }

    @Override
    public void onPause() {
	super.onPause();
	saveDrawing(WIP_FILENAME, true);
    }

    @Override
    public void onResume() {
        super.onResume();
        
        String orientation = getString(R.string.orientation);
        
        setRequestedOrientation(
                "landscape".equals(orientation)
                    ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    public void onConfigurationChanged (Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onAttachedToWindow() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
    }

    @Override
    protected void onStop() {
        mPrefs.edit().putInt(PREF_LAST_COLOR, mColor).commit();
        super.onStop();
    }

    private String dumpBundle(Bundle b) {
        if (b == null) return "null";
        StringBuilder sb = new StringBuilder("Bundle{");
        boolean first = true;
        for (String key : b.keySet()) {
            if (!first) sb.append(" ");
            first = false;
            sb.append(key+"=(");
            sb.append(b.get(key));
        }
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        Intent startIntent = getIntent();
        if (DEBUG) Log.d(TAG, "starting with intent=" + startIntent + " extras=" + dumpBundle(startIntent.getExtras()));
        String a = startIntent.getAction();
        if (a.equals(Intent.ACTION_EDIT)) {
            // XXX: what happens to the old drawing? we should really move to auto-save
            mSlate.clear();
            loadImageFromIntent(startIntent);
        } else if (a.equals(Intent.ACTION_SEND)) {
            // XXX: what happens to the old drawing? we should really move to auto-save
            mSlate.clear();
            loadImageFromContentUri((Uri)startIntent.getParcelableExtra(Intent.EXTRA_STREAM));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle icicle) {
    }

    @Override
    protected void onRestoreInstanceState(Bundle icicle) {
    }



    final static boolean hasAnimations() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB);
    }



    public void clickClear(View v) {
        mSlate.clear();
    }

    public boolean loadDrawing(String filename) {
        return loadDrawing(filename, false);
    }

    @SuppressLint("SdCardPath")
    @TargetApi(8)
    public File getPicturesDirectory() {
        final File d;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            d = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        } else {
            d = new File("/sdcard/Pictures");
        }
        return d;
    }

    public boolean loadDrawing(String filename, boolean temporary) {
        File d = getPicturesDirectory();
        d = new File(d, temporary ? IMAGE_TEMP_DIRNAME : IMAGE_SAVE_DIRNAME);
        final String filePath = new File(d, filename).toString();
        if (DEBUG) Log.d(TAG, "loadDrawing: " + filePath);
        
        if (d.exists()) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inDither = false;
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            opts.inScaled = false;
            Bitmap bits = BitmapFactory.decodeFile(filePath, opts);
            if (bits != null) {
                //mSlate.setBitmap(bits); // messes with the bounds
                mSlate.paintBitmap(bits);
                return true;
            }
        }
        return false;
    }

    public void saveDrawing(String filename) {
        saveDrawing(filename, false);
    }

    public void saveDrawing(String filename, boolean temporary) {
        saveDrawing(filename, temporary, /*animate=*/ false, /*share=*/ false, /*clear=*/ false);
    }

    public void saveDrawing(String filename, boolean temporary, boolean animate, boolean share, boolean clear) {
        final Bitmap localBits = mSlate.copyBitmap(/*withBackground=*/!temporary);
        if (localBits == null) {
            if (DEBUG) Log.e(TAG, "save: null bitmap");
            return;
        }
        
        final String _filename = filename;
        final boolean _temporary = temporary;
        final boolean _share = share;
        final boolean _clear = clear;

        new AsyncTask<Void,Void,String>() {
            @Override
            protected String doInBackground(Void... params) {
                String fn = null;
                try {
                    File d = getPicturesDirectory();
                    d = new File(d, _temporary ? IMAGE_TEMP_DIRNAME : IMAGE_SAVE_DIRNAME);
                    if (!d.exists()) {
                        if (d.mkdirs()) {
                            if (_temporary) {
                                final File noMediaFile = new File(d, MediaStore.MEDIA_IGNORE_FILENAME);
                                if (!noMediaFile.exists()) {
                                    new FileOutputStream(noMediaFile).write('\n');
                                }
                            }
                        } else {
                            throw new IOException("cannot create dirs: " + d);
                        }
                    }
                    File file = new File(d, _filename);
                    if (DEBUG) Log.d(TAG, "save: saving " + file);
                    OutputStream os = new FileOutputStream(file);
                    localBits.compress(Bitmap.CompressFormat.PNG, 0, os);
                    localBits.recycle();
                    os.close();
                    
                    fn = file.toString();
                } catch (IOException e) {
                    Log.e(TAG, "save: error: " + e);
                }
                return fn;
            }
            
            @Override
            protected void onPostExecute(String fn) {
                if (fn != null) {
                    synchronized(mDrawingsToScan) {
                        mDrawingsToScan.add(fn);
                        if (_share) {
                            mPendingShareFile = fn;
                        }
                        if (!mMediaScannerConnection.isConnected()) {
                            mMediaScannerConnection.connect(); // will scan the files and share them
                        }
                    }
                }

                if (_clear) mSlate.clear();
            }
        }.execute();
        
    }

    public void clickSave(View v) {
        if (mSlate.isEmpty()) return;
        
        v.setEnabled(false);
        final String filename = System.currentTimeMillis() + ".png"; 
        saveDrawing(filename);
        Toast.makeText(this, "Drawing saved: " + filename, Toast.LENGTH_SHORT).show();
        v.setEnabled(true);
    }
    
    public void clickClear() {
	mSlate.clear();
    }

    public void clickSaveAndClear(View v) {
        if (mSlate.isEmpty()) return;

        v.setEnabled(false);
        final String filename = System.currentTimeMillis() + ".png"; 
        saveDrawing(filename, 
                /*temporary=*/ false, /*animate=*/ true, /*share=*/ false, /*clear=*/ true);
        Toast.makeText(this, "Drawing saved: " + filename, Toast.LENGTH_SHORT).show();
        v.setEnabled(true);
    }

    private void setThingyEnabled(Object v, boolean enabled) {
        if (v == null) return;
        if (v instanceof View) ((View)v).setEnabled(enabled);
        else if (v instanceof MenuItem) ((MenuItem)v).setEnabled(enabled);
    }

    public void clickShare(View v) {
        //setThingyEnabled(v, false);
        final String filename = System.currentTimeMillis() + ".png";
        // can't use a truly temporary file because:
        // - we want mediascanner to give us a content: URI for it; some apps don't like file: URIs
        // - if mediascanner scans it, it will show up in Gallery, so it might as well be a regular drawing
        saveDrawing(filename,
                /*temporary=*/ false, /*animate=*/ false, /*share=*/ true, /*clear=*/ false);
        setThingyEnabled(v, true);
    }

    public void clickLoad(View unused) {
        hideOverflow();
        Intent i = new Intent(Intent.ACTION_PICK,
                       android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(i, LOAD_IMAGE); 
    }

    public void clickDebug(View unused) {
        hideOverflow();
        boolean debugMode = (mSlate.getDebugFlags() == 0); // toggle 
        mSlate.setDebugFlags(debugMode
            ? Slate.FLAG_DEBUG_EVERYTHING
            : 0);
        mDebugButton.setSelected(debugMode);
        Toast.makeText(this, "Debug mode " + ((mSlate.getDebugFlags() == 0) ? "off" : "on"),
            Toast.LENGTH_SHORT).show();
    }

    public void clickUndo(View unused) {
        mSlate.undo();
    }

    public void clickAbout(View unused) {
        hideOverflow();
        About.show(this);
    }

    public void clickQr(View unused) {
        hideOverflow();
        QrCode.show(this);
    }

    public void clickShareMarketLink(View unused) {
        hideOverflow();
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
        sendIntent.putExtra(Intent.EXTRA_TEXT,
                "http://play.google.com/store/apps/details?id=" + getPackageName());
        startActivity(Intent.createChooser(sendIntent, "Share the Markers app with:"));
    }

    public void clickMarketLink(View unused) {
        hideOverflow();
        Intent urlIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=" + getPackageName()));
        startActivity(urlIntent);
    }

    public void clickSiteLink(View unused) {
        hideOverflow();
        Intent urlIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("http://dsandler.org/markers?from=app"));
        startActivity(urlIntent);
    }


    private void hideOverflow() {
        mMenuDialog.dismiss();
    }


    public void setPenColor(int color) {
        mSlate.setPenColor(color);
    }
    
    public void setPenType(int type) {
        mSlate.setPenType(type);
    }
    
    protected void loadImageFromIntent(Intent imageReturnedIntent) {
        Uri contentUri = imageReturnedIntent.getData();
        loadImageFromContentUri(contentUri);
    }
    
    protected void loadImageFromContentUri(Uri contentUri) {
        Toast.makeText(this, "Loading from " + contentUri, Toast.LENGTH_SHORT).show();

        loadDrawing(WIP_FILENAME, true);
        mJustLoadedImage = true;

        try {
            Bitmap b = MediaStore.Images.Media.getBitmap(getContentResolver(), contentUri);
            if (b != null) {
                mSlate.paintBitmap(b);
                if (DEBUG) Log.d(TAG, "successfully loaded bitmap: " + b);
            } else {
                Log.e(TAG, "couldn't get bitmap from " + contentUri);
            }
        } catch (java.io.FileNotFoundException ex) {
            Log.e(TAG, "error loading image from " + contentUri + ": " + ex);
        } catch (java.io.IOException ex) {
            Log.e(TAG, "error loading image from " + contentUri + ": " + ex);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) { 
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent); 

        switch (requestCode) { 
        case LOAD_IMAGE:
            if (resultCode == RESULT_OK) {
                loadImageFromIntent(imageReturnedIntent);
            }
        }
    }

}
