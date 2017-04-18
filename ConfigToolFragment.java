/*
****************************************************************************
* Copyright(c) 2014 NXP Semiconductors                                     *
* All rights are reserved.                                                 *
*                                                                          *
* Software that is described herein is for illustrative purposes only.     *
* This software is supplied "AS IS" without any warranties of any kind,    *
* and NXP Semiconductors disclaims any and all warranties, express or      *
* implied, including all implied warranties of merchantability,            *
* fitness for a particular purpose and non-infringement of intellectual    *
* property rights.  NXP Semiconductors assumes no responsibility           *
* or liability for the use of the software, conveys no license or          *
* rights under any patent, copyright, mask work right, or any other        *
* intellectual property rights in or to any products. NXP Semiconductors   *
* reserves the right to make changes in the software without notification. *
* NXP Semiconductors also makes no representation or warranty that such    *
* application will be suitable for the specified use without further       *
* testing or modification.                                                 *
*                                                                          *
* Permission to use, copy, modify, and distribute this software and its    *
* documentation is hereby granted, under NXP Semiconductors' relevant      *
* copyrights in the software, without fee, provided that it is used in     *
* conjunction with NXP Semiconductor products(UCODE I2C, NTAG I2C).        *
* This  copyright, permission, and disclaimer notice must appear in all    *
* copies of this code.                                                     *
****************************************************************************
*/
package com.nxp.DINRailDemo.fragments;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;


import com.nxp.DINRailDemo.reader.Ntag_I2C_Demo;
import com.nxp.DINRailDemo.R;

//import android.widget.CompoundButton.OnCheckedChangeListener;

public class ConfigToolFragment extends Fragment implements RadioGroup.OnCheckedChangeListener, Spinner.OnItemSelectedListener {

	private static Spinner lang_spinner;

	private static RadioGroup Radiogroup_blue;
	private static RadioGroup Radiogroup_orange;
	private static RadioGroup Radiogroup_green;


	private static RadioButton Blue_on;
	private static RadioButton Orange_on;
	private static RadioButton Green_on;

	private LinearLayout banner;

	public static String getSetConfigstatus() {
		return setConfigstatus;
	}

	private static String setConfigstatus;


	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		//setContentView(R.layout.activity_configtool);
		setRetainInstance(true);
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		final View layout = inflater.inflate(R.layout.activity_configtool, container, false);

		lang_spinner = (Spinner) layout.findViewById(R.id.lang_Spinner);
		Radiogroup_blue = (RadioGroup) layout.findViewById(R.id.radiogroup_blue);
		Radiogroup_orange = (RadioGroup) layout.findViewById(R.id.radiogroup_orange);
		Radiogroup_green = (RadioGroup) layout.findViewById(R.id.radiogroup_green);
		Blue_on=(RadioButton) layout.findViewById(R.id.blue_on);
		Orange_on=(RadioButton) layout.findViewById(R.id.orange_on);
		Green_on=(RadioButton) layout.findViewById(R.id.green_on);
		Blue_on.setChecked(true);
		Green_on.setChecked(true);
		Orange_on.setChecked(true);
		Radiogroup_blue.setOnCheckedChangeListener(this);
		Radiogroup_orange.setOnCheckedChangeListener(this);
		Radiogroup_green.setOnCheckedChangeListener(this);
		banner= (LinearLayout) layout.findViewById(R.id.configbanner);

		banner.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {
				Toast.makeText(getActivity(),"Please tap the phone onto the module!",Toast.LENGTH_SHORT).show();
				return false;
			}
		});

		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				getActivity(), R.array.lang_Options,
				android.R.layout.simple_spinner_item);

		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		// Apply the adapter to the spinner
		lang_spinner.setAdapter(adapter);


		int index_radiogroup_blue=  Radiogroup_blue.indexOfChild(layout.findViewById(Radiogroup_blue.getCheckedRadioButtonId()));
		int index_radiogroup_orange= Radiogroup_orange.indexOfChild(layout.findViewById(Radiogroup_orange.getCheckedRadioButtonId()));
		int index_radiogroup_green= Radiogroup_green.indexOfChild(layout.findViewById(Radiogroup_green.getCheckedRadioButtonId()));


		String[] string_array = new String[4];
		string_array[0]= Integer.toString(index_radiogroup_orange);
		string_array[1]= Integer.toString(index_radiogroup_blue);
		string_array[2]= Integer.toString(index_radiogroup_green);
		string_array[3]= Integer.toString(lang_spinner.getSelectedItemPosition());
		setConfigstatus="";


		for( int i=0;i<string_array.length;i++) {
			setConfigstatus += string_array[i];
		}
		setSetConfigstatus(setConfigstatus);

		lang_spinner.setOnItemSelectedListener(this);

		return layout; // end onCreate

	}


	public static void setSetConfigstatus(String setConfigstatus) {
		ConfigToolFragment.setConfigstatus = setConfigstatus;
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {

		Itemselector();
	}


	@Override
	public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
		Itemselector();
	}

	@Override
	public void onNothingSelected(AdapterView<?> adapterView) {
		Itemselector();
	}

	private void Itemselector()
	{
		int index_radiogroup_blue=  Radiogroup_blue.indexOfChild(getView().findViewById(Radiogroup_blue.getCheckedRadioButtonId()));
		int index_radiogroup_orange= Radiogroup_orange.indexOfChild(getView().findViewById(Radiogroup_orange.getCheckedRadioButtonId()));
		int index_radiogroup_green= Radiogroup_green.indexOfChild(getView().findViewById(Radiogroup_green.getCheckedRadioButtonId()));


		String[] string_array = new String[4];

		string_array[0]= Integer.toString(index_radiogroup_orange);
		string_array[1]= Integer.toString(index_radiogroup_blue);
		string_array[2]= Integer.toString(index_radiogroup_green);
		string_array[3]= Integer.toString(lang_spinner.getSelectedItemPosition());
		setConfigstatus="";


		for( int k=0;k<string_array.length;k++) {
			setConfigstatus += string_array[k];
		}
		setSetConfigstatus(setConfigstatus);

	}

}
