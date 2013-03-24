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

package com.interactive.stroke.draw;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;

import com.interactive.stroke.draw.R;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
//import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
//import android.graphics.drawable.ColorDrawable;
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
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.Window;
import com.interactive.stroke.draw.GestureDetector.TranslationGestureDetector;
import com.interactive.stroke.draw.GestureDetector.RotationGestureDetector;
import com.slidingmenu.lib.SlidingMenu;
//import com.slidingmenu.lib.SlidingMenu.CanvasTransformer;

public class DrawActivity extends SherlockFragmentActivity  implements OnSeekBarChangeListener{    
    
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
    public MasterBucket mMasterBucket;
    private ColorDialogFragment mColorDialog;
    public int mColor;
    private SeekBar mBrushSizeBar;
    private SeekBar mBrushTransparencyBar;
    private  float DENSITY;
    private ImageButton mBrushButton;
    private ImageButton mEraserButton;
    private boolean mIsPenUsed = true;
    
    private float mScaleFactor = 1.f;
    private float mRotationDegrees = 0.f;
    private float mTranslationX = 0.f;
    private float mTranslationY = 0.f;  
    private boolean mIsSlateInTiltMode = false;

    private ScaleGestureDetector mScaleDetector;
    private RotationGestureDetector mRotateDetector;
    private TranslationGestureDetector mTranslationDetector;


    private View mDebugButton;

    private SlidingMenu menu;
    private final Paint paint = new Paint();
    private final ColorMatrix menuMatrix = new ColorMatrix();
    private static boolean sHackReady;
    private static boolean sHackAvailable;
    private static Field sRecreateDisplayList;
    private static Method sGetDisplayList;

    private Dialog mMenuDialog;

    private SharedPreferences mPrefs;

    private LinkedList<String> mDrawingsToScan = new LinkedList<String>();

    protected MediaScannerConnection mMediaScannerConnection;
    private String mPendingShareFile;
    private MediaScannerConnectionClient mMediaScannerClient = new MediaScannerConnection.MediaScannerConnectionClient() {
	@Override
	public void onMediaScannerConnected() {
	    if (DEBUG)
		Log.v(TAG, "media scanner connected");
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
	    if (DEBUG)
		Log.v(TAG, "File scanned: " + path);
	    synchronized (mDrawingsToScan) {
		if (path.equals(mPendingShareFile)) {
		    Intent sendIntent = new Intent(Intent.ACTION_SEND);
		    sendIntent.setType("image/png");
		    sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
		    startActivity(Intent.createChooser(sendIntent,
			    "Send drawing to:"));
		    mPendingShareFile = null;
		}
		scanNext();
	    }
	}
    };

    @SuppressWarnings("unused")
    private ActionMode mActionMode;
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

	// Called when the action mode is created; startActionMode() was called
	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
	    // Inflate a menu resource providing context menu items
	    MenuInflater inflater = mode.getMenuInflater();
	    inflater.inflate(R.menu.markers_activity_cab, menu);
	    mIsSlateInTiltMode = true;	    
	    return true;
	}

	// Called each time the action mode is shown. Always called after
	// onCreateActionMode, but
	// may be called multiple times if the mode is invalidated.
	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
	    return false; // Return false if nothing is done
	}

	// Called when the user selects a contextual menu item
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public boolean onActionItemClicked(ActionMode mode,
		com.actionbarsherlock.view.MenuItem item) {
	    switch (item.getItemId()) {
	    case R.id.menu_cab_reset:
		mScaleFactor = 1.f;
		mSlate.setScaleX(mScaleFactor);
		mSlate.setScaleY(mScaleFactor);
		mRotationDegrees = 0.f;
		mSlate.setRotation(mRotationDegrees);
		mTranslationX = 0.f;
		mTranslationY = 0.f;
		mSlate.setTranslationX(mTranslationX);
		mSlate.setTranslationY(mTranslationY);
		mIsSlateInTiltMode = false;
		mode.finish(); // Action picked, so close the CAB
		return true;
	    default:
		return false;
	    }
	}

	// Called when the user exits the action mode
	@Override
	public void onDestroyActionMode(ActionMode mode) {
	    mActionMode = null;
	    mIsSlateInTiltMode = false;
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


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onCreate(Bundle icicle) {
	super.onCreate(icicle);
	
	WindowManager.LayoutParams lp = getWindow().getAttributes();
	lp.format = PixelFormat.RGBA_8888;
	getWindow().setAttributes(lp);
	requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
	DENSITY  = getResources().getDisplayMetrics().density;
	setContentView(R.layout.main);
	mMasterBucket = (MasterBucket) this.findViewById(R.id.masterBucket);
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
		DrawActivity.this, mMediaScannerClient);

	if (icicle != null) {
	    onRestoreInstanceState(icicle);	    
	}
	
	ActionBar actionBar = this.getSupportActionBar();
	View mActionBarView = getLayoutInflater().inflate(R.layout.action_bar_cv, null);
	actionBar.setCustomView(mActionBarView);	
	actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM
		| ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);
	actionBar.setTitle(this.getString(R.string.draw_activity_title));

	mBrushSizeBar = (SeekBar) findViewById(R.id.brush_size_bar);
	mBrushSizeBar.setOnSeekBarChangeListener(this);
	mBrushSizeBar.setProgress(10);
	mBrushTransparencyBar = (SeekBar) findViewById(R.id.brush_transparency_bar);
	mBrushTransparencyBar.setOnSeekBarChangeListener(this);
	mBrushTransparencyBar.setProgress(0);
	
	mBrushButton = (ImageButton) findViewById(R.id.brush_button);
	mBrushButton.setSelected(true);
	mEraserButton = (ImageButton) findViewById(R.id.eraser_button);	
	
	mScaleDetector = new ScaleGestureDetector(getApplicationContext(), new ScaleListener());
	mRotateDetector = new RotationGestureDetector(getApplicationContext(), new RotateListener());
	mTranslationDetector = new TranslationGestureDetector(getApplicationContext(), new MoveListener());
	/*
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
	});*/

	loadSettings();
	setPenType(0); // place holder params until they are replaced by the new UI
	setPenColor(mColor);    

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
	menuMatrix.setSaturation(percentOpen);
	ColorMatrixColorFilter filter = new ColorMatrixColorFilter(menuMatrix);
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
        this.mColor = mPrefs.getInt(DrawActivity.PREF_LAST_COLOR, Color.BLACK);
        mMasterBucket.setColor(mColor);
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
	case R.id.menu_resize :	    
	    mActionMode = this.startActionMode(mActionModeCallback);
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
    public boolean dispatchTouchEvent(MotionEvent event) {
	if (!mIsSlateInTiltMode) {
	    return super.dispatchTouchEvent(event);
	}

	for (int i = 0; i < event.getPointerCount(); i++) {
	    if (event.getY(i) < this.getSupportActionBar().getHeight() + 
		    getResources().getDimension(R.dimen.notification_bar_height)) {
		return super.dispatchTouchEvent(event);
	    }
	    
	}
	mRotateDetector.onTouchEvent(event);
	mScaleDetector.onTouchEvent(event);
	mTranslationDetector.onTouchEvent(event);
	// mSlate.setPivotX(250);
	// mSlate.setPivotY(250);
	return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

	private float mInitialScaleFactor = -1;
	private static final float SCALING_THRESHOLD = 0.3f;
	private boolean mScaling = false;

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public boolean onScale(ScaleGestureDetector detector) {

	    if (!mScaling) {
		if (mInitialScaleFactor < 0) {
		    mInitialScaleFactor = detector.getScaleFactor();
		} else {
		    final float deltaScale = detector.getScaleFactor()
			    - mInitialScaleFactor;
		    if (Math.abs(deltaScale) > SCALING_THRESHOLD) {
			mScaling = true;
			return true;
		    }
		}
		return false;
	    }

	    mScaleFactor *= detector.getScaleFactor();
	    mScaleFactor = Math.min(Math.max(mScaleFactor, 0.8f), 3.0f);

	    mSlate.setScaleX(mScaleFactor);
	    mSlate.setScaleY(mScaleFactor);
	    // mSlate.setPivotX(mScaleDetector.getFocusX());
	    // mSlate.setPivotY(mScaleDetector.getFocusY());
	    return true;
	}
    }

    private class RotateListener extends RotationGestureDetector.SimpleOnRotateGestureListener {
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public boolean onRotate(RotationGestureDetector detector) {
	    mRotationDegrees -= detector.getRotationDegreesDelta();
	    mSlate.setRotation(mRotationDegrees);
	    // mSlate.setPivotX(mScaleDetector.getFocusX());
	    // mSlate.setPivotY(mScaleDetector.getFocusY());
	    return true;
	}
    }

    private class MoveListener extends TranslationGestureDetector.SimpleOnMoveGestureListener {
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public boolean onMove(TranslationGestureDetector detector) {
	    PointF delta = detector.getFocusDelta();
	    mTranslationX += delta.x;
	    mTranslationY += delta.y;
	    mSlate.setTranslationX(mTranslationX);
	    mSlate.setTranslationY(mTranslationY);
	    return true;
	}
    }
    
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
    		boolean fromUser) {
	switch (seekBar.getId()) {
	case R.id.brush_size_bar :
	    mSlate.setPenSize(2 +progress/DENSITY, 4 + 4 * progress /DENSITY);
	    break;
	case R.id.brush_transparency_bar :
	    mSlate.setPenOpacity(255 - progress);
	    break;
	}
    }
    

    
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {	
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {	
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
     
    public void onBrushButtonClick(View v) {
	if (!mIsPenUsed) {
	    mIsPenUsed = true;
	    mBrushButton.setSelected(true);
	    mEraserButton.setSelected(false);
	    setPenColor(mColor);
	}
    }
    
    public void onEraserButtonClick(View v) {
	if (mIsPenUsed) {
	    mIsPenUsed = false;
	mEraserButton.setSelected(true);
	mBrushButton.setSelected(false);
	setPenColor(0);
	}
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
