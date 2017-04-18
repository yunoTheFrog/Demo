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


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.nxp.DINRailDemo.R;
import com.nxp.DINRailDemo.utils.FileChooser;
import java.io.File;

public class FlashMemoryFragment extends Fragment {
	private static TextView filePath;
	public static boolean isAppFW() {
		return isAppFW;
	}
	public static void setIsAppFW(boolean isAppFW) {
		FlashMemoryFragment.isAppFW = isAppFW;
	}
	//private static flashTask task;
	private static boolean isAppFW = true;
	public static int getIndexFW() {
		return indexFW;
	}
	public void setIndexFW(int indexFW) {
		this.indexFW = indexFW;
	}
	private static int indexFW =0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.activity_flashmemory, container, false);

		((Button) v.findViewById(R.id.selectFlashStorage)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startFileChooser();
			}
		});
		((Button) v.findViewById(R.id.selectFlashApp)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CharSequence firmwares[] = new CharSequence[] {"DIN Rail Configuration",
						"DIN Rail Configuration(fast)","DIN Rail Configuration(slow)", "LED Blinker"};
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setTitle(getResources().getString(R.string.flash_app_select));
				builder.setItems(firmwares, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						setIsAppFW(true);
						setIndexFW(which);
						switch(indexFW) {
							case 0:
								filePath.setText(getResources().getString(R.string.file_demo));
								break;
							case 1:
								filePath.setText(getResources().getString(R.string.file_demo_fast));
								break;
							case 2:
								filePath.setText(getResources().getString(R.string.file_demo_slow));
								break;
							case 3:
								filePath.setText(getResources().getString(R.string.file_default_blinker));
								break;
							default:
								break;
						}
					}
				});
				builder.show();
			}
		});
		filePath = (TextView) v.findViewById(R.id.file_path);
		return v;
	}

	private void startFileChooser() {
		FileChooser chooser = new FileChooser(getActivity());
		chooser.setExtension("bin");
		chooser.setFileListener(new FileChooser.FileSelectedListener() {
			@Override
			public void fileSelected(final File file) {
				String path = file.getAbsolutePath();
				filePath.setText(path);
				// We do not use the default binary anymore
				setIsAppFW(false);
			}
		}).showDialog();
	}
}
