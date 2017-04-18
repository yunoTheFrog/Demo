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
package com.nxp.DINRailDemo.reader;

import java.io.IOException;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutionException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.nfc.FormatException;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import com.nxp.DINRailDemo.activities.MainActivity;
import com.nxp.DINRailDemo.fragments.DiagToolFragment;
import com.nxp.DINRailDemo.fragments.ConfigToolFragment;
import com.nxp.DINRailDemo.activities.VersionInfoActivity;
import com.nxp.DINRailDemo.exceptions.CommandNotSupportedException;
import com.nxp.DINRailDemo.exceptions.NotPlusTagException;
import com.nxp.DINRailDemo.listeners.WriteSRAMListener;
import com.nxp.DINRailDemo.reader.I2C_Enabled_Commands.NC_Reg_Func;
import com.nxp.DINRailDemo.reader.I2C_Enabled_Commands.NS_Reg_Func;
import com.nxp.DINRailDemo.reader.I2C_Enabled_Commands.R_W_Methods;
import com.nxp.DINRailDemo.reader.I2C_Enabled_Commands.SR_Offset;
import com.nxp.DINRailDemo.reader.Ntag_Get_Version.Prod;
import com.nxp.DINRailDemo.reader.Ntag_I2C_Commands.Register;
import com.nxp.DINRailDemo.R;


import static com.nxp.DINRailDemo.fragments.DiagToolFragment.setDiagStatus;

/**
 * Class for the different Demos.
 *
 * @author NXP67729
 *
 */


public class Ntag_I2C_Demo implements WriteSRAMListener {

	private I2C_Enabled_Commands reader;
	private Activity main;
	private Tag tag;

	/**
	 *
	 * Taskreferences.
	 *
	 */
    private LINConfigTask LINcTask;
	private LINDiagTask LINdTask;
	private LINResetTask LINrTask;

	private Byte[][] result;

	public void setEeprom_flag_0(int eeprom_flag_0) {
		this.eeprom_flag_0 = eeprom_flag_0;
	}

	int eeprom_flag_0;

	int[] led= new int[3];

	public int getLed(int nbr) {
		return led[nbr];
	}


	public void setLed(int nbr ,int val) {
		this.led[nbr] = val;
	}

	int green_led=0;

	/**
	 *
	 * DEFINES.
	 *
	 */
	private static final int LAST_FOUR_BYTES 	= 4;
	private static final int DELAY_TIME 		= 500;
	private static final int TRAILS 			= 300;
	private static final int DATA_SEND_BYTE 	= 12;
	private static final int VERSION_BYTE 		= 63;
	private static final int GET_VERSION_NR 	= 12;
	private static final int GET_FW_NR 			= 28;
	private static final int THREE_BYTES 		= 3;
	private static final int PAGE_SIZE 			= 4096;


	/**
	 * Constructor.
	 *
	 * @param tag
	 *            Tag with which the Demos should be performed
	 * @param main
	 *            MainActivity
	 */
	public Ntag_I2C_Demo(Tag tag, final Activity main) {
		try {
			if (tag == null) {
				this.main = null;
				this.tag = null;
				return;
			}
			this.main = main;
			this.tag = tag;

			reader = I2C_Enabled_Commands.get(tag);

			if (reader == null) {
				String message = "The Tag could not be identified or this NFC device does not ";
				String title = "Communication failed";
				showAlert(message, title);
			} else {
				reader.connect();
			}

			Ntag_Get_Version.Prod prod = reader.getProduct();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}


	private void showAlert(final String message, final String title) {
		main.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				new AlertDialog.Builder(main)
						.setMessage(message)
						.setTitle(title)
						.setPositiveButton("OK",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {

									}
								}).show();
			}
		});

	}

	/**
	 * Checks if the tag is still connected based on the previously detected reader.
	 *
	 * @return Boolean indicating tag connection
	 *
	 */
	public boolean isConnected() {
		return reader.isConnected();
	}

	/**
	 * Checks if the tag is still connected based on the tag.
	 *
	 * @return Boolean indicating tag presence
	 *
	 */
	public static boolean isTagPresent(Tag tag) {
		final Ndef ndef = Ndef.get(tag);
		if (ndef != null && !ndef.getType().equals("android.ndef.unknown")) {
			try {
				ndef.connect();
				final boolean isConnected = ndef.isConnected();
				ndef.close();
				return isConnected;
			} catch (final IOException e) {
				e.printStackTrace();
				return false;
			}
		} else {
			final NfcA nfca = NfcA.get(tag);
			if (nfca != null) {
				try {
					nfca.connect();
					final boolean isConnected = nfca.isConnected();
					nfca.close();

					return isConnected;
				} catch (final IOException e) {
					e.printStackTrace();
					return false;
				}
			} else {
				final NfcB nfcb = NfcB.get(tag);
				if (nfcb != null) {
					try {
						nfcb.connect();
						final boolean isConnected = nfcb.isConnected();
						nfcb.close();
						return isConnected;
					} catch (final IOException e) {
						e.printStackTrace();
						return false;
					}
				} else {
					final NfcF nfcf = NfcF.get(tag);
					if (nfcf != null) {
						try {
							nfcf.connect();
							final boolean isConnected = nfcf.isConnected();
							nfcf.close();
							return isConnected;
						} catch (final IOException e) {
							e.printStackTrace();
							return false;
						}
					} else {
						final NfcV nfcv = NfcV.get(tag);
						if (nfcv != null) {
							try {
								nfcv.connect();
								final boolean isConnected = nfcv.isConnected();
								nfcv.close();
								return isConnected;
							} catch (final IOException e) {
								e.printStackTrace();
								return false;
							}
						} else {
							return false;
						}
					}
				}
			}
		}
	}

	/**
	 *
	 * Finish all tasks.
	 *
	 */

	public void finishAllTasks() {
		Configfinish();
		diagfinish();
		resetfinish();
	}



	public void Configfinish() {
		if (LINcTask != null && !LINcTask.isCancelled()) {
			LINcTask.exit = true;
			try {
				LINcTask.get();
			} catch (Exception e) {
				e.printStackTrace();
			}
			LINcTask = null;
		}
	}

	public void diagfinish() {
		if (LINdTask != null && !LINdTask.isCancelled()) {
			LINdTask.exit = true;
			try {
				LINdTask.get();
			} catch (Exception e) {
				e.printStackTrace();
			}
			LINdTask = null;
		}
	}
	public void resetfinish() {
		if (LINrTask != null && !LINrTask.isCancelled()) {
			LINrTask.exit = true;
			try {
				LINrTask.get();
			} catch (Exception e) {
				e.printStackTrace();
			}
			LINrTask = null;
		}
	}


	/**
	 * Checks if the demo is ready to be executed.
	 *
	 * @return Boolean indicating demo readiness
	 *
	 */
	public boolean isReady() {
		if (tag != null && reader != null) {
			return true;
		}
		return false;
	}

	/**
	 *
	 * Set the current board version.
	 *
	 */
	public void setBoardVersion() throws IOException, FormatException,
			CommandNotSupportedException {

		byte[] dataTx = new byte[reader.getSRAMSize()];
		byte[] dataRx = new byte[reader.getSRAMSize()];

		dataTx[reader.getSRAMSize() - LAST_FOUR_BYTES] = 'V';

		if (!((reader.getSessionRegister(SR_Offset.NC_REG) & NC_Reg_Func.PTHRU_ON_OFF
				.getValue()) == NC_Reg_Func.PTHRU_ON_OFF.getValue())) {
			VersionInfoActivity.setBoardVersion("No Board attached");
			VersionInfoActivity.setBoardFWVersion("No Board attached");
			return;
		}

		try {
			reader.waitforI2Cread(DELAY_TIME);
		} catch (TimeoutException e1) {
			e1.printStackTrace();

			VersionInfoActivity.setBoardVersion("No Board attached");
			VersionInfoActivity.setBoardFWVersion("No Board attached");
			return;
		}

		reader.writeSRAMBlock(dataTx, null);

		for (int i = 0; i < TRAILS; i++) {
			if (((reader.getSessionRegister(SR_Offset.NS_REG) & NS_Reg_Func.SRAM_RF_READY
					.getValue()) == NS_Reg_Func.SRAM_RF_READY.getValue())) {
				break;
			}
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		if (!((reader.getSessionRegister(SR_Offset.NS_REG) & NS_Reg_Func.SRAM_RF_READY
				.getValue()) == NS_Reg_Func.SRAM_RF_READY.getValue())) {
			VersionInfoActivity.setBoardVersion("1.0");
			VersionInfoActivity.setBoardFWVersion("1.0");
			return;
		}

		dataRx = reader.readSRAMBlock();

		String boardVersion;
		String boardFWVersion;
		// Check if Data was send, else it is a ExplorerBoard FW
		if (dataRx[DATA_SEND_BYTE] == 0) {
			String version = Integer.toHexString((dataRx[VERSION_BYTE] >> LAST_FOUR_BYTES)
					& (byte) 0x0F)
					+ "." + Integer.toHexString(dataRx[VERSION_BYTE] & (byte) 0x0F);
			boardVersion = version;
			boardFWVersion = version;
		} else {
			boardVersion = new String(dataRx, GET_VERSION_NR, THREE_BYTES);
			boardFWVersion = new String(dataRx, GET_FW_NR, THREE_BYTES);
		}

		VersionInfoActivity.setBoardVersion(boardVersion);
		VersionInfoActivity.setBoardFWVersion(boardFWVersion);
	}



	private void showDemoNotSupportedAlert() {
		String message = main.getString(R.string.demo_not_supported);
		String title = main.getString(R.string.demo_not_supported_title);
		showAlert(message, title);
	}


    public void 	LINConfig() throws CommandNotSupportedException {

        LINcTask = new LINConfigTask();
        LINcTask.execute();


    }

	public void 	LINDiag() throws CommandNotSupportedException {


		LINdTask= new LINDiagTask();



		if(LINdTask.getStatus()==AsyncTask.Status.RUNNING)
			green_led=1;
		else
			LINdTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
	}



    private class LINConfigTask extends AsyncTask<Void, Byte[], Void> {

        private Boolean exit = false;

        @Override
        protected Void doInBackground(Void... params) {
			byte[] dataTx_eeprom= new byte[4];
            byte[] config_register;
			byte	NS_register;
            byte[] config_Status;

			boolean lockflag=true;
			try {
				if(reader.isConnected()) {
					do {
						try {
							config_register = reader.getSessionRegisters();
							NS_register = config_register[SR_Offset.NS_REG.getValue()];
							if ((NS_register & NS_Reg_Func.EEPROM_WR_BUSY.getValue()) == NS_Reg_Func.EEPROM_WR_BUSY
									.getValue()) {
								lockflag = true;
							} else {
								lockflag = false;
							}
						} catch (IOException e) {
							e.printStackTrace();
							cancel(true);
						} catch (FormatException e) {
							e.printStackTrace();
							cancel(true);
						} catch (CommandNotSupportedException e) {
							e.printStackTrace();
							cancel(true);
						}
					} while (lockflag && (!reader.isConnected()));

					config_Status = ConfigToolFragment.getSetConfigstatus().getBytes();

					dataTx_eeprom[0] = config_Status[0];
					dataTx_eeprom[1] = config_Status[1];
					dataTx_eeprom[2] = config_Status[2];
					dataTx_eeprom[3] = config_Status[3];

					// wait to prevent that a RF communication is
					// at the same time as µC I2C
					Thread.sleep(10);
					reader.waitforI2Cread(DELAY_TIME);
					reader.writeEEPROM(0x34, dataTx_eeprom);

				}

            } catch (FormatException e) {
                cancel(true);
                e.printStackTrace();
            } catch (IOException e) {
                cancel(true);
                e.printStackTrace();
            } catch (Exception e) {
                cancel(true);
                e.printStackTrace();
            }
            return null;
        }


		@Override
		protected void onPostExecute(Void res) {

			Toast.makeText(main, "Configuration written. Please remove the phone!",Toast.LENGTH_LONG).show();
			Vibrator v = (Vibrator)  main.getSystemService(Context.VIBRATOR_SERVICE);
			// Vibrate for 500 milliseconds
			v.vibrate(1000);
		}

    }

	private class LINDiagTask extends AsyncTask<Void, Byte, Void> {

		private Boolean exit = false;

		@Override
		protected Void doInBackground(Void... params) {
			byte[] dataRx_eeprom = new byte[8];
			byte[] config_register;
			byte[] config_Status;


			boolean lockflag=true;
			byte NS_register;

			try {
				config_Status = DiagToolFragment.getDiagStatus().getBytes();
				if (config_Status[0] == 'Z') {
					publishProgress(config_Status[0]);
				} else
					setDiagStatus("X");
				if(reader.isConnected()) {
					do {
						try {
							config_register = reader.getSessionRegisters();
							NS_register = config_register[SR_Offset.NS_REG.getValue()];
							if ((NS_register & NS_Reg_Func.I2C_LOCKED.getValue()) == NS_Reg_Func.I2C_LOCKED
									.getValue()) {
								lockflag = true;
							} else {
								lockflag = false;
							}
						} catch (IOException e) {
							cancel(true);
							e.printStackTrace();
						} catch (FormatException e) {
							cancel(true);
							e.printStackTrace();
						} catch (CommandNotSupportedException e) {
							cancel(true);
							e.printStackTrace();
						}
						reader.waitforI2Cwrite(500);
						if(!reader.isConnected()){

							break;
						}

					} while (lockflag);

					if(!lockflag) {
						dataRx_eeprom = reader.readEEPROM(0x35, 0x36);

						setLed(0, (dataRx_eeprom[0] << 8) | (dataRx_eeprom[1] & 0xFF));
						setLed(1, (dataRx_eeprom[2] << 8) | (dataRx_eeprom[3] & 0xFF));
						setLed(2, (dataRx_eeprom[4] << 8) | (dataRx_eeprom[5] & 0xFF));
					}
				}
			} catch (FormatException e) {
				cancel(true);
				e.printStackTrace();
			} catch (IOException e) {
				cancel(true);
				e.printStackTrace();
			} catch (CommandNotSupportedException e) {
				showDemoNotSupportedAlert();
				cancel(true);
				e.printStackTrace();
			} catch (Exception e) {
				cancel(true);
				e.printStackTrace();
			}
			return null;

		}


		@Override
		protected void onProgressUpdate(Byte[] bytes) {

			setDiagStatus("X");
			cancel(true);
			 new LINResetTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);

			return;
		}

		@Override
		protected void onCancelled(Void res){

			if(!reader.isConnected())
			{
				try {
					reader.connect();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if(reader.isConnected())
			{
				new LINDiagTask().executeOnExecutor(SERIAL_EXECUTOR);
			}
			return;

		}

		@Override
		protected void onPostExecute(Void aVoid) {

			 String orange_string =getLed(0)+"";
			 String blue_string =getLed(1)+"";
			 String green_string =getLed(2)+"";

			DiagToolFragment.dyn_orange_txt.setText(orange_string);
			 DiagToolFragment.dyn_blue_txt.setText(blue_string);
			DiagToolFragment.dyn_green_txt.setText(green_string);

			if(reader.isConnected())
				new LINDiagTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
		}
	}


	private class LINResetTask extends AsyncTask<Void, Integer, Void> {

		private Boolean exit = false;

		@Override
		protected Void doInBackground(Void... params) {

			byte[] dataTx_eeprom= new byte[8];
			byte[] config_register;
			byte NS_register;
			boolean lockflag=true;

				do {
					try {
						config_register=reader.getSessionRegisters();
						NS_register=config_register[SR_Offset.NS_REG.getValue()];
						if ((NS_register & NS_Reg_Func.I2C_LOCKED.getValue()) == NS_Reg_Func.I2C_LOCKED
								.getValue()) {
							lockflag = true;
						} else {
							lockflag = false;
						}
					} catch (IOException e) {
						e.printStackTrace();
					} catch (FormatException e) {
						e.printStackTrace();
					} catch (CommandNotSupportedException e) {
						e.printStackTrace();
					}

				}while(lockflag);

				try {

				dataTx_eeprom[0]= 1;
				dataTx_eeprom[1]= 2;
				dataTx_eeprom[2]= 3;
				dataTx_eeprom[3]= 4;
				dataTx_eeprom[4]= 5;
				dataTx_eeprom[5]= 6;
				dataTx_eeprom[6]= 'Z';
				Thread.sleep(10);
				reader.writeEEPROM( 0x35, dataTx_eeprom);
				Thread.sleep(10);
				reader.waitforI2Cwrite(100);

			} catch (FormatException e) {
				cancel(true);
				e.printStackTrace();
			} catch (IOException e) {
				cancel(true);
				e.printStackTrace();
			} catch (Exception e) {
				cancel(true);
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void res) {

			Toast.makeText(main, "Counter Reset",Toast.LENGTH_LONG).show();
			Vibrator v = (Vibrator)  main.getSystemService(Context.VIBRATOR_SERVICE);
			v.vibrate(1000);
			setDiagStatus("X");
			new LINDiagTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
		}


	}


	/**
	 * Performs the flashing of a new bin file from the NFC Device.
	 *
	 * @param bytesToFlash
	 *           Byte Array containing the new firmware to be flashed
	 * @return Boolean operation result
	 * @throws IOException
	 * @throws FormatException
	 */
	public Boolean Flash(byte[] bytesToFlash) {
		int sectorSize = PAGE_SIZE;
		
		byte[] data = null;
		byte[] flashData = null;
		int address_start =0x4000;

		try {
			int length = bytesToFlash.length;
			int flashes = length / sectorSize + (length % sectorSize == 0 ? 0 : 1);
			int blocks = (int) Math.ceil(length	/ (float) reader.getSRAMSize());

			// Set the number of writings
			MainActivity.setFLashDialogMax(blocks);
			
			for (int i = 0; i < flashes; i++) {
				int flash_addr = address_start + i * sectorSize;
				int flash_length = 0;
				
				if (length - (i + 1) * sectorSize < 0) {
					flash_length = roundUp(length % sectorSize);
					flashData = new byte[flash_length];
					Arrays.fill(flashData, (byte) 0);
					System.arraycopy(bytesToFlash, i * sectorSize, flashData, 0, length % sectorSize);
				} else {
					flash_length = sectorSize;
					flashData = new byte[flash_length];
					System.arraycopy(bytesToFlash, i * sectorSize, flashData, 0, sectorSize);
				}
				
				data = new byte[reader.getSRAMSize()];
				data[reader.getSRAMSize() - 4] = 'F';
				data[reader.getSRAMSize() - 3] = 'P';
				
				data[reader.getSRAMSize() - 8] = (byte) (flash_length >> 24 & 0xFF);
				data[reader.getSRAMSize() - 7] = (byte) (flash_length >> 16 & 0xFF);
				data[reader.getSRAMSize() - 6] = (byte) (flash_length >> 8 & 0xFF);
				data[reader.getSRAMSize() - 5] = (byte) (flash_length & 0xFF);

				data[reader.getSRAMSize() - 12] = (byte) (flash_addr >> 24 & 0xFF);
				data[reader.getSRAMSize() - 11] = (byte) (flash_addr >> 16 & 0xFF);
				data[reader.getSRAMSize() - 10] = (byte) (flash_addr >> 8 & 0xFF);
				data[reader.getSRAMSize() - 9] = (byte) (flash_addr & 0xFF);
				
				Log.d("FLASH", "Flashing to start");
				reader.writeSRAMBlock(data, null);
				Log.d("FLASH", "Start Block write " + (i + 1) + " out of " + flashes);
				
				reader.waitforI2Cread(100);
				
				Log.d("FLASH", "Starting Block writing");
				reader.writeSRAM(flashData, R_W_Methods.Fast_Mode, this);
				Log.d("FLASH", "All Blocks written");
				
				reader.waitforI2Cwrite(500);
				Thread.sleep(500);
				
				Log.d("FLASH", "Wait finished");
				byte[] response = reader.readSRAMBlock();
				Log.d("FLASH", "Block read");
				
				if (response[reader.getSRAMSize() - 4] != 'A' || response[reader.getSRAMSize() - 3] != 'C' || response[reader.getSRAMSize() - 2] != 'K') {
					Log.d("FLASH", "was nak");
					return false;
				}
				Log.d("FLASH", "was ack");
			}
			Log.d("FLASH", "Flash completed");
			
			data = new byte[reader.getSRAMSize()];
			data[reader.getSRAMSize() - 4] = 'F';
			data[reader.getSRAMSize() - 3] = 'S';
			reader.writeSRAMBlock(data, null);
			
			// Wait for the I2C to be ready
			reader.waitforI2Cread(DELAY_TIME);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (FormatException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		} catch (CommandNotSupportedException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Log.d("FLASH", "Flash returned");
		
		data = new byte[reader.getSRAMSize()];
		data[reader.getSRAMSize() - 4] = 'F';
		data[reader.getSRAMSize() - 3] = 'F';
		
		try {
			reader.writeSRAMBlock(data, null);

			// Wait for the I2C to be ready
			reader.waitforI2Cread(100);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (FormatException e) {
			e.printStackTrace();
		} catch (CommandNotSupportedException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
		return false;
	}


	
	/***
	 * Helper method to adjusts the number of bytes to be sent during the last flashing sector
	 * 
	 * @param num to round up
	 * @return number roundep up
	 */
	int roundUp(int num) {
		if(num <= 256) {
			return 256;
		} else if(num > 256 && num <= 512) {
			return 512;
		} else if(num > 512 && num <= 1024) {
			return 1024;
		} else {
			return 4096;
		}
	}


	@Override
	public void onWriteSRAM() {
		main.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				MainActivity.updateFLashDialog();
			}
		});
	}
}
