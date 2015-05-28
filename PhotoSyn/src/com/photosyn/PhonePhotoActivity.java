package com.photosyn;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.DriveIdResult;
import com.google.android.gms.drive.DriveApi.MetadataBufferResult;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveFolder.DriveFileResult;
import com.google.android.gms.drive.DriveFolder.DriveFolderResult;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.DriveApi.DriveContentsResult;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

public class PhonePhotoActivity extends Activity implements
		ConnectionCallbacks, OnConnectionFailedListener, OnItemSelectedListener {

	private GridView gridGallery;
	private Handler handler;
	private GalleryAdapter adapter;

	private ImageView imgSinglePick;
	private Button btnGalleryPick;
	private Button btnGalleryPickMul;

	private Button btnSyncSec, btnSynAll, btnSyncTest;

	private String action;
	private ViewSwitcher viewSwitcher;
	private ImageLoader imageLoader;

	private Bitmap mBitmapToSave;
	private static final String TAG = "save image to google drive";
	private GoogleApiClient mGoogleApiClient;
	private static final int REQUEST_CODE_CREATOR = 2;
	private static final int REQUEST_CODE_RESOLUTION = 3;
	private boolean isConnected = false;
	private String[] all_path;;
	private Spinner spFolder;

	private boolean isFolderCreated = false;
	private final String FOLDER_NAME = "PhotoSyn";
	private String subFolderName;
	DriveId parentFolderId;

	public static String EXISTING_FOLDER_ID;
	DriveId PHOTOSYN_FOLDER_ID;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_phonephoto);

		initImageLoader();
		init();
	}

	private void initImageLoader() {
		DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
				.cacheOnDisc().imageScaleType(ImageScaleType.EXACTLY_STRETCHED)
				.bitmapConfig(Bitmap.Config.RGB_565).build();
		ImageLoaderConfiguration.Builder builder = new ImageLoaderConfiguration.Builder(
				this).defaultDisplayImageOptions(defaultOptions).memoryCache(
				new WeakMemoryCache());

		ImageLoaderConfiguration config = builder.build();
		imageLoader = ImageLoader.getInstance();
		imageLoader.init(config);
	}

	private void init() {

		handler = new Handler();
		gridGallery = (GridView) findViewById(R.id.gridGallery);
		gridGallery.setFastScrollEnabled(true);
		adapter = new GalleryAdapter(getApplicationContext(), imageLoader);
		adapter.setMultiplePick(false);
		gridGallery.setAdapter(adapter);

		viewSwitcher = (ViewSwitcher) findViewById(R.id.viewSwitcher);
		viewSwitcher.setDisplayedChild(1);

		imgSinglePick = (ImageView) findViewById(R.id.imgSinglePick);

		btnGalleryPick = (Button) findViewById(R.id.btnGalleryPick);
		btnGalleryPick.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				Intent i = new Intent(Action.ACTION_PICK);
				startActivityForResult(i, 100);

			}
		});

		btnGalleryPickMul = (Button) findViewById(R.id.btnGalleryPickMul);
		btnGalleryPickMul.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent i = new Intent(Action.ACTION_MULTIPLE_PICK);
				startActivityForResult(i, 200);
			}
		});

		btnSyncSec = (Button) findViewById(R.id.btnGallerySyncSec);
		btnSyncSec.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// check if any images selected, and sync to google drive
				Log.i(TAG, "sync---");
				Log.i(TAG, "all_path.length is: " + all_path.length);
				Log.i(TAG, "isConnected: " + isConnected);
				if (isConnected) {
					String folderName = subFolderName; // needs user input
														// later
					for (int i = 0; i < all_path.length; i++) {
						Log.e("multiple file path: ", all_path[i]);
						File imgFile = new File(all_path[i]);
						if (imgFile.exists()) {

							mBitmapToSave = BitmapFactory.decodeFile(imgFile
									.getAbsolutePath());
							saveFileToDrive(folderName + i);
						}
					}

				}

			}
		});

		/* pop up window for user to select folder and other */
		btnSyncTest = (Button) findViewById(R.id.btnGallerySyncTest);
		btnSyncTest.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				LayoutInflater li = getLayoutInflater();

				final View promptsView = li
						.inflate(R.layout.drive_dialog, null);

				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
						PhonePhotoActivity.this);

				alertDialogBuilder.setView(promptsView);

				alertDialogBuilder.setPositiveButton(R.string.btn_save,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								// set new album folder name here
								EditText mEdit = (EditText) promptsView
										.findViewById(R.id.et_name);
								subFolderName = mEdit.getText().toString();

								for (int i = 0; i < all_path.length; i++) {
									Log.e("multiple file path: ", all_path[i]);
									File imgFile = new File(all_path[i]);
									if (imgFile.exists()) {

										mBitmapToSave = BitmapFactory
												.decodeFile(imgFile
														.getAbsolutePath());
										saveFileToDrive(subFolderName + i);
									}
								}
							}
						});

				alertDialogBuilder.setNegativeButton(R.string.btn_cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								// do something here

							}
						});

				final AlertDialog alertDialog = alertDialogBuilder.create();

				// Spinner element
				spFolder = (Spinner) promptsView.findViewById(R.id.sp_folder);

				// Loading spinner data from database
				loadDriveFolderData();

				// Spinner click listener
				spFolder.setOnItemSelectedListener(PhonePhotoActivity.this);

				alertDialog.show();
			}
		});

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
			adapter.clear();

			viewSwitcher.setDisplayedChild(1);
			String single_path = data.getStringExtra("single_path");
			Log.e("sing file path: ", single_path);
			imageLoader.displayImage("file://" + single_path, imgSinglePick);

			File imgFile = new File(single_path);
			if (imgFile.exists()) {
				mBitmapToSave = BitmapFactory.decodeFile(imgFile
						.getAbsolutePath());
			}

		} else if (requestCode == 200 && resultCode == Activity.RESULT_OK) {
			all_path = data.getStringArrayExtra("all_path");

			ArrayList<CustomGallery> dataT = new ArrayList<CustomGallery>();

			for (String string : all_path) {
				// Log.e("multiple file path: ", string);
				CustomGallery item = new CustomGallery();
				item.sdcardPath = string;

				dataT.add(item);
			}

			viewSwitcher.setDisplayedChild(0);
			adapter.addAll(dataT);
		}
	}

	/**
	 * Create a new file and save it to Drive.
	 */
	private void saveFileToDrive(final String folderName) {
		// Start by creating a new contents, and setting a callback.
		Log.i(TAG, "Creating new contents.");
		final Bitmap image = mBitmapToSave;
		Drive.DriveApi.newDriveContents(mGoogleApiClient).setResultCallback(
				new ResultCallback<DriveContentsResult>() {

					@Override
					public void onResult(DriveContentsResult result) {
						// If the operation was not successful, we cannot do
						// anything
						// and must
						// fail.
						if (!result.getStatus().isSuccess()) {
							Log.i(TAG, "Failed to create new contents.");
							return;
						}
						// Otherwise, we can write our data to the new contents.
						Log.i(TAG, "New contents created.");
						// Get an output stream for the contents.
						OutputStream outputStream = result.getDriveContents()
								.getOutputStream();
						// Write the bitmap data from it.
						ByteArrayOutputStream bitmapStream = new ByteArrayOutputStream();
						image.compress(Bitmap.CompressFormat.PNG, 100,
								bitmapStream);
						try {
							outputStream.write(bitmapStream.toByteArray());
						} catch (IOException e1) {
							Log.i(TAG, "Unable to write file contents.");
						}
						// Create the initial metadata - MIME type and title.
						// Note that the user will be able to change the title
						// later.
						MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
								.setMimeType("image/jpeg")
								.setTitle(folderName + ".png").build();
						// Create an intent for the file chooser, and start it.
						/*
						 * IntentSender intentSender = Drive.DriveApi
						 * .newCreateFileActivityBuilder()
						 * .setInitialMetadata(metadataChangeSet)
						 * .setInitialDriveContents( result.getDriveContents())
						 * .build(mGoogleApiClient); try {
						 * startIntentSenderForResult(intentSender,
						 * REQUEST_CODE_CREATOR, null, 0, 0, 0);
						 * Toast.makeText(PhonePhotoActivity.this,
						 * "Photo sync successfully!",
						 * Toast.LENGTH_SHORT).show();
						 * 
						 * } catch (SendIntentException e) { Log.i(TAG,
						 * "Failed to launch file chooser."); }
						 */

						final DriveContents driveContents = result
								.getDriveContents();
						final ResultCallback<DriveFileResult> fileCallback = new ResultCallback<DriveFileResult>() {
							@Override
							public void onResult(DriveFileResult result) {
								if (!result.getStatus().isSuccess()) {
									showMessage("Error while trying to create the file");
									return;
								}
								showMessage("Created a file with content: "
										+ result.getDriveFile().getDriveId());
							}
						};
						/*
						 * Drive.DriveApi .getRootFolder(getGoogleApiClient())
						 * .createFile(getGoogleApiClient(), metadataChangeSet,
						 * driveContents) .setResultCallback(fileCallback);
						 */
						Drive.DriveApi
								.getFolder(getGoogleApiClient(), parentFolderId)
								.createFile(getGoogleApiClient(),
										metadataChangeSet, driveContents)
								.setResultCallback(fileCallback);

					}
				});
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mGoogleApiClient == null) {
			// Create the API client and bind it to an instance variable.
			// We use this instance as the callback for connection and
			// connection
			// failures.
			// Since no account name is passed, the user is prompted to choose.
			mGoogleApiClient = new GoogleApiClient.Builder(this)
					.addApi(Drive.API).addScope(Drive.SCOPE_FILE)
					.addConnectionCallbacks(this)
					.addOnConnectionFailedListener(this).build();
		}
		// Connect the client. Once connected, the camera is launched.
		mGoogleApiClient.connect();
	}

	@Override
	protected void onPause() {
		if (mGoogleApiClient != null) {
			mGoogleApiClient.disconnect();
		}
		super.onPause();
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		// Called whenever the API client fails to connect.
		Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
		if (!result.hasResolution()) {
			// show the localized error dialog.
			GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this,
					0).show();
			return;
		}
		// The failure has a resolution. Resolve it.
		// Called typically when the app is not yet authorized, and an
		// authorization
		// dialog is displayed to the user.
		try {
			result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
		} catch (SendIntentException e) {
			Log.e(TAG, "Exception while starting resolution activity", e);
		}
	}

	@Override
	public void onConnected(Bundle connectionHint) {
		Log.i(TAG, "API client connected.");
		isConnected = true;

		// check if a sync folder has been created, if yes, retrieve it;
		// otherwise, create it

		EXISTING_FOLDER_ID = Drive.DriveApi.getRootFolder(getGoogleApiClient())
				.getDriveId().getResourceId();

		/*
		 * PHOTOSYN_FOLDER_ID =
		 * Drive.DriveApi.getFolder(getGoogleApiClient(),DriveId
		 * .decodeFromString("PhotoSyn")) .getDriveId().getResourceId();
		 * 
		 * Drive.DriveApi.fetchDriveId(getGoogleApiClient(),PHOTOSYN_FOLDER_ID)
		 * .setResultCallback(idCallback);
		 */

		/* query certain files */

		DriveFolder folder = Drive.DriveApi.getRootFolder(getGoogleApiClient());
		Query query = new Query.Builder().addFilter(
				Filters.and(Filters.eq(SearchableField.MIME_TYPE,
						"application/vnd.google-apps.folder"), Filters.eq(
						SearchableField.TITLE, FOLDER_NAME))).build();
		folder.queryChildren(getGoogleApiClient(), query).setResultCallback(
				childrenRetrievedCallback);

		/* create new folder */

		// DriveFolder folder =

		if (mBitmapToSave == null) {
			// This activity has no UI of its own. Just start the camera.
			// startActivityForResult(new
			// Intent(MediaStore.ACTION_IMAGE_CAPTURE),
			// REQUEST_CODE_CAPTURE_IMAGE);
			return;
		}

		// saveFileToDrive();

	}

	@Override
	public void onConnectionSuspended(int cause) {
		Log.i(TAG, "GoogleApiClient connection suspended");
	}

	/**
	 * Shows a toast message.
	 */
	public void showMessage(String message) {
		Toast.makeText(this, message, Toast.LENGTH_LONG).show();
	}

	/**
	 * Getter for the {@code GoogleApiClient}.
	 */
	public GoogleApiClient getGoogleApiClient() {
		return mGoogleApiClient;
	}

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub

	}

	/**
	 * Function to load the spinner data (folder information) for google drive
	 * */
	private void loadDriveFolderData() {
		List<String> lables = new ArrayList<String>();
		lables.add("a");
		lables.add("b");
		lables.add("c");

		// Creating adapter for spinner
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, lables);

		// Drop down layout style - list view with radio button
		dataAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		// attaching data adapter to spinner
		spFolder.setAdapter(dataAdapter);
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
		public void onResult(MetadataBufferResult result) {
			if (!result.getStatus().isSuccess()) {
				showMessage("Problem while retrieving files");
				return;
			}
			// mResultsAdapter.clear();
			// mResultsAdapter.append(result.getMetadataBuffer());
			showMessage("Successfully listed files.");
		}
	};

	ResultCallback<MetadataBufferResult> childrenRetrievedCallback = new ResultCallback<MetadataBufferResult>() {
		@Override
		public void onResult(MetadataBufferResult result) {
			if (!result.getStatus().isSuccess()) {
				showMessage("Problem while retrieving files");
				return;
			}
			// mResultsAdapter.clear();
			// mResultsAdapter.append(result.getMetadataBuffer());

			// NEEDS TO CHECK TRASHED ONE TO MAKE SURE THE FOLDER IS ACTUALLY
			// CREATED
			if (result.getMetadataBuffer().getCount() > 0) {
				isFolderCreated = true;

			}
			showMessage("Successfully listed"
					+ result.getMetadataBuffer().getCount() + " files.");
			MetadataBuffer buffer = result.getMetadataBuffer();
			for (Metadata m : buffer) {
				Log.i(TAG, "Metadata name  " + m.getTitle());
			}

			DriveFolder folder = Drive.DriveApi
					.getRootFolder(getGoogleApiClient());
			if (!isFolderCreated) {
				MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
						.setTitle(FOLDER_NAME).build();
				folder.createFolder(getGoogleApiClient(), changeSet)
						.setResultCallback(folderCreatedCallback);

			} else {
				parentFolderId = result.getMetadataBuffer().get(0).getDriveId();
			}

		}
	};

	ResultCallback<DriveFolderResult> folderCreatedCallback = new ResultCallback<DriveFolderResult>() {
		@Override
		public void onResult(DriveFolderResult result) {
			if (!result.getStatus().isSuccess()) {
				showMessage("Problem while trying to create a folder");
				return;
			}
			showMessage("Folder succesfully created");
			parentFolderId = result.getDriveFolder().getDriveId();
		}
	};

}
