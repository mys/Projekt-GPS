package com.miccelinski.gps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Contacts.People;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

/*********************************************************************************
 * Projekt GPS<br>
 * <br>
 * http://www.wimim.polsl.pl<br>
 * <br>
 * 1. GPS Permissions, LocationManager, LocationProvider, enabling GPS<br>
 * http://developer.android.com/training/basics/location/locationmanager.html<br>
 * <br>
 * 2. Building an Alert Dialog - na starcie, ListDialog - wybor miasta<br>
 * http://developer.android.com/guide/topics/ui/dialogs.html<br>
 * <br>
 * 3. LocationListener<br>
 * http://developer.android.com/training/basics/location/currentlocation.html<br>
 * <br>
 * 4. Cykl aktywnosci aplikacji ..link<br>
 * <br>
 * 5. MediaPlayback - sygnal fixa<br>
 * http://developer.android.com/guide/topics/media/mediaplayer.html<br>
 * <br>
 * 6. Localization strategies (network/gps)<br>
 * http://developer.android.com/guide/topics/location/strategies.html<br>
 * <br>
 * 7. Google maps, api, klucz<br>
 * http://developer.android.com/guide/topics/location/index.html<br>
 * <br>
 * 8. Geocoder<br>
 * http://developer.android.com/reference/android/location/Geocoder.html<br>
 * http://developer.android.com/training/basics/location/locationmanager.html<br>
 * 
 * 9. menus
 * http://developer.android.com/guide/topics/ui/menus.html
 * 
 * 10. sms/phone book
 * 
 * 11. zapis do pliku XML polozen
 * http://developer.android.com/guide/topics/data/data-storage.html
 * <br>
 * <br>
 * 
 * @author Micha³ Celiñski-Mys³aw
 *********************************************************************************/
public class MainActivity extends MapActivity {
	
	/******************************************************************************
	 * 
	 * Stored data
	 * 
	 ******************************************************************************/
	public static String PREFS_NAME = "MyPrefsFile";
	
	public List<Spot> spots = new ArrayList<Spot>();
	public Spot GLIWICE_AEI = new Spot("Gliwice - AEI", 50.288498, 18.677279);
	public Spot KATOWICE_DWORZEC = new Spot("Katowice - Dworzec g³ówny", 50.258459, 19.01736);
	public Spot TYCHY_DWORZEC = new Spot("Tychy - Dworzec P³n.", 50.13637, 18.964462);

	/******************************************************************************
	 * 
	 * GPS Position calculations
	 * 
	 ******************************************************************************/	
	// destination name and coordinates
	public String Name = "";
	public double[] cel = { 0, 0 };

	// our X and Y positions
	public double X;
	public double Y;

	// distance between us and target
	public double distance;

	// degrees between our direction and target
	public float degrees_target;
	// degrees between our direction and north pole
	public float degrees_north;

	// satellites found on sky
	public int count = 0;
	// satellites binded each other
	public int usedInFix = 0;
	// time required to obtain first position from gps
	public int firstFixTime = 0;

	/******************************************************************************
	 * 
	 * Togglers
	 * 
	 ******************************************************************************/
	public int showSatellite = 0;
	public int showMap = 0;
	public int showCompass = 0;
	public int showArrow = 0;

	/******************************************************************************
	 * 
	 * UI Composition widgets
	 * 
	 ******************************************************************************/
	public TextView textViewProvider;
	public TextView textViewSatelity;
	public TextView textViewWspolrzedne;
	public TextView textViewFirstFix;
	public TextView textViewCel;
	public TextView textViewDystans;
	public TextView textViewMoc;

	public Button buttonToggleSatellite;
	public Button buttonToggleMap;
	public Button buttonToggleCompass;
	public Button buttonChangeDest;
	public Button buttonMinus;
	public Button buttonPlus;

	public ImageView imageViewArrow;
	public ImageView imageViewCompass;

	public MapView mapView;

	/******************************************************************************
	 * 
	 * Location obtainers
	 * 
	 ******************************************************************************/
	public Handler mHandler;
	public LocationListener mLocationListener;
	public LocationManager mLocationManager;
	public GpsStatus.Listener mGpsStatusListener;
	public Location mCurrentBestLocation;

	/******************************************************************************
	 * 
	 * Sensors management
	 * 
	 ******************************************************************************/
	public SensorManager mSensorManager;
	public Sensor mAccelerometer;
	public Sensor mMangetometer;

	/******************************************************************************
	 * 
	 * Map controllers
	 * 
	 ******************************************************************************/
	public MyLocationOverlay mMyLocationOverlay;
	public MyLocationOverlay mTargetLocationOverlay;
	public MapController mMapController;

	/******************************************************************************
	 * 
	 * Geocoder
	 * 
	 ******************************************************************************/
	public Geocoder mGeocoder;

	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		textViewProvider = (TextView) findViewById(R.id.textViewProvider);
		textViewSatelity = (TextView) findViewById(R.id.textViewSatelity);
		textViewWspolrzedne = (TextView) findViewById(R.id.textViewWspolrzedne);
		textViewFirstFix = (TextView) findViewById(R.id.textViewFirstFix);
		textViewCel = (TextView) findViewById(R.id.textViewCel);
		textViewDystans = (TextView) findViewById(R.id.textViewDystans);
		textViewMoc = (TextView) findViewById(R.id.textViewMoc);
		buttonToggleSatellite = (Button) findViewById(R.id.buttonToggleSatellite);
		buttonToggleMap = (Button) findViewById(R.id.buttonToggleMap);
		buttonToggleCompass = (Button) findViewById(R.id.buttonToggleCompass);
		buttonChangeDest = (Button) findViewById(R.id.buttonZmienCel);
		buttonMinus = (Button) findViewById(R.id.buttonMinus);
		buttonPlus = (Button) findViewById(R.id.buttonPlus);
		imageViewArrow = (ImageView) findViewById(R.id.imageViewArrow);
		imageViewCompass = (ImageView) findViewById(R.id.imageViewCompass);
		mapView = (MapView) findViewById(R.id.mapView);
		
		LoadSpots();

		mMyLocationOverlay = new MyLocationOverlay(this, mapView);
		mapView.getOverlays().add(mMyLocationOverlay);
		mapView.postInvalidate();

		textViewProvider.setText("Naziemnie (NETWORK)");
		textViewDystans.setText("Wybierz cel!");
		textViewCel
				.setText("Cel: " + Name + "\n" + String.format(Locale.US ,"%.4f", cel[0]) + ", " + String.format(Locale.US ,"%.4f", cel[1]));
		textViewFirstFix.setText("");
		
		imageViewArrow.setVisibility(View.INVISIBLE);
		imageViewArrow.setScaleX(0.70f);
		imageViewArrow.setScaleY(0.70f);
		imageViewCompass.setVisibility(View.INVISIBLE);
		mapView.setVisibility(View.INVISIBLE);
		buttonMinus.setVisibility(View.INVISIBLE);
		buttonPlus.setVisibility(View.INVISIBLE);
		
		buttonMinus.setOnTouchListener(new View.OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				mMapController.zoomOut();
				return false;
			}
		});
		
		buttonPlus.setOnTouchListener(new View.OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				mMapController.zoomIn();
				return false;
			}
		});
		

		// Handler for updating text fields on the UI
		mHandler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case 0:
					textViewWspolrzedne.setText((String) msg.obj);
					break;
				case 1:
					textViewProvider.setText((String) msg.obj);
					break;
				}
			}
		};

		// gps
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		mLocationListener = LocationListener;
		mGpsStatusListener = GpsStatusListener;
		mLocationManager.addGpsStatusListener(mGpsStatusListener);

		// sensors
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mAccelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mMangetometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStart()
	 */
	@Override
	public void onStart() {
		super.onStart();

		// This verification should be done during onStart() because the system
		// calls this method when the user returns to the activity, which
		// ensures the desired location provider is enabled each time the
		// activity resumes from the stopped state.
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		boolean gpsEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
				mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

		if (!gpsEnabled) {
			// Build an alert dialog here that requests that the user enable
			// the location services, then when the user clicks the "OK" button,
			// call enableLocationSettings()

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setIcon(android.R.drawable.ic_menu_mylocation);
			builder.setTitle(R.string.dialog_gps_title).setMessage(
					R.string.dialog_gps_message);
			builder.setPositiveButton(R.string.ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							enableLocationSettings();
						}
					});
			builder.setNegativeButton(R.string.cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							// User cancelled the dialog
						}
					});
			AlertDialog dialog = builder.create();
			dialog.show();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	public void onResume() {
		super.onResume();

		// onResume is always called after onStart, even if the app hasn'tbeen
		// paused
		// add location listener and request updates every 5000ms or 10m
		mLocationManager.requestLocationUpdates(
				LocationManager.NETWORK_PROVIDER, 5000, 10, mLocationListener);
		mLocationManager.requestLocationUpdates(
				LocationManager.GPS_PROVIDER, 5000, 10, mLocationListener);
		
		SetLastPosition(mLocationManager.getLastKnownLocation(
				LocationManager.NETWORK_PROVIDER));

		mSensorManager.registerListener(SensorEventListener, mAccelerometer,
				SensorManager.SENSOR_DELAY_UI);
		mSensorManager.registerListener(SensorEventListener, mMangetometer,
				SensorManager.SENSOR_DELAY_UI);

		// mMyLocationOverlay.enableMyLocation();
		// mMapController = mapView.getController();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	public void onPause() {
		super.onPause();

		// GPS, as it turns out, consumes battery like crazy
		mLocationManager.removeUpdates(mLocationListener);

		// Same with sensors
		mSensorManager.unregisterListener(SensorEventListener);

		// and location on mapView
		mMyLocationOverlay.disableMyLocation();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.activity_main, menu);
	    return true;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    	case R.id.menu_delete:
	    		
	    		String[] polozenia = new String[spots.size()-3];
	    		for(int i = 3; i < spots.size(); i++){   
	    			polozenia[i-3] = spots.get(i).Name; 
	    		}
	    		
	    		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
	    		builder.setTitle("Usuñ lokalizacjê:").setItems(polozenia,
	    				new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								spots.remove(which+3);
								SaveSpots();
								
								Toast.makeText(MainActivity.this, "Pomyœlnie usuniêto lokalizacjê", Toast.LENGTH_LONG).show();
							}
						}
				);
	    		AlertDialog dialog = builder.create();
				dialog.show();
	    		
	    		return true;
	        case R.id.menu_send:
                
                Intent smsIntent = new Intent(Intent.ACTION_VIEW);    
                smsIntent.setType("vnd.android-dir/mms-sms");
                smsIntent.putExtra("sms_body", "[Wiadomoœæ z aplikacji Projekt GPS]\nData: " + Calendar.getInstance().getTime().toLocaleString() + "\nWspó³rzêdne: [" + Y + ", " + X + "]");
                startActivity(smsIntent);
	        	
	            return true;
	        case R.id.menu_about:
	        	
	        	AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
				builder2.setIcon(android.R.drawable.ic_menu_help);
				builder2.setTitle("Projekt in¿ynierski").setMessage(
						"Aplikacja mobilna wykorzystuj¹ca GPS do wskazywania po³o¿enia obiektów na otwartych przestrzeniach\n\n" +
						"Autorzy:\nMicha³ CELIÑSKI-MYS£AW\nDr Marcin PILARCZYK");
				
				AlertDialog dialog2 = builder2.create();
				dialog2.show();
				
				return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	
	/**
	 * Get last known position for quick start
	 * 
	 * @param location
	 */
	public void SetLastPosition(Location location){
		// A new location update is received. Do something useful with it.
		// In this case, we're sending the update to a handler which then
		// updates the UI with the new location.
		Message.obtain(
				mHandler,
				0,
				"Szerokoœæ: " + String.format(Locale.US ,"%.4f", location.getLatitude()) + ", \nD³ugoœæ: "
						+ String.format(Locale.US ,"%.4f", location.getLongitude())).sendToTarget();

		X = location.getLongitude();
		Y = location.getLatitude();

		setRotation();
		onDraw();
		calcDistance();
		setMap();
	}
	
	/**
	 * Restore spots from preferences file
	 */
	public void LoadSpots(){
		spots.clear();
		spots.add(GLIWICE_AEI);
		spots.add(KATOWICE_DWORZEC);
		spots.add(TYCHY_DWORZEC);
		
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        int pos = 0;
        while (settings.getString("Name" + pos, "") != ""){
        	spots.add(new Spot(settings.getString("Name" + pos, ""), 
        			(double)settings.getFloat("CoordinateY" + pos, 0),
        			(double)settings.getFloat("CoordinateX" + pos, 0)));
        	pos++;
        }
	}
	
	/**
	 * Save spots to preferences file
	 */
	public void SaveSpots(){
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
	    SharedPreferences.Editor editor = settings.edit();
	    editor.clear();
	    
	    if (spots.size() > 3){
	    	for (int i = 3; i < spots.size(); i++){
	    		editor.putString("Name" + (i-3), spots.get(i).Name);
	    		editor.putFloat("CoordinateY" + (i-3), (float)spots.get(i).coordinates[0]);
	    		editor.putFloat("CoordinateX" + (i-3), (float)spots.get(i).coordinates[1]);
	    	}
	    	
	    }

	    // Commit the edits!
	    editor.commit();
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see com.google.android.maps.MapActivity#isRouteDisplayed()
	 */
	@Override
	public boolean isRouteDisplayed() {
		return false;
	}

	/**
	 * Open Android system location settings
	 */
	public void enableLocationSettings() {
		Intent settingsIntent = new Intent(
				Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		startActivity(settingsIntent);
	}

	/**
	 * Opens popup to change target destination
	 * 
	 * @param v
	 */
	public void changeDestination(final View v) {
		String[] miasta = new String[spots.size()+2];
		for(int i = 0; i < spots.size(); i++){   
			miasta[i] = spots.get(i).Name; 
		}
		miasta[spots.size()] = "WprowadŸ adres...";
		miasta[spots.size()+1] = "Dane z SMSa";
		
		AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
		builder.setTitle("Wybierz cel:").setItems(miasta,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if (which < spots.size()){
							Name = spots.get(which).Name;
							cel = spots.get(which).coordinates;
							textViewCel.setText("Cel: " + spots.get(which).Name + "\n"
									+ String.format(Locale.US ,"%.4f", spots.get(which).coordinates[0]) + ", " + String.format(Locale.US ,"%.4f", spots.get(which).coordinates[1]));
						
							showArrow = 1;
							imageViewArrow.setVisibility(View.VISIBLE);
							imageViewArrow.setAlpha(0.9f);
	
							setRotation();
							onDraw();
							calcDistance();

							setMap();
						
						}
						else if (which == spots.size()){
							final Dialog dialog_new = new Dialog(v.getContext());
							dialog_new.setContentView(R.layout.dialog_new);
							dialog_new.setTitle("WprowadŸ adres:");
							dialog_new.setCancelable(true);

							Button buttonGo = (Button) dialog_new
									.findViewById(R.id.buttonGo);
							buttonGo.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View v) {
									EditText etNazwa = (EditText) dialog_new.findViewById(R.id.EditTextNazwa);
									EditText etMiasto = (EditText) dialog_new.findViewById(R.id.editTextMiasto);
									EditText etUlica = (EditText) dialog_new.findViewById(R.id.editTextUlica);
									EditText etNumer = (EditText) dialog_new.findViewById(R.id.editTextNumer);

									if (etMiasto.getText().toString().length() > 0) {
										String addressInput = etMiasto
												.getText().toString()
												+ ", "
												+ etUlica.getText().toString()
												+ " "
												+ etNumer.getText().toString();
										List<Address> addresses = null;
										try {
											addresses = new Geocoder(
													MainActivity.this)
													.getFromLocationName(
															addressInput, 1);
										} catch (IOException e) {
											e.printStackTrace();
										}
										if (etNazwa.length() > 0)
											Name = etNazwa.toString();
										else
											Name = addressInput;
										cel[0] = addresses.get(0).getLatitude();
										cel[1] = addresses.get(0)
												.getLongitude();
										textViewCel.setText("Cel: "
												+ addressInput
												+ "\n"
												+ String.format(Locale.US,
														"%.4f", cel[0])
												+ ", "
												+ String.format(Locale.US,
														"%.4f", cel[1]));
										showArrow = 1;
										imageViewArrow
												.setVisibility(View.VISIBLE);
										imageViewArrow.setAlpha(0.9f);
										setRotation();
										onDraw();
										calcDistance();
										setMap();
										dialog_new.cancel();
									}
								}
							});
							
							Button buttonSave = (Button) dialog_new
									.findViewById(R.id.buttonSave);
							buttonSave.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View v) {
									EditText etNazwa = (EditText) dialog_new.findViewById(R.id.EditTextNazwa);
									EditText etMiasto = (EditText) dialog_new.findViewById(R.id.editTextMiasto);
									EditText etUlica = (EditText) dialog_new.findViewById(R.id.editTextUlica);
									EditText etNumer = (EditText) dialog_new.findViewById(R.id.editTextNumer);

									if (etMiasto.getText().toString().length() > 0) {
										String addressInput = etMiasto
												.getText().toString()
												+ ", "
												+ etUlica.getText().toString()
												+ " "
												+ etNumer.getText().toString();
										List<Address> addresses = null;
										try {
											addresses = new Geocoder(
													MainActivity.this)
													.getFromLocationName(
															addressInput, 1);
										} catch (IOException e) {
											e.printStackTrace();
										}
										if (etNazwa.length() > 0)
											Name = etNazwa.toString();
										else
											Name = addressInput;
										cel[0] = addresses.get(0).getLatitude();
										cel[1] = addresses.get(0)
												.getLongitude();
										textViewCel.setText("Cel: "
												+ addressInput
												+ "\n"
												+ String.format(Locale.US,
														"%.4f", cel[0])
												+ ", "
												+ String.format(Locale.US,
														"%.4f", cel[1]));
										spots.add(new Spot(Name, cel[0], cel[1]));
										SaveSpots();
										showArrow = 1;
										imageViewArrow
												.setVisibility(View.VISIBLE);
										imageViewArrow.setAlpha(0.9f);
										setRotation();
										onDraw();
										calcDistance();
										setMap();
										dialog_new.cancel();
									}
								}
							});
							
							dialog_new.show();
						}
						else {
							// Get Names of all people numbers in phone
							String key, value;
							String columns[] = new String[] { People.NAME, People.NUMBER };
							Uri mContacts = People.CONTENT_URI;
							Cursor cur = managedQuery(mContacts, columns, null, null, null);
							Hashtable<String,String> ActualSender = new Hashtable<String,String>();
							if (cur.moveToFirst()) {
							        do {
							                value = cur.getString(cur.getColumnIndex(People.NAME));
							                key = cur.getString(cur.getColumnIndex(People.NUMBER));
							                        if(key!=null)
							                                ActualSender.put(key, value);
							        } while (cur.moveToNext());
							}
							
							// Get messages
							Cursor c = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);
							List<String> messages = new ArrayList<String>();
							if(c.moveToFirst()){
						        for(int i=0;i<c.getCount();i++){
						        	if (c.getString(c.getColumnIndexOrThrow("body")).toString().contains("Projekt GPS")){
						        		messages.add(ActualSender.get(c.getString(c.getColumnIndexOrThrow("address")).toString().substring(c.getString(c.getColumnIndexOrThrow("address")).toString().length()-9)) + 
						        				" (" + c.getString(c.getColumnIndexOrThrow("address")).toString() + ")" + "\n" +
						        				c.getString(c.getColumnIndexOrThrow("body")).toString().substring(c.getString(c.getColumnIndexOrThrow("body")).toString().indexOf("]")+1));
						        	}
						            c.moveToNext();
						        }
							}
							c.close();
							
							final String[] message = new String[messages.size()];
							messages.toArray(message);
							AlertDialog.Builder builder2 = new AlertDialog.Builder(v.getContext());
							builder2.setTitle("Otrzymane wspó³rzêdne:");
							builder2.setItems(message, new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									String temp = message[which].substring(message[which].indexOf(": [") + 3, message[which].length()-1);
									
									Name = message[which].substring(0, message[which].indexOf(")")+1);
									cel[0] = Double.valueOf(temp.substring(0, temp.indexOf(",")));
									cel[1] = Double.valueOf(temp.substring(temp.indexOf(",")+1));
									textViewCel.setText("Cel: " + Name
											+ "\n" + String.format(Locale.US ,"%.4f", cel[0]) + ", " + String.format(Locale.US ,"%.4f", cel[1]));
									
									spots.add(new Spot(Name, cel[0], cel[1]));
									SaveSpots();

									showArrow = 1;
									imageViewArrow.setVisibility(View.VISIBLE);
									imageViewArrow.setAlpha(0.9f);

									setRotation();
									onDraw();
									calcDistance();

									setMap();
									
									Toast.makeText(v.getContext(), "Pomyœlnie wczytano [" + Y + ", " + X + "]", 1).show();
								}
							});
							
							AlertDialog dialog2 = builder2.create();
							dialog2.show();
						}

					}
				});

		AlertDialog dialog = builder.create();
		dialog.show();
	}

	/**
	 * Toggle map to visible or no
	 * 
	 * @param v
	 */
	public void toggleMap(View v) {
		if (showMap == 1) {
			showMap = 0;

			mMyLocationOverlay.disableMyLocation();

			mapView.setVisibility(View.INVISIBLE);
			buttonMinus.setVisibility(View.INVISIBLE);
			buttonPlus.setVisibility(View.INVISIBLE);
			buttonToggleMap.setText("Wy³.");

		} else {
			showSatellite = 0;
			showMap = 1;

			mMyLocationOverlay.enableMyLocation();
			mMapController = mapView.getController();

			mapView.setVisibility(View.VISIBLE);
			buttonMinus.setVisibility(View.VISIBLE);
			buttonPlus.setVisibility(View.VISIBLE);
			mapView.setSatellite(false);
			buttonToggleMap.setText("W³.");
			buttonToggleSatellite.setText("Wy³.");

			setMap();
		}
	}

	/**
	 * Toggle satellite map to visible or no
	 * 
	 * @param v
	 */
	public void toggleSatellite(View v) {
		if (showSatellite == 1) {
			showSatellite = 0;

			mMyLocationOverlay.disableMyLocation();

			mapView.setVisibility(View.INVISIBLE);
			buttonMinus.setVisibility(View.INVISIBLE);
			buttonPlus.setVisibility(View.INVISIBLE);
			buttonToggleSatellite.setText("Wy³.");

		} else {
			showMap = 0;
			showSatellite = 1;

			mMyLocationOverlay.enableMyLocation();
			mMapController = mapView.getController();

			mapView.setVisibility(View.VISIBLE);
			buttonMinus.setVisibility(View.VISIBLE);
			buttonPlus.setVisibility(View.VISIBLE);
			mapView.setSatellite(true);
			buttonToggleSatellite.setText("W³.");
			buttonToggleMap.setText("Wy³.");

			setMap();
		}
	}

	/**
	 * Set map settings (center, zoom etc.)
	 */
	public void setMap() {
		if (showMap == 1) {
			GeoPoint centerPoint = new GeoPoint((int) (1e6 * Y),
					(int) (1e6 * X));
			mMapController.setCenter(centerPoint);
			mMapController.zoomToSpan((int) (1e6 * (cel[0] - Y) * 2 ),
					(int) (1e6 * (cel[1] - X) * 4 ));
		}
	}

	/**
	 * Toggle compass to visible or no
	 * 
	 * @param v
	 */
	public void toggleCompass(View v) {
		if (showCompass == 1) {
			showCompass = 0;
			imageViewCompass.setVisibility(View.INVISIBLE);
			buttonToggleCompass.setText("Wy³.");
		} else {
			showCompass = 1;
			imageViewCompass.setVisibility(View.VISIBLE);
			buttonToggleCompass.setText("W³.");
		}
	}

	/**
	 * Set angle between north direction and destination direction
	 */
	public void setRotation() {
		double dLat = Math.toRadians((cel[0] - Y));
		double dLon = Math.toRadians((cel[1] - X));
		double lat1 = Math.toRadians(Y);
		double lat2 = Math.toRadians(cel[0]);
		
		double y = Math.sin(dLon) * Math.cos(lat2);
		double x = Math.cos(lat1)*Math.sin(lat2) - 
				Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon); 
		degrees_target = (float) Math.toDegrees(Math.atan2(y, x));
		
		// pitagoras:
//		degrees_target = (float) -(Math.atan2(cel[0] - Y, cel[1] - X) * 180 / Math.PI) + 90;
	}

	/**
	 * Calculate distance between us and target, in meters.<br>
	 * using ‘haversine’ formula for better accuracy
	 * 
	 * --old- One degree of latitude and latitude equals 111200m
	 */
	public void calcDistance() {
		if (showArrow == 1) {
			double dLat = Math.toRadians((cel[0] - Y));
			double dLon = Math.toRadians((cel[1] - X));
			double lat1 = Math.toRadians(Y);
			double lat2 = Math.toRadians(cel[0]);
			
			double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
					Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2); 
			double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
			distance = 6371000 * c;
			textViewDystans.setText("Dystans:\n" + (int) distance + "m");
			
			// pitagoras:
//			distance = Math.sqrt(Math.pow(cel[0] - Y, 2)
//					+ Math.pow(cel[1] - X, 2)) * 111200;
		}
	}

	/**
	 * Redraw rotated arrow, compass and map
	 */
	public void onDraw() {
		Matrix matrix = new Matrix();

		if (showArrow == 1) {
			matrix.postRotate((float) degrees_target - degrees_north,
					imageViewArrow.getDrawable().getBounds().width() / 2,
					imageViewArrow.getDrawable().getBounds().height() / 2);
			imageViewArrow.setImageMatrix(matrix);
		}

		if (showCompass == 1) {
			matrix.reset();
			matrix.postRotate((float) -degrees_north, imageViewCompass
					.getDrawable().getBounds().width() / 2, imageViewCompass
					.getDrawable().getBounds().height() / 2);
			imageViewCompass.setImageMatrix(matrix);
		}

		if (showMap == 1 || showSatellite == 1)
			mapView.setRotation((float) -degrees_north);

	}

	/**
	 * Get number of paired satellites, fixed satellites. Show power of signal
	 */
	public GpsStatus.Listener GpsStatusListener = new GpsStatus.Listener() {

		public void onGpsStatusChanged(int event) {
			if (event == GpsStatus.GPS_EVENT_FIRST_FIX) {
				GpsStatus gpsStatus = mLocationManager.getGpsStatus(null);
				firstFixTime = gpsStatus.getTimeToFirstFix() / 1000;
				textViewFirstFix.setText("Z³apano fixa w: " + firstFixTime
						+ "s");

				MediaPlayer mp = MediaPlayer.create(MainActivity.this,
						R.raw.message);
				mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

					@Override
					public void onCompletion(MediaPlayer mp) {
						mp.release();
					}
				});
				mp.start();
			}

			if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
				GpsStatus gpsStatus = mLocationManager.getGpsStatus(null);

				count = 0;
				usedInFix = 0;
				textViewMoc.setText("Moc: ");

				if (gpsStatus.getSatellites() == null) {
					textViewSatelity.setText("Znalezione satelity: 0");
					return;
				}
				for (GpsSatellite satellite : gpsStatus.getSatellites()) {
					textViewMoc
							.setText(textViewMoc.getText().toString()
									+ String.format("%.2f", satellite.getSnr())
									+ " | ");
					count++;
					if (satellite.usedInFix()) {
						usedInFix++;
					}
				}
				textViewSatelity.setText("Znalezione satelity: " + count
						+ ",\nSfiksowane: " + usedInFix);

			}
		}
	};

	/**
	 * Define a listener that responds to gravity and geomagnetic sensor
	 * updates. Obtain device's angle between north
	 */
	public SensorEventListener SensorEventListener = new SensorEventListener() {

		float[] mGravity = new float[3];
		float[] mGeomagnetic = new float[3];

		@Override
		public void onSensorChanged(SensorEvent event) {
			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				for (int i = 0; i < 3; i++)
					mGravity[i] = event.values[i];
			}
			if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
				for (int i = 0; i < 3; i++)
					mGeomagnetic[i] = event.values[i];
			}

			if (mGravity != null && mGeomagnetic != null) {
				float[] R = new float[9];
				float[] I = new float[9];
				boolean success = SensorManager.getRotationMatrix(R, I,
						mGravity, mGeomagnetic);
				if (success) {
					float[] orientation = new float[3];
					SensorManager.getOrientation(R, orientation);

					float azimuth = (float) Math.toDegrees(orientation[0]);
					azimuth = (azimuth + 360) % 360;

					if (azimuth > 270 && degrees_north < 90)
						azimuth -= 360;
					if (azimuth < 90 && degrees_north > 270)
						azimuth += 360;

					degrees_north = ((degrees_north * 19 + azimuth) / 20 + 360) % 360;

					onDraw();
				}
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub

		}
	};

	/**
	 * Define a listener that responds to location updates Obtain device's
	 * latitude, longtitude
	 */
	public LocationListener LocationListener = new LocationListener() {

		@Override
		public void onLocationChanged(Location location) {

			if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {

				Message.obtain(mHandler, 1, "Satelitarnie (GPS)").sendToTarget();

			} else if (location.getProvider().equals(
					LocationManager.NETWORK_PROVIDER)) {

				if (usedInFix > 0)
					return;

				Message.obtain(mHandler, 1, "Naziemnie (NETWORK)").sendToTarget();
			}

			// A new location update is received. Do something useful with it.
			// In this case, we're sending the update to a handler which then
			// updates the UI with the new location.
			Message.obtain(
					mHandler,
					0,
					"Szerokoœæ: " + String.format(Locale.US ,"%.4f", location.getLatitude())  + "\nD³ugoœæ: "
							+ String.format(Locale.US ,"%.4f", location.getLongitude())).sendToTarget();

			X = location.getLongitude();
			Y = location.getLatitude();

			setRotation();
			onDraw();
			calcDistance();
			setMap();

		}

		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub

		}
	};
}
