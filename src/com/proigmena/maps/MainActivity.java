package com.proigmena.maps;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class MainActivity extends Activity implements OnMapClickListener, OnMapLongClickListener{

	//Google directions progress
	private ProgressDialog pDialog;
	
	//Initialize vars to calculate directions
	//START
    LatLng startDirectionsLocation = null;
    //END
    LatLng endDirectionsLocation = null;
    
    String directionsMode = "driving";
    
	//Create Google map var
	private GoogleMap mMap;
	//Create db instance
	MarkerDataBaseAdapter markerDataBaseAdapter;
	//ArrayList contains all markers
	List<Marker> markers = new ArrayList<Marker>();
	
	private boolean isMarkerShowEnabled;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// get Instance  of Database Adapter
	    markerDataBaseAdapter = new MarkerDataBaseAdapter(this);
	    markerDataBaseAdapter = markerDataBaseAdapter.open();
		
		InitMap();
		
		mMap.setOnMapLongClickListener(this);
		//Enable markers show action
		isMarkerShowEnabled = true;
		
		
		mMap.setOnInfoWindowClickListener(
			  new OnInfoWindowClickListener(){
			    public void onInfoWindowClick(final Marker marker){
			    	
			    	Log.d("Chris","Info pressed");
			    	//Open dialog menu
			    	final Dialog dialog = new Dialog(MainActivity.this);
			        dialog.setContentView(R.layout.dialoginfowindow);
			        dialog.setTitle(marker.getTitle());
			        
			        //Set onclick listeners for all buttons: Edit, Delete, Get Directions, Share
			        //***DELETE BUTTON
			        Button buttonDeleteMarker = (Button)dialog.findViewById(R.id.buttonDeleteMarker);
			        buttonDeleteMarker.setOnClickListener(new View.OnClickListener() {

			            public void onClick(View v) {
			            	
			            	
			        	    
			            	int deletedMarkers = markerDataBaseAdapter.deleteMarker(marker.getPosition());
			            	if(deletedMarkers == 1){
			            		marker.remove();
			            		Toast.makeText(getApplicationContext(),
			    	                    R.string.toast_marker_deleted, Toast.LENGTH_SHORT)
			    	                    .show(); 
			            	}
			            	dialog.dismiss();
			            }
			        });
			        //***EDIT BUTTON
			        Button buttonEditMarker = (Button)dialog.findViewById(R.id.buttonEditMarker);
			        buttonEditMarker.setOnClickListener(new View.OnClickListener() {

			            public void onClick(View v) {
			            	//Alter dialog layout
			            	dialog.setContentView(R.layout.insertmarker);
			            	dialog.setTitle(R.string.dialog_edit_title);
			            	//Complete form to edit
			            	final  EditText editTextMarkerTitle = (EditText)dialog.findViewById(R.id.editTextMarkerTitle);
			                final  EditText editTextMarkerDesc = (EditText)dialog.findViewById(R.id.editTextMarkerDesc);
			                final  Spinner editTextMarkerCat = (Spinner)dialog.findViewById(R.id.editTextMarkerCat);
			                editTextMarkerTitle.setText(marker.getTitle());
			                editTextMarkerDesc.setText(marker.getSnippet());
			            	//handle alter button click
			                Button buttonSaveMarker = (Button)dialog.findViewById(R.id.buttonSaveMarker);
			                buttonSaveMarker.setOnClickListener(new View.OnClickListener() {
			                	 public void onClick(View v) {
			                		 //Save to db, alter marker live
			                		 // get The User name and Password
			                         String markerTitle = editTextMarkerTitle.getText().toString();
			                         String markerDesc = editTextMarkerDesc.getText().toString();
			                         String markerCat = editTextMarkerCat.getSelectedItem().toString();
			                         
			                         // check if any of the fields are empty
			                         if(markerTitle.equals("") || markerDesc.equals(""))
			                         {
			                             Toast.makeText(getApplicationContext(), R.string.toast_fields_required, Toast.LENGTH_LONG).show();
			                             return;
			                         }
			                         // Save the Data in Database
			                         int updated = markerDataBaseAdapter.updateMarker(markerTitle, markerDesc, markerCat, marker.getPosition());
			                         if(updated > 0) {
			                        	 Toast.makeText(getApplicationContext(), R.string.toast_marker_saved, Toast.LENGTH_LONG).show();
				                         //alter marker
				                         marker.setTitle(markerTitle);
				                         marker.setSnippet(markerDesc);
				                         
			                         }
			                         
			                         dialog.dismiss();
			                	 }
			                });
			                //hanfle cancel button
			                Button buttonCancelSaveMarker = (Button)dialog.findViewById(R.id.buttonCancelSaveMarker);
			                buttonCancelSaveMarker.setOnClickListener(new View.OnClickListener() {
			                	 public void onClick(View v) {
			                		 dialog.dismiss(); 
			                	 }
			                });
			            	//save to db
			            	//dialog dismiss
			            	
			            }
			        });
			        //***GETDIRECTIONS
			        Button buttonGetDirToMarker = (Button)dialog.findViewById(R.id.buttonGetDirection);
			        buttonGetDirToMarker.setOnClickListener(new View.OnClickListener() {

			            public void onClick(View v) {
			            	
			            	//Open dialog for directions info
			    			final Dialog dialog = new Dialog(MainActivity.this);
			    	        dialog.setContentView(R.layout.getdirectionsmarker);
			    	        dialog.setTitle(R.string.menu_get_directions);
			    	        //Set start Location
			    	        handleStartSpinnerLocationOption(dialog);
			    	        //Mode
			    	        handleDirModeOption(dialog);
			            	//Set end to marker
			            	endDirectionsLocation = marker.getPosition();
			            	
			            	//Init Go button
			    	        Button goButton = (Button)dialog.findViewById(R.id.dirGoButton);
			    	        goButton.setOnClickListener(new View.OnClickListener(){
			    	        	public void onClick(View v) {
			    	        		 //Check directionsMode
			    	        		if(startDirectionsLocation == null|| endDirectionsLocation == null) {
			    	        			Toast.makeText(getApplicationContext(), R.string.toast_fields_required,  Toast.LENGTH_LONG).show();
			    	        			return;
			    	        		}

			    	        		new GoogleDirections().execute(buildApiQuery());
			    	        		//hide dialog
			    	        		dialog.dismiss();
			    	        		
			    	        	 }
			    	        });
			    	        
			    	        dialog.show();
			            }
			        });
			        //***SHARE
			        //TODO: 
			       
			    	dialog.show();
			    }
			  }
			);
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.mainmenu, menu);
		return true;
	}
	/**
	* Function to ensure that map is available
	**/
	private void InitMap() {
	    // Do a null check to confirm that we have not already instantiated the map.
	    if (mMap == null) {
	        mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
	                            .getMap();
	        // Check if we were successful in obtaining the map.
	        if (mMap == null) {
	        	//Map is not loaded 
	        	Toast.makeText(getApplicationContext(),
	                    "Sorry, map is not available!", Toast.LENGTH_SHORT)
	                    .show();       	
	        }
	    }
	}
	
	/**
	 * Function to callback click for the options menu
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == R.id.insertmarker) {
			//Set map on click listerner
			mMap.setOnMapClickListener(this);
			//Show user message to click anywhere he wants
			Toast.makeText(getApplicationContext(), R.string.toast_select_location,  Toast.LENGTH_LONG).show();
		}else if(item.getItemId() == R.id.showmarkers) {
			//SHow all markers
			//Iterate db records and insert marker
			showAllMarkers();
		}else if(item.getItemId() == R.id.clearmap) {
			clearMap();
		}else if(item.getItemId() == R.id.getdirections) {
			
			//Root directions
			//Open dialog for directions info
			final Dialog dialog = new Dialog(MainActivity.this);
	        dialog.setContentView(R.layout.getdirectionsroot);
	        dialog.setTitle(R.string.menu_get_directions);
	        
	        //StartLocation
	        handleStartSpinnerLocationOption(dialog);
     
	        //End 
	        handleEndSpinnerLocationOption(dialog);
	        
	        //Mode
	        handleDirModeOption(dialog);
	        
	        //Init Go button
	        Button goButton = (Button)dialog.findViewById(R.id.dirGoButton);
	        goButton.setOnClickListener(new View.OnClickListener(){
	        	public void onClick(View v) {
	        		 //Check directionsMode
	        		if(startDirectionsLocation == null || endDirectionsLocation == null) {
	        			Toast.makeText(getApplicationContext(), R.string.toast_fields_required,  Toast.LENGTH_LONG).show();
	        			return;
	        		}	        		
	        		//Get directions
	        		new GoogleDirections().execute(buildApiQuery());
	        		//hide dialog
	        		dialog.dismiss();        		
	        	 }
	        });
	        
	        dialog.show();
	        
	        
			//new GoogleDirections().execute("http://maps.googleapis.com/maps/api/directions/json?origin=-25.3641,-49.2857&destination=-25.3928,-49.2728&region=en&sensor=false");
			  
		}else if(item.getItemId() == R.id.settings){
			Intent intent = new Intent(this, SetPreferenceActivity.class);
		    startActivity(intent);
		}
		
		return true;
	}
	
	/**
	 * Function to handle the start Location Spinner for the Directions options
	 */
	private void handleStartSpinnerLocationOption(final Dialog dialog) {
		//START OPTIONS SPINNER
        Spinner startOptionsSpinner = (Spinner)dialog.findViewById(R.id.startLocationOption);
        
        startOptionsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
        	
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) { 

            	Object item = adapterView.getItemAtPosition(i);
            	Log.d("CHIRS","Item Selected Starting Point "+item.toString());
            	//Switch value and do things
            	String option = item.toString();
            	if( option.equals(getString(R.string.dir_option_point_map))) {
            		//dialog off
            		dialog.dismiss();
            		Toast.makeText(getApplicationContext(), R.string.toast_select_location_for_starting_point,  Toast.LENGTH_LONG).show();
            		//Set map listener get point
            		mMap.setOnMapClickListener(new OnMapClickListener() {

            	        @Override
            	        public void onMapClick(LatLng point) {
            	            Log.d("Map","Map clicked for Starting point");
            	            startDirectionsLocation = point;
            	            dialog.show();
            	            //delete listener
            	            mMap.setOnMapClickListener(null);
            	        }
            	    });
            	}else if( option.equals(getString(R.string.dir_option_gps))) {
        			//Get gps
            		GPSTracker gps = new GPSTracker(MainActivity.this);

                    // check if GPS enabled     
                    if(gps.canGetLocation()){

                        double latitude = gps.getLatitude();
                        double longitude = gps.getLongitude();
                        //Set starting point
                        startDirectionsLocation = new LatLng(latitude, longitude);
                        //Go to gps and zoom
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(startDirectionsLocation, 12));
                        
                        Log.d("CHRIS GPS", "GPS Location: "+latitude+", "+longitude);
                    }else{
                        // can't get location
                        // GPS or Network is not enabled
                        // Ask user to enable GPS/network in settings
                        gps.showSettingsAlert();
                    }
        		}else if( option.equals(getString(R.string.dir_option_write))) {
        			//Promt user to write
        			final Dialog inputDialog = new Dialog(MainActivity.this);
        			
        			inputDialog.setContentView(R.layout.inputlocation);
        			inputDialog.setTitle(R.string.dir_option_write);
        			Button goInputButton = (Button)inputDialog.findViewById(R.id.goInputButton);
        			
        			goInputButton.setOnClickListener(new View.OnClickListener(){
        	        	public void onClick(View v) {
        	        		EditText inputLocationText = (EditText)inputDialog.findViewById(R.id.inputLocationText);
        	        		String location = inputLocationText.getText().toString();
        	        		Log.d("CHRIS","Start Location: "+location);
        	        		//Location is a form like : lat,lng
        	        		List<String> latlngList = Arrays.asList(location.split("\\s*,\\s*"));
        	        		if(latlngList.size() == 2) {
        	        			inputDialog.dismiss();
        	        			startDirectionsLocation = new LatLng(Double.parseDouble(latlngList.get(0)),Double.parseDouble(latlngList.get(1)));
        	        		}else{
        	        			Toast.makeText(getApplicationContext(), R.string.toast_no_results_try_again,  Toast.LENGTH_LONG).show();
        	        		}

        	        	}
        			});
        			inputDialog.show();
        		}//end of switch options
            	
            }
            public void onNothingSelected(AdapterView<?> adapterView) {
                return;
            } 
        }); 
	}
	/**
	 * Function to handle dir mode change
	 */
	private void handleDirModeOption(Dialog dialog) {
		Spinner dirModeSpinner = (Spinner)dialog.findViewById(R.id.dirMode);
        
		dirModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
        	
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) { 

            	Object item = adapterView.getItemAtPosition(i);
            	Log.d("CHIRS","Item Selected Dir mode "+item.toString());
            	//Switch value and do things
            	String option = item.toString();
            	if( option.equals(getString(R.string.dir_mode_bicycling))) {
            		directionsMode = "bicycling";
            	}else if( option.equals(getString(R.string.dir_mode_driving))) {
            		directionsMode = "driving";
            	}else if( option.equals(getString(R.string.dir_mode_walking))) {
            		directionsMode = "walking";
            	}
            	
            }
            public void onNothingSelected(AdapterView<?> adapterView) {
                return;
            } 
        });	
	}
	/**
	 * Function to handle the end Location Spinner for the Directions options
	 */
	private void handleEndSpinnerLocationOption(final Dialog dialog) {
		Spinner endOptionsSpinner = (Spinner)dialog.findViewById(R.id.endLocationOption);
        endOptionsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) { 

            	Object item = adapterView.getItemAtPosition(i);
            	Log.d("CHIRS","Item Selected Ending Point "+item.toString());
            	//Switch value and do things
            	String option = item.toString();
            	if( option.equals(getString(R.string.dir_option_point_map))) {
            		//dialog off
            		dialog.dismiss();
            		Toast.makeText(getApplicationContext(), R.string.toast_select_location_for_ending_point,  Toast.LENGTH_LONG).show();
            		//Set map listener get point
            		mMap.setOnMapClickListener(new OnMapClickListener() {

            	        @Override
            	        public void onMapClick(LatLng point) {
            	            Log.d("Map","Map clicked for Ending point");
            	            endDirectionsLocation = point;
            	            dialog.show();
            	            //delete listener
            	            mMap.setOnMapClickListener(null);
            	        }
            	    });
            	}else if( option.equals(getString(R.string.dir_option_write))) {
        			//Promt user to write
        			final Dialog inputDialog = new Dialog(MainActivity.this);
        			
        			inputDialog.setContentView(R.layout.inputlocation);
        			inputDialog.setTitle(R.string.dir_option_write);
        			Button goInputButton = (Button)inputDialog.findViewById(R.id.goInputButton);
        			
        			goInputButton.setOnClickListener(new View.OnClickListener(){
        	        	public void onClick(View v) {
        	        		EditText inputLocationText = (EditText)inputDialog.findViewById(R.id.inputLocationText);
        	        		String location = inputLocationText.getText().toString();
        	        		Log.d("CHRIS","Start Location: "+location);
        	        		//Location is a form like : lat,lng
        	        		List<String> latlngList = Arrays.asList(location.split("\\s*,\\s*"));
        	        		if(latlngList.size() == 2) {
        	        			inputDialog.dismiss();
        	        			endDirectionsLocation = new LatLng(Double.parseDouble(latlngList.get(0)),Double.parseDouble(latlngList.get(1)));
        	        		}else{
        	        			Toast.makeText(getApplicationContext(), R.string.toast_no_results_try_again,  Toast.LENGTH_LONG).show();
        	        		}

        	        	}
        			});
        			inputDialog.show();
        		}//end of switch options
            	
            }
            public void onNothingSelected(AdapterView<?> adapterView) {
                return;
            } 
        });
	}
	/*
	 * Function to build the google directions api url request
	 */
	private String buildApiQuery() {
		
		//Test sharedPrefs
		SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		String lang = mySharedPreferences.getString("setting_lang", "en");
		Log.d("CHRIS PREF", lang);
		String units = mySharedPreferences.getString("setting_units", "metric");
		Log.d("CHRIS PREF", units);
		
		String startLocParam = String.valueOf(startDirectionsLocation.latitude)+","+String.valueOf(startDirectionsLocation.longitude);	
		String endLocParam = String.valueOf(endDirectionsLocation.latitude)+","+String.valueOf(endDirectionsLocation.longitude);
		
		String url = "http://maps.googleapis.com/maps/api/directions/json?" 
					+ "origin=" + startLocParam + "&"
					+ "destination=" + endLocParam + "&sensor=false&"
					+ "mode=" + directionsMode + "&language=" + lang +"&units=" + units;
		Log.d("CHRIS", "URL: "+url);
		
		return url;
	}
	/**
	 * Function to catch the map click event
	 */
	@Override
	 public void onMapClick(LatLng point) {
//	 	mMap.animateCamera(CameraUpdateFactory.newLatLng(point));
		Log.d("Chris","Map clicked");
		//Delete map click listener
		mMap.setOnMapClickListener(null);
		//Open dialog, get lng, lat
		final Dialog dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.insertmarker);
        dialog.setTitle(R.string.dialog_title);
        
        
        // get the Refferences of views
        final  EditText editTextMarkerTitle = (EditText)dialog.findViewById(R.id.editTextMarkerTitle);
        final  EditText editTextMarkerDesc = (EditText)dialog.findViewById(R.id.editTextMarkerDesc);
        final  Spinner editTextMarkerCat = (Spinner)dialog.findViewById(R.id.editTextMarkerCat);

        Button buttonSaveMarker = (Button)dialog.findViewById(R.id.buttonSaveMarker);
        
        Button buttonCancelSaveMarker = (Button)dialog.findViewById(R.id.buttonCancelSaveMarker);
        buttonCancelSaveMarker.setOnClickListener(new View.OnClickListener() {
        	 public void onClick(View v) {
        		 dialog.dismiss(); 
        	 }
        });
        final LatLng finalPoint = point;
        // Set On ClickListener
        buttonSaveMarker.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                // get The User name and Password
                String markerTitle = editTextMarkerTitle.getText().toString();
                String markerDesc = editTextMarkerDesc.getText().toString();
                String markerCat = editTextMarkerCat.getSelectedItem().toString();
                
                // check if any of the fields are empty
                if(markerTitle.equals("") || markerDesc.equals(""))
                {
                    Toast.makeText(getApplicationContext(), R.string.toast_fields_required, Toast.LENGTH_LONG).show();
                    return;
                }
                // Save the Data in Database
                markerDataBaseAdapter.insertMarker(markerTitle, markerDesc, markerCat, finalPoint);
                
                //Toast.makeText(getApplicationContext(), R.string.toast_marker_saved, Toast.LENGTH_LONG).show();
                dialog.dismiss();
                Log.d("CHRIS",markerCat);
                
                float color = BitmapDescriptorFactory.HUE_AZURE;
                
                if( markerCat.equals(getString(R.string.category_food)) ) {
                	color = BitmapDescriptorFactory.HUE_ORANGE;
                }else if ( markerCat.equals(getString(R.string.category_cofee)) ) {
                	color = BitmapDescriptorFactory.HUE_RED;
                }else if ( markerCat.equals(getString(R.string.category_fun)) ) {
                	color = BitmapDescriptorFactory.HUE_CYAN;
                }
               
                Marker marker = mMap.addMarker(new MarkerOptions().position(finalPoint)
														            .title(markerTitle)
														            .snippet(markerDesc)
														            .icon(BitmapDescriptorFactory.defaultMarker(color)));
				markers.add(marker);
                
                
            }
        });
        
        dialog.show();
	 }
	/*
	 * Function to catch the map Long click event
	 */
	@Override
    public void onMapLongClick(LatLng point) {


		Geocoder geo = new Geocoder(getApplicationContext(), Locale.getDefault());   
		try {
			List<Address> myList = geo.getFromLocation(point.latitude, point.longitude, 1);
			
			Toast.makeText(getApplicationContext(),
	                "Pressed@"+ myList.size() + "@" + point.toString(), Toast.LENGTH_LONG)
	                .show();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.d("CHRIS GEO IOExheptions",e.getMessage());
			e.printStackTrace();
		}
		
        

    }
	/*
	 * Show all markers on the map, disable this action untill clear is called.
	 */
	public void showAllMarkers()
	{
		Log.d("Chris","showAllMarkers Entered");
		if(!isMarkerShowEnabled){
			Toast.makeText(getApplicationContext(), R.string.toast_markers_already_shown, Toast.LENGTH_LONG).show();
			return;
		}
		isMarkerShowEnabled = false;

		//query all markers
		Cursor cursor = markerDataBaseAdapter.getAllMarkers();
		
		//Itarate thoygh db markers
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			LatLng point = new LatLng( cursor.getDouble(cursor.getColumnIndex("LAT")), cursor.getDouble(cursor.getColumnIndex("LNG")) );
			float color = BitmapDescriptorFactory.HUE_AZURE;
            String markerCat = cursor.getString(cursor.getColumnIndex("CAT"));
            if( markerCat.equals(getString(R.string.category_food)) ) {
            	color = BitmapDescriptorFactory.HUE_ORANGE;
            }else if ( markerCat.equals(getString(R.string.category_cofee)) ) {
            	color = BitmapDescriptorFactory.HUE_RED;
            }else if ( markerCat.equals(getString(R.string.category_fun)) ) {
            	color = BitmapDescriptorFactory.HUE_CYAN;
            }
			Marker marker = mMap.addMarker(new MarkerOptions()
					            .position(point)
					            .title(cursor.getString(cursor.getColumnIndex("TITLE")))
					            .snippet(cursor.getString(cursor.getColumnIndex("DESC")))
					            .icon(BitmapDescriptorFactory.defaultMarker(color)));
			//TODO: Cat icons
			markers.add(marker);
			cursor.moveToNext();
		}
		cursor.close();
		
		//Log.d("Chris",markers.toString());
	}
	/*
	 * Clear all on the map, enable show all again
	 */
	public void clearMap() {
		mMap.clear();
		Toast.makeText(getApplicationContext(), R.string.toast_map_cleared, Toast.LENGTH_LONG).show();
		isMarkerShowEnabled = true;
	}
	
	
	@Override
    protected void onResume() {
		InitMap();
		markerDataBaseAdapter.open();
        super.onResume();
    }

    @Override
    protected void onPause() {
    	markerDataBaseAdapter.close();
        super.onPause();
    }
    
    /*
     * Method to decode polyline points
     */
    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    /*
     * Google directions class
    */
    class GoogleDirections extends AsyncTask<String, String, String>{

    	
    	
    	//Routes JSONArray
    	JSONArray routes = null;
    	
    	
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage(getString(R.string.pdialog_fetching_url));
            pDialog.setProgress(0);
            pDialog.setCancelable(false);
            pDialog.show();
 
        }
        
        @Override
        protected String doInBackground(String... uri) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response;
            String responseString = null;
            try {
                response = httpclient.execute(new HttpGet(uri[0]));
                StatusLine statusLine = response.getStatusLine();
                if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    out.close();
                    responseString = out.toString();
                } else{
                    //Closes the connection.
                    response.getEntity().getContent().close();
                    throw new IOException(statusLine.getReasonPhrase());
                }
            } catch (ClientProtocolException e) {
                //TODO Handle problems..
            } catch (IOException e) {
                //TODO Handle problems..
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            //Do anything with response..
            //Log.d("CHIRS",result);
            //Update Progress dialog to 50%
            pDialog.setMessage(getString(R.string.pdialog_proccess_url));
            pDialog.setProgress(50);
            try{
            	JSONObject jsonobj = new JSONObject(result);
            	
            	//Get JSON Array code
            	routes = jsonobj.getJSONArray("routes");
            	//Loop through routes
            	for(int i = 0; i < routes.length(); i++) {
            		JSONObject r = routes.getJSONObject(i);
            		
            		JSONArray legs = r.getJSONArray("legs");
            		//Loop legs 
            		for(int j = 0; j < legs.length(); j++) {
            			JSONObject leg = legs.getJSONObject(j);
            			//Get start Loc 
            			JSONObject startLocation = leg.getJSONObject("start_location");
            			String startLocationLat = startLocation.getString("lat");
            			String startAddress = leg.getString("start_address");
            			String startLocationLng = startLocation.getString("lng");
            			Log.d("CHRIS DEBUG START LAT",startLocationLat);
            			Log.d("CHRIS DEBUG START LNG",startLocationLng);
            			//Insert marker to map
            			mMap.addMarker(new MarkerOptions()
            						.position(new LatLng(Double.parseDouble(startLocationLat), Double.parseDouble(startLocationLng)))
            						.title(startAddress)
            						);
            			//Get end Loc
            			JSONObject endLocation = leg.getJSONObject("end_location");
            			String endLocationLat = endLocation.getString("lat");
            			String endLocationLng = endLocation.getString("lng");
            			String endAddress = leg.getString("end_address");
            			Log.d("CHRIS DEBUG END LAT",endLocationLat);
            			Log.d("CHRIS DEBUG END LNG",endLocationLng);
            			//Insert marker to map
            			mMap.addMarker(new MarkerOptions()
						.position(new LatLng(Double.parseDouble(endLocationLat), Double.parseDouble(endLocationLng)))
						.title(endAddress)
						.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
						);
            			//Now loop into steps and draw a line ! 
            			JSONArray steps = leg.getJSONArray("steps");
            			
            			for(int k = 0; k < steps.length(); k++) {
            				JSONObject step = steps.getJSONObject(k);
            				JSONObject stepStartLoc = step.getJSONObject("start_location");
            				//Get other info
            				String stepDuration = step.getJSONObject("duration").getString("text");
            				String stepDistance = step.getJSONObject("distance").getString("text");
            				//Get all encoded points for this step!
            				String stepPath = step.getJSONObject("polyline").getString("points");
            				//Add all location into pointsList
            				LatLng p = new LatLng(Double.parseDouble(stepStartLoc.getString("lat")), Double.parseDouble(stepStartLoc.getString("lng")));
            				//Add marker for each step
            				if(k > 0){ //DONT ADD MARKER ON START LOCATION
	            				mMap.addMarker(new MarkerOptions()
	            									.position(p)
	            									.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
	            									.title("Step "+k)
	            									.snippet("-Duration: "+stepDuration+" -Distance: "+stepDistance)
	            						);
            				}
            				
            				//Create poly
            				List<LatLng> points = decodePoly(stepPath); // list of latlng
            				
            				Log.d("CHRIS POLY", points.toString());
            				for (int w = 0; w < points.size() - 1; w++) {
	            				LatLng src = points.get(w);
	            				LatLng dest = points.get(w + 1);
	            				mMap.addPolyline(new PolylineOptions() 
            												.add(new LatLng(src.latitude, src.longitude), new LatLng(dest.latitude,dest.longitude))
            												.width(5)
            												.color(Color.RED)
	            											);
            				 }//Poly points loops per step
            				
            			}//Steps loop
            			
            		}//Legs loop	
            	}//Routes loop
            	pDialog.setProgress(90);
            } catch (JSONException e) {
            	Log.d("CHRIS JSONException", e.toString());
                e.printStackTrace();
            }
            
            // Dismiss the progress dialog
            if (pDialog.isShowing())
                pDialog.dismiss();
        }
    }
    
}

