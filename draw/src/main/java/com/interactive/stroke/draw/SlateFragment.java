package com.interactive.stroke.draw;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
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
import android.util.Pair;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import com.interactive.stroke.draw.utils.PreferenceConstants;
import com.interactive.stroke.draw.utils.Rotation;


public class SlateFragment extends Fragment {
    
    private static final boolean DEBUG = true;
    private static final String TAG = "Draw";
    
    public boolean mJustLoadedImage = false;
    
    public Slate mSlate;
    public int mColor;
    
    protected MediaScannerConnection mMediaScannerConnection;
    private LinkedList<String> mDrawingsToScan = new LinkedList<String>();
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

    int mOriginalOrientation;
    int mOrientation;
    int mOriginalHeight;
    int mOriginalWidth;
    
    public static final String IMAGE_SAVE_DIRNAME = "Drawings";
    public static final String IMAGE_TEMP_DIRNAME = IMAGE_SAVE_DIRNAME + "/.temporary";
    
    
    @Override
    public void onCreate(Bundle icicle) {
	super.onCreate(icicle);
	Log.d("SLATE", "onCreate");
	setRetainInstance(true);
	mMediaScannerConnection = new MediaScannerConnection(this.getActivity(),
							     mMediaScannerClient);
	computeDefaultStartingSize();
	mOriginalOrientation = ((WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE))
	.getDefaultDisplay().getRotation();
	mOriginalOrientation = Rotation.convertToAngleInDegrees(mOriginalOrientation);
    }

    ViewGroup slateContainer;
    @Override    
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	Log.d("SLATE","onCreateView");
	View view = inflater.inflate(R.layout.slate, container, false);
	
	if (mSlate == null) {
	    mSlate = new Slate(this.getActivity());
	    mSlate.setDrawingBackground(Color.WHITE); 
	    // TODO the drawing Background will be configurable in the project creation wizard
	    // Load the old buffer if necessary
	    if (!mJustLoadedImage) {
		loadDrawing(PreferenceConstants.WIP_FILENAME, true);
	    } else {
		mJustLoadedImage = false;
	    }
	}	
	
	slateContainer = ((ViewGroup) view.findViewById(R.id.slate));
	slateContainer.setClipChildren(false);
	FrameLayout.LayoutParams frameParams = this.getSlateLayout(getSlateDimensionsAfterRotation());
	slateContainer.addView(mSlate, 0, frameParams);
	setPenType(0); // place holder param until brush choice is added in the new UI
	setPenColor(mColor);
	
	mOrientation = ((WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE))
		.getDefaultDisplay().getRotation();
	mOrientation = Rotation.convertToAngleInDegrees(mOrientation);
	return view;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
	super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
	super.onResume();
	if (mSlate.getParent() == null) {
	    slateContainer.addView(mSlate, 0, this.getSlateLayout(getSlateDimensionsAfterRotation()));
	}
    }
    
    @Override
    public void onViewStateRestored (Bundle savedInstanceState) {
	super.onViewStateRestored(savedInstanceState);
    }
    
    @Override
    public void onStop() {
	super.onStop();
	slateContainer.removeView(mSlate);
    }
	
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
	super.onConfigurationChanged(newConfig);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private FrameLayout.LayoutParams getSlateLayout(Pair<Integer, Integer> dimensions) {
	FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(dimensions.first, dimensions.second);
	frameParams.gravity = Gravity.TOP | Gravity.LEFT;
	frameParams.bottomMargin = this.getResources().
		getDimensionPixelOffset(R.dimen.default_drawing_distance_from_border);
	TypedValue tv = new TypedValue();
	getActivity().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true);
	int actionBarHeight = getResources().getDimensionPixelSize(tv.resourceId);
	frameParams.topMargin = this.getResources().
		getDimensionPixelOffset(R.dimen.default_drawing_distance_from_border) + actionBarHeight;
	frameParams.leftMargin  = this.getResources().getDimensionPixelOffset(R.dimen.toolbar_width);
	return frameParams;
    }

    
    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void computeDefaultStartingSize () {	
	TypedValue tv = new TypedValue();
	getActivity().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true);
	int actionBarHeight = getResources().getDimensionPixelSize(tv.resourceId);
	Display display = getActivity().getWindowManager().getDefaultDisplay();
	
	DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
	
	if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
	    Point outSize = new Point();
	    display.getSize(outSize);
	    mOriginalHeight = outSize.y;
	    mOriginalWidth = outSize.x;
	} else {
	    mOriginalHeight = display.getHeight();
	    mOriginalWidth = display.getWidth();
	}

	mOriginalHeight = mOriginalHeight
		- this.getResources().getDimensionPixelOffset(R.dimen.notification_bar_height)
		- actionBarHeight
		- this.getResources().getDimensionPixelOffset(R.dimen.default_drawing_verticalMargin);
	mOriginalWidth = mOriginalWidth 
		- 2 * this.getResources().getDimensionPixelOffset(R.dimen.toolbar_width);
    }
    
    private Pair<Integer, Integer> getSlateDimensionsAfterRotation() {
	int screenOrientation = ((WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE))
		.getDefaultDisplay().getRotation();
	screenOrientation = Rotation.convertToAngleInDegrees(screenOrientation);
	int deltaOrientation = Math.abs(mOriginalOrientation - screenOrientation);
	
	if (deltaOrientation % 90 == 0) {
	    return new Pair<Integer, Integer>(mOriginalWidth, mOriginalHeight);
	} else {
	    return new Pair<Integer, Integer>(mOriginalHeight, mOriginalWidth);
	}
    }

    public boolean loadDrawing(String filename) {
	return loadDrawing(filename, false);
    }

    public boolean loadDrawing(String filename, boolean temporary) {
   	File d = getPicturesDirectory();
   	d = new File(d, temporary ? IMAGE_TEMP_DIRNAME : IMAGE_SAVE_DIRNAME);
   	final String filePath = new File(d, filename).toString();
   	if (DEBUG)
   	    Log.d(TAG, "loadDrawing: " + filePath);
   	if (d.exists()) {
   	    BitmapFactory.Options opts = new BitmapFactory.Options();
   	    opts.inDither = false;
   	    opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
   	    opts.inScaled = false;
   	    Bitmap bits = BitmapFactory.decodeFile(filePath, opts);

   	    if (bits != null) {
   		// mSlate.setBitmap(bits); // messes with the bounds
   		mSlate.paintBitmap(bits);
   		return true;
   	    }
   	}
   	return false;
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
    
    public void setPenColor(int color) {
        mSlate.setPenColor(color);
    }
    
    public void setPenType(int type) {
        mSlate.setPenType(type);
    }

    public void saveDrawing(String filename) {
        saveDrawing(filename, false);
    }

    public void saveDrawing(String filename, boolean temporary) {
        saveDrawing(filename,
        	    temporary,
        	    /*animate=*/ false,
        	    /*share=*/ false,
        	    /*clear=*/ false);
    }

    public void saveDrawing(String filename, 
	    		    boolean temporary,
	    		    boolean animate,
	    		    boolean share,
	    		    boolean clear) {
	final Bitmap localBits = mSlate
		.copyBitmap(/* withBackground= */!temporary);
	if (localBits == null) {
	    if (DEBUG)
		Log.e(TAG, "save: null bitmap");
	    return;
	}

	final String _filename = filename;
	final boolean _temporary = temporary;
	final boolean _share = share;
	final boolean _clear = clear;

	new AsyncTask<Void, Void, String>() {
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
		    if (DEBUG)
			Log.d(TAG, "save: saving " + file);
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
		    synchronized (mDrawingsToScan) {
			mDrawingsToScan.add(fn);
			if (_share) {
			    mPendingShareFile = fn;
			}
			if (!mMediaScannerConnection.isConnected()) {
			 // will scan the files and share them
			    mMediaScannerConnection.connect(); 
			}
		    }
		}

		if (_clear)
		    mSlate.clear();
	    }
	}.execute();

    }

    
    
}
