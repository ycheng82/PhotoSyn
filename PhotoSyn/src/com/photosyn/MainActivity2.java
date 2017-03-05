package com.photosyn;

import com.parse.Parse;
import com.parse.ParseObject;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;

import android.support.v7.app.ActionBarActivity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainActivity2 extends ActionBarActivity{


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main2);
		
		/*test parse cloud*/
		// Enable Local Datastore.
		//Parse.enableLocalDatastore(this);
		 
		//Parse.initialize(this, "qQ3nPze0VeXvxGQrUCXG5jAtXVhKr3SNy8oLFyA2", "eMAICaoG5FGYMRdRiiaN4azVYAbBtdgRG4zyh2DR");
	
		//ParseObject testObject = new ParseObject("TestObject");
		//testObject.put("foo", "bar");
		//testObject.saveInBackground();
		/*end of test*/


		
		
		
		
		
	}
	
	@Override
	protected void onStart() {
	    super.onStart();

	}



	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * show photos on phone
	 * @param view
	 */
	public void showPhonePhoto(View view) {
		Intent phonePhotoIntent = new Intent(this, PhonePhotoActivity.class);
	    startActivity(phonePhotoIntent);

	 }
	
	
	/**
	 * show photos from google drive
	 * @param view
	 */
	public void showDrivePhoto(View view) {
		Intent drivePhotoIntent = new Intent(this, DrivePhotoActivity.class);
	    startActivity(drivePhotoIntent);

	 }

}
