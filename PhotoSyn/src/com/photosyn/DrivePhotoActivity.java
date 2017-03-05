package com.photosyn;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.photosyn.R;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.DriveIdResult;
import com.google.android.gms.drive.DriveApi.MetadataBufferResult;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveApi.DriveContentsResult;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveResource.MetadataResult;

import android.provider.MediaStore;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;

/**
 * get photos from google drive, load it on page, save it to gallery
 * 
 * @author Ye
 *
 */
public class DrivePhotoActivity extends BaseDemoActivity {
	private final String FOLDER_NAME = "PhotoSyn";
	private ProgressDialog mProgressDialog;
	
	@Override
	public void onConnected(Bundle connectionHint) {
		super.onCreate(connectionHint);
		setContentView(R.layout.activity_drivephoto);
		String folderId = Drive.DriveApi.getRootFolder(getGoogleApiClient()).getDriveId().getResourceId().toString(); 
		Drive.DriveApi.fetchDriveId(getGoogleApiClient(),folderId)
        .setResultCallback(rootFolderCallback);
	}

	/*
	 * callback function
	 * read PhotoSyn from rootFolder
	 */
	final private ResultCallback<DriveIdResult> rootFolderCallback = new ResultCallback<DriveIdResult>() {
        @Override
        public void onResult(DriveIdResult result) {
        	//read not success
            if (!result.getStatus().isSuccess()) {
                showMessage("Cannot find DriveId. Are you authorized to vipew this file?");
                return;
            }
            
            DriveId driveId = result.getDriveId();
            DriveFolder folder = Drive.DriveApi.getFolder(getGoogleApiClient(),driveId);;
            folder.listChildren(getGoogleApiClient())
                    .setResultCallback(metadataResult);
        }
    };

    /*
     * callback function
     * read subfolders of PhotoSyn
     */
    final private ResultCallback<MetadataBufferResult> metadataResult = new
            ResultCallback<MetadataBufferResult>() {
        @Override
        public void onResult(MetadataBufferResult result) {
        	//read not success
            if (!result.getStatus().isSuccess()) {
                showMessage("Problem while retrieving files");
                return;
            }
            //TODO:
            //condition check, no PhotoSyn has been created
            
            //PhotoSyn found
            showMessage("Listed " + result.getMetadataBuffer().getCount()
					+ " files.");
            DriveId driveId = result.getMetadataBuffer().get(0).getDriveId();
            DriveFolder folder = Drive.DriveApi.getFolder(getGoogleApiClient(),driveId);;
            //fetch subfolders of PhotoSyn
            folder.listChildren(getGoogleApiClient())
                    .setResultCallback(folderDataResult);
            result.getMetadataBuffer().release();
        }
    };
    
    final private ResultCallback<MetadataBufferResult> folderDataResult = new
            ResultCallback<MetadataBufferResult>() {
        @Override
        public void onResult(MetadataBufferResult result) {
        	//read not success
            if (!result.getStatus().isSuccess()) {
                showMessage("Problem while retrieving files");
                return;
            }
            //TODO:
            //no subfolder exists
            
            //subfolder found
            showMessage("Listed " + result.getMetadataBuffer().getCount()
					+ " files/folders.");
            for (int i = 0; i < result.getMetadataBuffer().getCount(); i++){
            	if ((result.getMetadataBuffer().get(i).isFolder()) && (!result.getMetadataBuffer().get(i).isTrashed()) ){
            		System.out.println(result.getMetadataBuffer().get(i).getTitle());
            		DriveId driveId = result.getMetadataBuffer().get(i).getDriveId();
                    final DriveFolder folder = Drive.DriveApi.getFolder(getGoogleApiClient(),driveId);
            
                    //fetch images contained in subfolder
					folder.listChildren(getGoogleApiClient()).setResultCallback(imageNameResult);
                    
            	}
            	
            }
            result.getMetadataBuffer().release();
        }
    };
    final private ResultCallback<MetadataBufferResult> imageNameResult = new
            ResultCallback<MetadataBufferResult>() {
        @Override
        public void onResult(MetadataBufferResult result) {
        	//read not success
            if (!result.getStatus().isSuccess()) {
                showMessage("Problem while retrieving files");
                return;
            }
            //TODO:
            //no image exists
            
            //images found
            showMessage("Listed " + result.getMetadataBuffer().getCount()
					+ " files.");
            for (int i = 0; i < result.getMetadataBuffer().getCount(); i++){
            	if ((!result.getMetadataBuffer().get(i).isFolder()) && (!result.getMetadataBuffer().get(i).isTrashed()) ){
            		System.out.println(result.getMetadataBuffer().get(i).getTitle());
            		final DriveFile file = Drive.DriveApi.getFile(getGoogleApiClient(),
    						result.getMetadataBuffer().get(i).getDriveId());
            		new Thread(new Runnable() { 
                        public void run(){        
                        	file.open(getGoogleApiClient(), DriveFile.MODE_READ_ONLY, null)
    						.setResultCallback(contentsOpenedCallback);
                        }
                    }).start();
            	}
            	
            }
            result.getMetadataBuffer().release();
        }
    };
    
    /*
     * callback function
     * fetch image data from google drive
     */
	ResultCallback<DriveContentsResult> contentsOpenedCallback = new ResultCallback<DriveContentsResult>() {
		@Override
		public void onResult(DriveContentsResult result) {
			//read not successful
			if (!result.getStatus().isSuccess()) {
				//TODO:
				// display an error saying file can't be opened
				return;
			}
			// DriveContents object contains pointers
			// to the actual byte stream
			final DriveContents contents = result.getDriveContents();
			final String fileName = contents.getDriveId().getResourceId().toString();
			DriveContents driveContents = result.getDriveContents();
			System.out.println(driveContents.getParcelFileDescriptor().getFileDescriptor().toString());
            InputStream is = driveContents.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(is);
			savePhoto(bitmap, fileName);
			try {
				is.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	
		}
	};


	/**
	 * save photo to the gallery
	 * 
	 * @param view
	 */
	public void savePhoto(Bitmap bitmap, String fileName) {
		MediaStore.Images.Media.insertImage(getContentResolver(),bitmap,
					fileName, "Photo Syn backup");
	}

}

