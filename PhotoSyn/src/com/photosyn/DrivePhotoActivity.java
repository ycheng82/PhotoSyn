package com.photosyn;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.DriveContentsResult;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.DriveApi.DriveIdResult;
import com.google.android.gms.drive.DriveApi.MetadataBufferResult;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.drive.DriveFolder;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class DrivePhotoActivity extends BaseDemoActivity {
	// private ListView mResultsListView;
	// private ResultsAdapter mResultsAdapter;
	private final String FOLDER_NAME = "PhotoSyn";
	private static final String TAG = "get file list";
	private DriveId PHOTOSYN_FOLDER_ID;
	private ImageView img;
	private ProgressDialog pDialog;
	private Bitmap bitmap;

	@Override
	public void onConnected(Bundle connectionHint) {
		super.onCreate(connectionHint);
		setContentView(R.layout.activity_drivephoto);
		// mResultsListView = (ListView) findViewById(R.id.listViewResults);
		// mResultsAdapter = new ResultsAdapter(this);
		// mResultsListView.setAdapter(mResultsAdapter);
		/* query certain files */

		DriveFolder folder = Drive.DriveApi.getRootFolder(getGoogleApiClient());
		Query query = new Query.Builder().addFilter(
				Filters.and(Filters.eq(SearchableField.MIME_TYPE,
						"application/vnd.google-apps.folder"), Filters.eq(
						SearchableField.TITLE, FOLDER_NAME))).build();
		
		getGoogleApiClient();
		folder.queryChildren(getGoogleApiClient(), query).setResultCallback(
				childrenRetrievedCallback);
		img = (ImageView) findViewById(R.id.imageView1);

	}

	final private ResultCallback<DriveIdResult> idCallback = new ResultCallback<DriveIdResult>() {
		@Override
		public void onResult(DriveIdResult result) {
			if (!result.getStatus().isSuccess()) {
				showMessage("Cannot find DriveId. Are you authorized to view this file?");
				return;
			}
			DriveFolder folder = Drive.DriveApi.getFolder(getGoogleApiClient(),
					result.getDriveId());
			folder.listChildren(getGoogleApiClient()).setResultCallback(
					metadataResult);
		}
	};
	
	
	final private ResultCallback<MetadataBufferResult> metadataResult = new ResultCallback<MetadataBufferResult>() {

		@Override
		public void onResult(final MetadataBufferResult result) {
			if (!result.getStatus().isSuccess()) {
				showMessage("Problem while retrieving files");
				return;
			}
			// mResultsAdapter.clear();
			// mResultsAdapter.append(result.getMetadataBuffer());
			showMessage("Listed " + result.getMetadataBuffer().getCount()
					+ " files.");

			Log.i(TAG, result.getMetadataBuffer().get(0).getTitle());
			Log.i(TAG, result.getMetadataBuffer().get(1).getTitle());// use
																		// isTrashed()
																		// to
																		// check
																		// if it
																		// is in
																		// trash

			// String t1 = result.getMetadataBuffer().get(0).getWebViewLink();
			/* getWebViewLink returns null */

			String t2 = result.getMetadataBuffer().get(0).getWebContentLink();
			Log.i(TAG, t2);
			// new DownloadImageTask((ImageView) findViewById(R.id.imageView1))
			// .execute(result.getMetadataBuffer().get(0).getWebContentLink());

			
			
			//new LoadImage().execute(result.getMetadataBuffer().get(0).getWebContentLink());
			
			//test retrieving the drive file
			DriveFile file = Drive.DriveApi.getFile (getGoogleApiClient(), result.getMetadataBuffer().get(0).getDriveId());
			file.open(getGoogleApiClient(), DriveFile.MODE_READ_ONLY, null)
					        .setResultCallback(contentsOpenedCallback);


		}
	};
	
	ResultCallback<DriveContentsResult> contentsOpenedCallback =
	        new ResultCallback<DriveContentsResult>() {
	    @Override
	    public void onResult(DriveContentsResult result) {
	        if (!result.getStatus().isSuccess()) {
	            // display an error saying file can't be opened
	            return;
	        }
	        // DriveContents object contains pointers
	        // to the actual byte stream
	        DriveContents contents = result.getDriveContents();
	        showMessage("Successfully get drivecontents");
	        
	        new LoadImage().execute(contents);
	    }
	};

	
	
	ResultCallback<MetadataBufferResult> childrenRetrievedCallback = new ResultCallback<MetadataBufferResult>() {
		@Override
		public void onResult(MetadataBufferResult result) {
			if (!result.getStatus().isSuccess()) {
				showMessage("Problem while retrieving files");
				return;
			}
			showMessage("Successfully listed"
					+ result.getMetadataBuffer().getCount() + " files.");
			PHOTOSYN_FOLDER_ID = result.getMetadataBuffer().get(0).getDriveId();

			/*
			 * MetadataBuffer buffer = result.getMetadataBuffer(); for (Metadata
			 * m : buffer) { Log.i(TAG, "Metadata name  " + m.getTitle()); }
			 */

			Drive.DriveApi.fetchDriveId(getGoogleApiClient(),
					PHOTOSYN_FOLDER_ID.getResourceId()).setResultCallback(
					idCallback);

		}
	};

	private class LoadImage extends AsyncTask<DriveContents,String, Bitmap> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(DrivePhotoActivity.this);
			pDialog.setMessage("Loading Image ....");
			pDialog.show();
		}

		protected Bitmap doInBackground(DriveContents ... args) {
			try {
				bitmap = BitmapFactory.decodeStream(args[0].getInputStream());
			} catch (Exception e) {
				e.printStackTrace();
			}
			return bitmap;
		}

		protected void onPostExecute(Bitmap image) {
			if (image != null) {
				img.setImageBitmap(image);
				pDialog.dismiss();
			} else {
				pDialog.dismiss();
				Toast.makeText(DrivePhotoActivity.this,
						"Image Does Not exist or Network Error",
						Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	/**
	 * save photo to the gallery
	 * @param view
	 */
	public void savePhoto(View view) {
		MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "my_photo" , "My photo test");
	 }
}