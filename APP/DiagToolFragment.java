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

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;



import com.nxp.DINRailDemo.R;
import com.nxp.DINRailDemo.activities.MainActivity;
import com.nxp.DINRailDemo.listeners.WriteSRAMListener;
import com.nxp.DINRailDemo.reader.Ntag_I2C_Demo;

//import android.widget.CompoundButton.OnCheckedChangeListener;

public class DiagToolFragment extends Fragment implements
		OnClickListener {


	private static Button stop_button;

	private LinearLayout banner;

	public static TextView dyn_blue_txt;
	public static TextView dyn_green_txt;
	public static TextView dyn_orange_txt;

	public static String getDiagStatus() {
		return DiagStatus;
	}

	public static void setDiagStatus(String diagStatus) {
		DiagStatus = diagStatus;
	}

	private static String DiagStatus;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	@SuppressLint("NewApi")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {


		View layout = inflater.inflate(R.layout.activity_diagnosistool,container, false);

		banner= (LinearLayout) layout.findViewById(R.id.diagbanner);

		dyn_blue_txt=(TextView)layout.findViewById(R.id.dyn_blueled_num) ;
		dyn_green_txt=(TextView)layout.findViewById(R.id.dyn_greenled_num) ;
		dyn_orange_txt=(TextView)layout.findViewById(R.id.dyn_orangeled_num) ;

		banner.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {

				Toast.makeText(getActivity(),"Please tap the phone onto the module!",Toast.LENGTH_SHORT).show();
				return false;
			}
		});

		stop_button=(Button) layout.findViewById(R.id.stop_butt);

		stop_button.setOnClickListener(this);
		setDiagStatus("X");

		return layout; // end onCreate

	}


	@Override

	public void onClick(View v) {

		switch(v.getId()){

			case R.id.stop_butt: {

				setDiagStatus("Z");

				Toast.makeText(getActivity(),"Please tap the phone onto the module!",Toast.LENGTH_SHORT).show();

				break;
			}

		}

	} // END onClick (View v)

}
