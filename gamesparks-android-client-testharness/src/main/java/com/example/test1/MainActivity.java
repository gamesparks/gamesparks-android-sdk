package com.example.test1;

import java.util.*;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;
import android.hardware.*;
import android.content.Intent;

import com.gamesparks.sdk.*;
import com.gamesparks.sdk.android.*;
import com.gamesparks.sdk.api.autogen.GSRequestBuilder.*;
import com.gamesparks.sdk.api.autogen.GSResponseBuilder.*;
import com.gamesparks.sdk.api.GSData;
import com.gamesparks.sdk.api.autogen.GSMessageHandler;
import com.gamesparks.sdk.api.autogen.GSMessageHandler.*;
import com.gamesparks.sdk.realtime.*;

public class MainActivity extends Activity
{
	private enum Servers {
		PRODUCTION,
		STAGE,
		TEST
	}

	private static int 				numCycles = 0;
	private static GameRTSession 	mGameRTSession = null;

	private boolean		mConnected = false;
	private boolean		mAuthenticated = false;
	private boolean		mLiveMode = false;
	private Context		mContext;
	private EditText 	mEditText;
	private TextView 	mTextView1;
	private TextView	mTextView2;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	
		setContentView(R.layout.activity_main);

		mEditText = (EditText)findViewById(R.id.editText1);
		mTextView1 = (TextView)findViewById(R.id.textView1);
		mTextView2 = (TextView)findViewById(R.id.textView2);

		mContext = this;

		Button button1 = (Button)findViewById(R.id.button1);
		button1.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				connect();
			}
		});

		Button button2 = (Button)findViewById(R.id.button2);
		button2.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				disconnect();
			}
		});

		Button button3 = (Button)findViewById(R.id.button3);
		button3.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mEditText.setText("");
			}
		});

		Button button4 = (Button)findViewById(R.id.button4);
		button4.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				authenticate();
			}
		});

		Button button5 = (Button)findViewById(R.id.button5);
		button5.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				sendLogEventRequest();
			}
		});

		Button button6 = (Button)findViewById(R.id.button6);
		button6.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				sendEndSessionRequest();
			}
		});

		Button button7 = (Button)findViewById(R.id.button7);
		button7.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startRTMatchSession();
			}
		});

		//GameSparksSerializer.ObjectToGSData(new MyTestClass());

		initGS();
	}

	public void refreshEditText(String line) {
		String finalText;

		finalText = line + "\n" + mEditText.getText().toString();

		mEditText.setText(finalText);
	}

	@Override
	public void onStart() {
		super.onStart();

		connect();
	}

	@Override
	public void onStop() {
		super.onStop();

		disconnect();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
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

	private void initGS() {
		GSAndroidPlatform.initialise(mContext, "exampleKey12", "exampleSecret1234567890123456789", "", mLiveMode, true);

		GSData data = GSAndroidPlatform.gs().getGSPlatform().getDeviceStats();

		GSAndroidPlatform.gs().getGSPlatform().logMessage(Arrays.asList(data.getBaseData()).toString());

		Intent openMainActivity = new Intent(mContext, MainActivity.class);
		openMainActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(openMainActivity);

		GSAndroidPlatform.gs().setOnAvailable(new GSEventConsumer<Boolean>() {
			@Override
			public void onEvent(Boolean available) {
				mConnected = available;

				if (available) {
					mTextView1.setText("Connected");

					refreshEditText("OK");
				} else {
					mTextView1.setText("Not connected");
					mTextView2.setText(" ");

					refreshEditText("ERROR!");
				}
			}
		});

		GSAndroidPlatform.gs().setOnAuthenticated(new GSEventConsumer<String>() {
			@Override
			public void onEvent(String s) {
				if (s != null && !s.equals("") && GSAndroidPlatform.gs().isAuthenticated()) {
					mAuthenticated = true;

					mTextView2.setText("Authenticated");
				} else {
					mAuthenticated = false;

					mTextView2.setText(" ");
				}
			}
		});

		GSAndroidPlatform.gs().getMessageHandler().setMatchFoundMessageListener(new GSEventConsumer<GSMessageHandler.MatchFoundMessage>() {
			@Override
			public void onEvent(MatchFoundMessage event) {
				mGameRTSession = new GameRTSession(event.getAccessToken(), event.getHost(), event.getPort());
			}
		});

		GSAndroidPlatform.gs().getMessageHandler().setMatchNotFoundMessageListener(new GSEventConsumer<GSMessageHandler.MatchNotFoundMessage>() {
			@Override
			public void onEvent(MatchNotFoundMessage event) {
				GSAndroidPlatform.gs().getGSPlatform().logMessage("MATCH NOT FOUND");

				startRTMatchSession();
			}
		});

		Thread thread = new Thread() {
			public void run() {
				while (true) {
					if (mGameRTSession != null) {
						mGameRTSession.mSession.update();

						if (mGameRTSession.ready) {
							RTData data = new RTData();

							data.setLong(1, numCycles);

							try {
								mGameRTSession.mSession.sendRTData(1, GameSparksRT.DeliveryIntent.UNRELIABLE, data, new int[]{});
							} catch (Exception e) {
								e.printStackTrace();

							}

							numCycles ++;
						}
					}

					try {
						Thread.sleep(1000 / 60);
					} catch (InterruptedException e) {
					}
				}
			}
		};

		thread.start();
	}

	private void connect()
	{
		if (!mConnected) {
			mTextView1.setText("Connecting...");

			refreshEditText("Connecting...");

			GSAndroidPlatform.gs().start();
		}
	}

	private void disconnect()
	{
		if (mConnected) {
			mConnected = false;

			GSAndroidPlatform.gs().stop();

			mTextView1.setText("Disconnected");

			if (!GSAndroidPlatform.gs().isAuthenticated()) {
				mAuthenticated = false;

				mTextView2.setText(" ");
			}

			refreshEditText("Disconnected");
		}
	}

	private void authenticate()
	{
		if (mConnected && !mAuthenticated)
		{
			mTextView1.setText("Authenticating...");

			refreshEditText("Authenticating...");

			GSAndroidPlatform.gs().getRequestBuilder().createDeviceAuthenticationRequest()
					.setDeviceId((String.valueOf((new Random()).nextInt(1000))))
					.setDeviceOS("Android")
					.send(new GSEventConsumer<AuthenticationResponse>() {
						@Override
						public void onEvent(AuthenticationResponse event) {
							if (event.getAuthToken() == null || event.getAuthToken().equals(""))
							{
								mTextView1.setText("Connected");

								refreshEditText("ERROR!");
							}
							else
							{
								mTextView1.setText("Ready");

								refreshEditText("OK");
							}
						}
					});
		}
	}

	private void sendLogEventRequest()
	{
		if (mConnected)
		{
			mTextView1.setText("Sending LogEventRequest...");

			refreshEditText("Sending LogEventRequest...");

			GSAndroidPlatform.gs().getRequestBuilder().createLogEventRequest()
					.setEventKey("002")
					.send(new GSEventConsumer<LogEventResponse>() {
						@Override
						public void onEvent(LogEventResponse logEventResponse) {
							GSData scriptData = logEventResponse.getScriptData();

							if (GSAndroidPlatform.gs().isAuthenticated()) {
								mTextView1.setText("Ready");

								sendLogEventRequest();
							}
							else
							{
								mTextView1.setText("Connected");
							}
							if (logEventResponse.hasErrors()) {
								refreshEditText("ERROR!");
							}
							else {
								refreshEditText("OK");
							}
						}
					});
		}
	}

	private void sendEndSessionRequest() {
		if (mConnected) {
			mTextView1.setText("Sending EndSessionRequest...");

			refreshEditText("Sending EndSessionRequest...");

			EndSessionRequest request = GSAndroidPlatform.gs().getRequestBuilder().createEndSessionRequest();

			request.send(new GSEventConsumer<EndSessionResponse>() {
				@Override
				public void onEvent(EndSessionResponse endSessionResponse) {
					if (endSessionResponse.hasErrors()) {
						refreshEditText("ERROR!");
					}
					else {
						refreshEditText("OK");
					}
				}
			});
		}
	}

	private void startRTMatchSession() {
		if (mConnected && mAuthenticated) {
			mTextView1.setText("Starting RT Match Session...");

			refreshEditText("Starting RT Match Session...");

			MatchmakingRequest request = GSAndroidPlatform.gs().getRequestBuilder().createMatchmakingRequest();

			request.setSkill(1);
			request.setMatchShortCode("Match_STD");
			request.send(new GSEventConsumer<MatchmakingResponse>() {
				@Override
				public void onEvent(MatchmakingResponse event) {
					if (event.hasErrors()) {
						refreshEditText("ERROR!");
					} else {
						refreshEditText("OK");
					}
				}
			});
		}
	}
}
