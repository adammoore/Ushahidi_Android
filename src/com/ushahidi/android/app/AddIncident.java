/** 
 ** Copyright (c) 2010 Ushahidi Inc
 ** All rights reserved
 ** Contact: team@ushahidi.com
 ** Website: http://www.ushahidi.com
 ** 
 ** GNU Lesser General Public License Usage
 ** This file may be used under the terms of the GNU Lesser
 ** General Public License version 3 as published by the Free Software
 ** Foundation and appearing in the file LICENSE.LGPL included in the
 ** packaging of this file. Please review the following information to
 ** ensure the GNU Lesser General Public License version 3 requirements
 ** will be met: http://www.gnu.org/licenses/lgpl.html.	
 **	
 **
 ** If you have questions regarding the use of this file, please contact
 ** Ushahidi developers at team@ushahidi.com.
 ** 
 **/

/**
 * This class is based on the code here
 * http://code.google.com/p/android-phonefinder/
 */
package com.ushahidi.android.app;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import com.ushahidi.android.app.R;

import com.ushahidi.android.app.data.AddIncidentData;
import com.ushahidi.android.app.data.UshahidiDatabase;
import com.ushahidi.android.app.net.UshahidiHttpClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

public class AddIncident extends Activity {
	private static final int HOME = Menu.FIRST+1;
	private static final int LIST_INCIDENT = Menu.FIRST+2;
	private static final int INCIDENT_MAP = Menu.FIRST+3;
	private static final int INCIDENT_REFRESH= Menu.FIRST+4;
	private static final int SETTINGS = Menu.FIRST+5;
	private static final int ABOUT = Menu.FIRST+6;
	private static final int GOTOHOME = 0;
	private static final int MAP_INCIDENTS = 1;
	private static final int LIST_INCIDENTS = 2;
	private static final int REQUEST_CODE_SETTINGS = 2;
	private static final int REQUEST_CODE_ABOUT = 3;
	private static final int REQUEST_CODE_IMAGE = 4;
	private static final int REQUEST_CODE_CAMERA = 5;
	private static final int VIEW_MAP = 1;
	
	private Geocoder gc;
	private List<Address> foundAddresses;
	
	// date and time
    private int mYear;
    private int mMonth;
    private int mDay;
    private int mHour;
    private int mMinute;
    private int counter = 0;
    private static double longitude;
    private static double latitude;
    private String errorMessage = "";
    private String dateToSubmit = "";
	private boolean error = false;
	private EditText incidentTitle;
	private EditText incidentLocation;
	private EditText incidentDesc;
	private TextView incidentDate;
	private TextView selectedPhoto;
	private TextView selectedCategories;
	private Button btnSend;
	private Button btnCancel;
	private Button btnAddCategory;
	private Button pickTime;
	private Button pickDate;
	private Button btnPicture;
	private Button addLocation;
	private HashMap<Integer,Integer> timeDigits;
	private Bundle bundle;
	private Bundle extras;
	private static final int DIALOG_ERROR_NETWORK = 0;
	private static final int DIALOG_ERROR_SAVING = 1;
    private static final int DIALOG_LOADING_CATEGORIES= 2;
    private static final int DIALOG_LOADING_LOCATIONS = 3;
	private static final int DIALOG_CHOOSE_IMAGE_METHOD = 4;
	private static final int DIALOG_MULTIPLE_CATEGORY = 6;
	private static final int TIME_DIALOG_ID = 7;
    private static final int DATE_DIALOG_ID = 8;
	private String filename = "";
	private final static Handler mHandler = new Handler();
	private Vector<String> vectorCategories = new Vector<String>();
	private Vector<String> categoriesId = new Vector<String>();
	private HashMap<String, String> categoriesTitle = new HashMap<String, String>();
	private HashMap<String,String> params = new HashMap<String, String>();
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.add_incident);
        foundAddresses = new ArrayList<Address>();
		gc = new Geocoder(this);
        
        initComponents();
        
        //load settings
        UshahidiService.loadSettings(AddIncident.this);
         
    }
	
	//menu stuff
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {
		populateMenu(menu);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		populateMenu(menu);

		return(super.onCreateOptionsMenu(menu));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		return(applyMenuChoice(item) ||
				super.onOptionsItemSelected(item));
	}

	public boolean onContextItemSelected(MenuItem item) {

		return(applyMenuChoice(item) ||
				super.onContextItemSelected(item));
	}
	
	private void populateMenu(Menu menu) {
		MenuItem i;
		
		i = menu.add( Menu.NONE, HOME, Menu.NONE, R.string.menu_home );
		i.setIcon(R.drawable.ushahidi_home);
		
		i = menu.add( Menu.NONE, LIST_INCIDENT, Menu.NONE, R.string.incident_list );
		i.setIcon(R.drawable.ushahidi_list);
		  
		i = menu.add( Menu.NONE, INCIDENT_MAP, Menu.NONE, R.string.incident_menu_map );
		i.setIcon(R.drawable.ushahidi_map);
		  
		
		i = menu.add( Menu.NONE, INCIDENT_REFRESH, Menu.NONE, R.string.incident_menu_refresh );
		i.setIcon(R.drawable.ushahidi_refresh);
		  
		i = menu.add( Menu.NONE, SETTINGS, Menu.NONE, R.string.menu_settings );
		i.setIcon(R.drawable.ushahidi_settings);
		  
		i = menu.add( Menu.NONE, ABOUT, Menu.NONE, R.string.menu_about );
		i.setIcon(R.drawable.ushahidi_about);
		
		
	}
	
	private boolean applyMenuChoice(MenuItem item) {
		Intent launchPreferencesIntent;
		switch (item.getItemId()) {
		
		case LIST_INCIDENT:
			launchPreferencesIntent = new Intent( AddIncident.this,ListIncidents.class);
    		startActivityForResult( launchPreferencesIntent, LIST_INCIDENTS );
    		setResult(RESULT_OK);
			return true;
	
		case INCIDENT_MAP:
			launchPreferencesIntent = new Intent( AddIncident.this, ViewIncidents.class);
    		startActivityForResult( launchPreferencesIntent,MAP_INCIDENTS );
			return true;
	
		case HOME:
			launchPreferencesIntent = new Intent( AddIncident.this,Ushahidi.class);
    		startActivityForResult( launchPreferencesIntent, GOTOHOME );
    		setResult(RESULT_OK);
			return true;
		
		case ABOUT:
			launchPreferencesIntent = new Intent( AddIncident.this,About.class);
    		startActivityForResult( launchPreferencesIntent, REQUEST_CODE_ABOUT );
    		setResult(RESULT_OK);
			return true;
			
		case SETTINGS:	
			launchPreferencesIntent = new Intent().setClass(AddIncident.this, Settings.class);
			
			// Make it a subactivity so we know when it returns
			startActivityForResult(launchPreferencesIntent, REQUEST_CODE_SETTINGS);
			return true;
		
		}
		return false;
	}
	
	/**
	 * Initialize UI components
	 */
	private void initComponents(){
		btnPicture = (Button) findViewById(R.id.btnPicture);
		btnAddCategory = (Button) findViewById(R.id.add_category);
		selectedPhoto = (TextView) findViewById(R.id.lbl_photo);
		selectedCategories = (TextView) findViewById(R.id.lbl_category);
		
		incidentTitle = (EditText) findViewById(R.id.incident_title);
		
		incidentTitle.setOnFocusChangeListener(new View.OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if( !hasFocus ) {
					if(TextUtils.isEmpty(incidentTitle.getText())) {
						incidentTitle.setError(getString(R.string.empty_report_title));
					}
				}
				
			}
			
		});
		
		incidentLocation = (EditText) findViewById(R.id.incident_location);
		incidentLocation.setOnFocusChangeListener( new View.OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if( !hasFocus ) {
					if( TextUtils.isEmpty(incidentLocation.getText())) {
						incidentLocation.setError(getString(R.string.empty_report_location));
					}
				}
			}
		});
		
		incidentDesc = (EditText) findViewById(R.id.incident_desc);
		incidentDesc.setOnFocusChangeListener(new View.OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if( !hasFocus ) {
					if(TextUtils.isEmpty(incidentDesc.getText())) {
						incidentDesc.setError(getString(R.string.empty_report_description));
					}
				}
			}
			
		});
		
		btnSend = (Button) findViewById(R.id.incident_add_btn);
		btnCancel = (Button) findViewById(R.id.incident_add_cancel);
		incidentDate = (TextView) findViewById(R.id.lbl_date);
		pickDate = (Button) findViewById(R.id.pick_date);
		pickTime = (Button) findViewById(R.id.pick_time);
		addLocation = (Button) findViewById(R.id.location);
		
		//open location map window
		addLocation.setOnClickListener( new View.OnClickListener(){
			public void onClick( View v ) {
				
				Intent intent = new Intent( AddIncident.this,LocationMap.class);
				startActivityForResult(intent,VIEW_MAP);
				setResult( RESULT_OK, intent );
				
			}
		});
		
		btnSend.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v){
				//Dipo Fix
				error = false;
				if( TextUtils.isEmpty(incidentTitle.getText())) {
					errorMessage = getString(R.string.empty_report_title);
					error = true;
				}
				
				if( TextUtils.isEmpty(incidentDesc.getText())) {
					errorMessage += getString(R.string.empty_report_description);
					error = true;
				}
				
				if( TextUtils.isEmpty(incidentLocation.getText())) {
					errorMessage += getString(R.string.empty_report_location);
					error = true;
				}
				
				//Dipo Fix
				if(vectorCategories.size() == 0) {
					errorMessage += getString(R.string.empty_report_categories);
					error = true;
				}
				
				if( !error ) {
					if( Util.isConnected(AddIncident.this) ){ 
						if( !postToOnline() ) {
							mHandler.post(mSentIncidentFail);
						}else { 
							mHandler.post(mSentIncidentSuccess);
							clearFields();
							
							//after a successful upload, delete the file
							File f = new File(UshahidiService.savePath + UshahidiService.fileName);
							if(f.exists()){
								f.delete();
							}
						}
					}else {
					final Thread tr = new Thread() {
						@Override
						public void run() {
							try {
								mHandler.post(mSentIncidentOffline);
								
							} finally {
							}
						}
					};
					tr.start();
					}
				
				}else{
					final Toast t = Toast.makeText(AddIncident.this,
							"Error!\n\n"+ errorMessage,
							Toast.LENGTH_LONG);
					t.show();
					errorMessage = "";
				}
			 
				
				}
			});
		
		btnPicture.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(DIALOG_CHOOSE_IMAGE_METHOD);
			}
		});
		
		btnCancel.setOnClickListener( new View.OnClickListener() {
			public void onClick(View v) {
				clearFields();
				Intent intent = new Intent( AddIncident.this,Ushahidi.class);
        		startActivityForResult( intent, GOTOHOME );
        		setResult(RESULT_OK);
        		finish();
			}
		});
		
		btnAddCategory.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(DIALOG_MULTIPLE_CATEGORY);
				counter++;
			}
		});
		
		pickDate.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                showDialog(DATE_DIALOG_ID);
            }
        });
		
        pickTime.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                showDialog(TIME_DIALOG_ID);
            }
        });
        
        final Calendar c = Calendar.getInstance();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);
        mHour = c.get(Calendar.HOUR_OF_DAY);
        mMinute = c.get(Calendar.MINUTE);
        c.get(Calendar.AM_PM);
        updateDisplay();
        
	}
	
	//fetch categories
	public String[] showCategories() {
		  Cursor cursor = UshahidiApplication.mDb.fetchAllCategories();
		  
		  String categories[] = new String[cursor.getCount()];
	
		  int i = 0;
		  if (cursor.moveToFirst()) {
			  
			  int titleIndex = cursor.getColumnIndexOrThrow(UshahidiDatabase.CATEGORY_TITLE);
			  
			  int idIndex = cursor.getColumnIndexOrThrow(UshahidiDatabase.CATEGORY_ID);
			  
			  do {
				  categories[i] = cursor.getString(titleIndex);
				  categoriesTitle.put(String.valueOf(cursor.getInt(idIndex)), 
						  cursor.getString(titleIndex));
				  categoriesId.add(String.valueOf(cursor.getInt(idIndex)));
				  i++;
			  }while( cursor.moveToNext() );
		  }
		 
		  cursor.close();
		  return categories;
		  
	}
	
	//reset records in the field
	private void clearFields() {
		btnPicture = (Button) findViewById(R.id.btnPicture);
		btnAddCategory = (Button) findViewById(R.id.add_category);
		incidentTitle.setText("");
		incidentLocation.setText("");
		incidentDesc.setText("");
		vectorCategories.clear();
		selectedPhoto.setText("");
		selectedCategories.setText("");
		counter = 0;
		updateDisplay();
		
		//clear persistent data
		 SharedPreferences.Editor editor = getPreferences(0).edit();
	     editor.putString("title", "");
	     editor.putString("desc", "");
	     editor.putString("date", "");
	     editor.commit();
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		// The preferences returned if the request code is what we had given
		// earlier in startSubActivity
		switch(requestCode){
			case REQUEST_CODE_CAMERA:
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);	//pull it out of landscape mode
				break;
	
			case REQUEST_CODE_IMAGE:
				if(resultCode != RESULT_OK){
					return;
				}
				Uri uri = data.getData();
				Bitmap b = null;
				try {
					b = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
				} catch (FileNotFoundException e) {
					break;
				} catch (IOException e) {
					break;
				}
				ByteArrayOutputStream byteArrayos = new ByteArrayOutputStream();
				try {
					b.compress(CompressFormat.JPEG, 75, byteArrayos);				
					byteArrayos.flush();
				} catch (OutOfMemoryError e){
					break;
				} catch (IOException e) {
					break;
				}
				filename = "android_pic_upload" + randomString() + ".jpg";
				ImageManager.writeImage(byteArrayos.toByteArray(), filename);
				UshahidiService.fileName = filename;
				selectedPhoto.setText(UshahidiService.fileName);
				break;
				
			case VIEW_MAP:
				if(resultCode != RESULT_OK){
					return;
				}
				
				bundle = null;
				extras = data.getExtras();
				if( extras != null ) bundle = extras.getBundle("locations");
				
				if( bundle != null && !bundle.isEmpty() ) {
					incidentLocation.setText( bundle.getString("location"));
				
					AddIncident.latitude = bundle.getDouble("latitude");
					AddIncident.longitude = bundle.getDouble("longitude");
				}
				break;
		}
	}
	
	private static Random random = new Random();

	protected static String randomString() {
		return Long.toString(random.nextLong(), 10);
	}
	
	//
	final Runnable mSentIncidentOffline = new Runnable() {
		public void run() {
			if( addToDb() == -1 ) {
				mHandler.post(mSentIncidentFail);
			}else { 
				mHandler.post(mSentIncidentOfflineSuccess);
				//clearFields();
			}
		}
	};
	
	final Runnable mSentIncidentFail = new Runnable() {
		public void run() {
			Util.showToast(AddIncident.this, R.string.failed_to_add_report_online);
		}
	};
	
	final Runnable mSentIncidentOfflineFail = new Runnable() {
		public void run() {
			Util.showToast(AddIncident.this, R.string.failed_to_add_report_offline);
		}
	};
	
	final Runnable mSentIncidentOfflineSuccess = new Runnable() {
		public void run() {
			Util.showToast(AddIncident.this, R.string.report_successfully_added_offline);
	
		}
	};
	
	//
	final Runnable mSendIncidentOnline = new Runnable() {
		public void run() {
			if( !postToOnline() ) {
				mHandler.post(mSentIncidentFail);
			}else { 
				mHandler.post(mSentIncidentSuccess);
				
			}
		}
	};
	
	//
	final Runnable mSentIncidentSuccess = new Runnable() {
		public void run() {
			Util.showToast(AddIncident.this, R.string.report_successfully_added_online);
			
		}
	};

	/**
	 * Create various dialog
	 */
	@Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_ERROR_NETWORK: {
                AlertDialog dialog = (new AlertDialog.Builder(this)).create();
                dialog.setTitle("Network error!");
                dialog.setMessage("Network error, please ensure you are connected to the internet");
                dialog.setButton2("Ok", new Dialog.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();						
					}
        		});
                dialog.setCancelable(false);
                return dialog;
            }
            case DIALOG_ERROR_SAVING:{
           	 	AlertDialog dialog = (new AlertDialog.Builder(this)).create();
                dialog.setTitle("File System error!");
                dialog.setMessage("File System error, please ensure your save path is correct!");
                dialog.setButton2("Ok", new Dialog.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();						
					}
        		});
                dialog.setCancelable(false);
                return dialog;
           }
            case DIALOG_LOADING_CATEGORIES: {
                ProgressDialog dialog = new ProgressDialog(this);
                dialog.setTitle("Loading Categories");
                dialog.setMessage("Please wait while categories are loaded...");
                dialog.setIndeterminate(true);
                dialog.setCancelable(false);
                return dialog;
            }
            
            case DIALOG_LOADING_LOCATIONS: {
                ProgressDialog dialog = new ProgressDialog(this);
                dialog.setTitle("Loading Categories");
                dialog.setMessage("Please wait while categories are loaded...");
                dialog.setIndeterminate(true);
                dialog.setCancelable(false);
                return dialog;
            }
            case DIALOG_CHOOSE_IMAGE_METHOD:{
            	AlertDialog dialog = (new AlertDialog.Builder(this)).create();
                dialog.setTitle("Choose Method");
                dialog.setMessage("Please choose how you would like to get the picture.");
                dialog.setButton("Gallery", new Dialog.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent();
						intent.setAction(Intent.ACTION_PICK);
						intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
						startActivityForResult(intent, REQUEST_CODE_IMAGE);
						dialog.dismiss();
					}
                });
                dialog.setButton2("Cancel", new Dialog.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
                });
                
                /**
                 * Disabling camera functionality for now. will be re implemented in the next release.
                 * dialog.setButton3("Camera", new Dialog.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
												
						Intent launchPreferencesIntent = new Intent().setClass(AddIncident.this, ImageCapture.class);
						// Make it a subactivity so we know when it returns
						startActivityForResult(launchPreferencesIntent, REQUEST_CODE_CAMERA);
						dialog.dismiss();
					}
        		});*/
                
                dialog.setCancelable(false);
                return dialog;
            	
            }
            
            case DIALOG_MULTIPLE_CATEGORY: {
            	return new AlertDialog.Builder(this)
                .setTitle(R.string.add_categories)
                .setMultiChoiceItems(showCategories(),
                        null,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton,
                                    boolean isChecked) {
                            	
                            	if( isChecked ) {
                            		 
                            		vectorCategories.add(categoriesId.get( whichButton ));
                            		if( !vectorCategories.isEmpty()){
                            			selectedCategories.setText(Util.limitString(
                            					categoriesTitle.get(vectorCategories.get(0)), 15));
                            		}
                            		error = false;
                            	} else {
                            		//fixed a crash here.
                            		vectorCategories.remove(categoriesId.get( whichButton ));
                            		
                            		if( vectorCategories.isEmpty()){
                            			selectedCategories.setText("");
                            		} else {
                            			selectedCategories.setText(Util.limitString(
                            					categoriesTitle.get(vectorCategories.get(0)), 15));
                            		}
                            	}
                            	
                                /* User clicked on a check box do some stuff */
                            }
                        })
                .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	
                        /* User clicked Yes so do some stuff */
                    }
                })
               .create();
            }
            
            case TIME_DIALOG_ID:
                return new TimePickerDialog(this,
                        mTimeSetListener, mHour, mMinute, false);
                
            case DATE_DIALOG_ID:
                return new DatePickerDialog(this,
                            mDateSetListener,
                            mYear, mMonth, mDay);
        }
        return null;
    }
	

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
            case TIME_DIALOG_ID:
                ((TimePickerDialog) dialog).updateTime(mHour, mMinute);
                break;
            case DATE_DIALOG_ID:
                ((DatePickerDialog) dialog).updateDate(mYear, mMonth, mDay);
                break;
        }
    }    

    private void updateDisplay() {
    	String amPm;
    	timeDigits = new HashMap<Integer,Integer>();
        
        timeDigits.put(00, 12);
        timeDigits.put(13, 1);
        timeDigits.put(14, 2);
        timeDigits.put(15, 3);
        timeDigits.put(16, 4);
        timeDigits.put(17, 5);
        timeDigits.put(18, 6);
        timeDigits.put(19, 7);
        timeDigits.put(20, 8);
        timeDigits.put(21, 9);
        timeDigits.put(22, 10);
        timeDigits.put(23, 11);
        timeDigits.put(24, 12);
        timeDigits.put(12, 12);
        timeDigits.put(1, 1);
        timeDigits.put(2, 2);
        timeDigits.put(3, 3);
        timeDigits.put(4, 4);
        timeDigits.put(5, 5);
        timeDigits.put(6, 6);
        timeDigits.put(7, 7);
        timeDigits.put(8, 8);
        timeDigits.put(9, 9);
        timeDigits.put(10, 10);
        timeDigits.put(11, 11);
        timeDigits.put(12, 12);
    	if( mHour >=12 )
    		amPm = "PM";
    	else
    		amPm = "AM";
    	
    	String strDate = new StringBuilder()
        
    	// Month is 0 based so add 1
    	.append(mYear).append("-")
    	.append(pad(mMonth + 1)).append("-")
        .append(pad(mDay)).toString();
    	
    	String dateTime = Util.formatDate("yyyy-MM-dd",strDate,"MMMM dd, yyyy");
    	
    	incidentDate.setText( dateTime + " at "+pad(timeDigits.get(mHour))+":"+pad(mMinute) +" "+amPm);
    	
    	dateToSubmit =  new StringBuilder()
        // Month is 0 based so add 1
        .append(pad(mMonth + 1)).append("/")
        .append(pad(mDay)).append("/")
        .append(mYear).append(" ")
        .append(pad(timeDigits.get(mHour))).append(":")
        .append(pad(mMinute)).append(" ")
		.append(amPm).toString();
    }

    private DatePickerDialog.OnDateSetListener mDateSetListener =
            new DatePickerDialog.OnDateSetListener() {

                public void onDateSet(DatePicker view, int year, int monthOfYear,
                        int dayOfMonth) {
                    mYear = year;
                    mMonth = monthOfYear;
                    mDay = dayOfMonth;
                    updateDisplay();
                }
            };

    private TimePickerDialog.OnTimeSetListener mTimeSetListener =
            new TimePickerDialog.OnTimeSetListener() {

                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    mHour = hourOfDay;
                    mMinute = minute;
                    
                    updateDisplay();
                }
            };

    private static String pad(int c) {
        if (c >= 10)
            return String.valueOf(c);
        else
            return "0" + String.valueOf(c);
    }
    
    /**
     * Insert incident data into db when app is offline.
     * @author henryaddo
     *
     */
    public long addToDb() {
    	String dates[] = dateToSubmit.split(" ");
    	String time[] = dates[1].split(":");
    	
    	List<AddIncidentData> addIncidentsData = new ArrayList<AddIncidentData>();
    	AddIncidentData addIncidentData = new AddIncidentData();
    	addIncidentsData.add(addIncidentData);
    	
    	addIncidentData.setIncidentTitle(incidentTitle.getText().toString());
    	addIncidentData.setIncidentDesc(incidentDesc.getText().toString());
    	addIncidentData.setIncidentDate(dates[0]);
    	addIncidentData.setIncidentHour(Integer.parseInt(time[0]));
    	addIncidentData.setIncidentMinute(Integer.parseInt(time[1]));
    	addIncidentData.setIncidentAmPm(dates[2]);
    	addIncidentData.setIncidentCategories(Util.implode(vectorCategories));
    	addIncidentData.setIncidentLocName(incidentLocation.getText().toString());
    	addIncidentData.setIncidentLocLatitude(String.valueOf(latitude));
    	addIncidentData.setIncidentLocLongitude(String.valueOf(longitude));
    	addIncidentData.setIncidentPhoto(UshahidiService.fileName);
    	addIncidentData.setPersonFirst(UshahidiService.firstname);
    	addIncidentData.setPersonLast(UshahidiService.lastname);
    	addIncidentData.setPersonEmail(UshahidiService.email);
    	
    	//add it to database.
    	return UshahidiApplication.mDb.addIncidents(addIncidentsData);
    }
    
    /**
     * Post directly to online.
     * @author henryaddo
     *
     */
    public boolean postToOnline() {
    	
    	//String dates[] = incidentDate.getText().toString().split(" ");
    	String dates[] = dateToSubmit.split(" ");
    	String time[] = dates[1].split(":");
    	String categories = Util.implode(vectorCategories);
    	
    	StringBuilder urlBuilder = new StringBuilder(UshahidiService.domain);
    	urlBuilder.append("/api");
    	params.put("task","report");
		params.put("incident_title", incidentTitle.getText().toString());
		params.put("incident_description", incidentDesc.getText().toString()); 
		params.put("incident_date", dates[0]); 
		params.put("incident_hour", time[0]); 
		params.put("incident_minute", time[1]);
		params.put("incident_ampm", dates[2].toLowerCase());
		params.put("incident_category", categories);
		params.put("latitude", String.valueOf(latitude));
		params.put("longitude", String.valueOf(longitude)); 
		params.put("location_name", incidentLocation.getText().toString());
		params.put("person_first", UshahidiService.firstname);
		params.put("person_last", UshahidiService.lastname);
		params.put("person_email", UshahidiService.email);
		params.put("filename", UshahidiService.fileName);
		
		try {
			return UshahidiHttpClient.PostFileUpload(urlBuilder.toString(), params);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
    }
	
	 /**
     * Upon being resumed we can retrieve the current state.  This allows us
     * to update the state if it was changed at any time while paused.
     */
    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getPreferences(0); 
        String title = prefs.getString("title", null);
        String desc = prefs.getString("desc", null);
        //String date = prefs.getString("date", null);
    	
        if ( title  != null) 
            incidentTitle.setText(title, TextView.BufferType.EDITABLE);
           
        if( desc != null )
            incidentDesc.setText(desc, TextView.BufferType.EDITABLE);
        
        //if( date != null )
          //  incidentDate.setText(date, TextView.BufferType.EDITABLE);
           
    }

    /**
     * Any time we are paused we need to save away the current state, so it
     * will be restored correctly when we are resumed.
     */
    @Override
    protected void onPause() {
    	super.onPause();

        SharedPreferences.Editor editor = getPreferences(0).edit();
        editor.putString("title", incidentTitle.getText().toString());
        editor.putString("desc", incidentDesc.getText().toString());
        //editor.putString("date", incidentDate.getText().toString());
        editor.commit();
    }
    
    /**
     * Geocode user entered location name.
     * 
     * @param String - the location to be geocoded
     * 
     * @return int - 0 on success, 1 network failure, 2 couldn't geocode wrong location name
     */
    public int geocodeLocationName( String locationName ) {
    	if(Util.isConnected(AddIncident.this)) {
    		try {
				foundAddresses = gc.getFromLocationName(locationName,5);
				Address address = foundAddresses.get(0);
				AddIncident.latitude = address.getLatitude();
				AddIncident.longitude = address.getLongitude();
				
			} catch (IOException e) {
				return 2;
			}
    	} 
    	return 1;
    }
    
}
