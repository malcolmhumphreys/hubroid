package org.idlesoft.android.hubroid;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class Hubroid extends Activity {
	public static final String PREFS_NAME = "HubroidPrefs";
	private SharedPreferences m_prefs;
	private SharedPreferences.Editor m_editor;
	private String m_username;
	private String m_token;
	public ListView m_menuList;
	public JSONObject m_userData;
	public ProgressDialog m_progressDialog;
	public boolean m_isLoggedIn;

	public static JSONObject make_api_request(URL url) {
		JSONObject json = null;
		HttpClient c = new DefaultHttpClient();
		HttpGet getReq;
		try {
			getReq = new HttpGet(url.toURI());
			HttpResponse resp = c.execute(getReq);
			if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				resp.getEntity().writeTo(os);
				json = new JSONObject(os.toString());
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return json;
	}

	public static String getGravatarID(String name) {
		String id = null;
		try {
			File root = Environment.getExternalStorageDirectory();
			if (root.canWrite()) {
				File hubroid = new File(root, "hubroid");
				if (!hubroid.exists() && !hubroid.isDirectory()) {
					hubroid.mkdir();
				}
				File gravatars = new File(hubroid, "gravatars");
				if (!gravatars.exists() && !gravatars.isDirectory()) {
					gravatars.mkdir();
				}
				File image = new File(gravatars, name + ".id");
				if (image.exists() && image.isFile()) {
					FileReader fr = new FileReader(image);
					BufferedReader in = new BufferedReader(fr);
					id = in.readLine();
					in.close();
				} else {
					URL query = new URL("http://github.com/api/v2/json/user/show/" + URLEncoder.encode(name));
					id = make_api_request(query).getJSONObject("user").getString("gravatar_id");
					FileWriter fw = new FileWriter(image);
					BufferedWriter bw = new BufferedWriter(fw);
					bw.write(id);
					bw.flush();
					bw.close();
				}
			}
		} catch (FileNotFoundException e) {
			Log.e("debug", "Error saving bitmap", e);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return id;
	}

	public static Bitmap getGravatar(String id, int size) {
		Bitmap bm = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory()
				+ "/hubroid/gravatars/"
				+ id + "_" + size + ".png");
		if (bm == null) {
			try {
				URL aURL = new URL(
				"http://www.gravatar.com/avatar.php?gravatar_id="
						+ URLEncoder.encode(id) + "&size=" + size
						+ "&d=" + URLEncoder.encode("http://github.com/eddieringle/hubroid/raw/master/res/drawable/default_gravatar.png"));
				URLConnection conn = aURL.openConnection();
				conn.connect();
				InputStream is = conn.getInputStream();
				BufferedInputStream bis = new BufferedInputStream(is);
				bm = BitmapFactory.decodeStream(bis);
				bis.close();
				is.close();
			} catch (IOException e) {
				Log.e("debug", "Error getting bitmap", e);
			}
			try {
				File root = Environment.getExternalStorageDirectory();
				if (root.canWrite()) {
					File hubroid = new File(root, "hubroid");
					if (!hubroid.exists() && !hubroid.isDirectory()) {
						hubroid.mkdir();
					}
					File gravatars = new File(hubroid, "gravatars");
					if (!gravatars.exists() && !gravatars.isDirectory()) {
						gravatars.mkdir();
					}
					File image = new File(gravatars, id + "_" + size + ".png");
					bm.compress(CompressFormat.PNG, 100, new FileOutputStream(image));
				}
			} catch (FileNotFoundException e) {
				Log.e("debug", "Error saving bitmap", e);
			}
		}

		return bm;
	}

	public static final String[] MAIN_MENU = new String[] {
		"Watched Repos",
		"Followers/Following",
		//"Activity Feeds",
		"My Repositories",
		"Search",
		"Profile"
	};

	public boolean onPrepareOptionsMenu(Menu menu) {
		if (!menu.hasVisibleItems()) {
			menu.add(0, 1, 0, "Clear Preferences");
			menu.add(0, 2, 0, "Clear Cache");
		}
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 1:
			m_editor.clear().commit();
			Intent intent = new Intent(Hubroid.this, Hubroid.class);
			startActivity(intent);
        	return true;
		case 2:
			File root = Environment.getExternalStorageDirectory();
			if (root.canWrite()) {
				File hubroid = new File(root, "hubroid");
				if (!hubroid.exists() && !hubroid.isDirectory()) {
					return true;
				} else {
					hubroid.delete();
					return true;
				}
			}
		}
		return false;
	}

	private OnItemClickListener onMenuItemSelected = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> pV, View v, int pos, long id) {
			Intent intent;
			switch(pos) {
			case 0:
				intent = new Intent(Hubroid.this, WatchedRepositories.class);
				startActivity(intent);
				break;
			case 1:
				intent = new Intent(Hubroid.this, FollowersFollowing.class);
				startActivity(intent);
				break;
			/*case 2:
				Toast.makeText(MainScreen.this, "Activity Feeds", Toast.LENGTH_SHORT).show();
				break;*/
			case 2:
				intent = new Intent(Hubroid.this, RepositoriesList.class);
				startActivity(intent);
				break;
			case 3:
				Toast.makeText(Hubroid.this, "Search", Toast.LENGTH_SHORT).show();
				break;
			case 4:
				Toast.makeText(Hubroid.this, "Profile", Toast.LENGTH_SHORT).show();
				break;
			default:
				Toast.makeText(Hubroid.this, "Umm...", Toast.LENGTH_SHORT).show();
				break;
			}
		}
	};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        m_prefs = getSharedPreferences(PREFS_NAME, 0);
    	m_editor = m_prefs.edit();
    	m_username = m_prefs.getString("login", "");
        m_token = m_prefs.getString("token", "");
        m_isLoggedIn = m_prefs.getBoolean("isLoggedIn", false);

        if (!m_isLoggedIn) {
			Intent intent = new Intent(Hubroid.this, SplashScreen.class);
			startActivity(intent);
			Hubroid.this.finish();
		} else {
	        setContentView(R.layout.main_menu);
	
	        m_progressDialog = ProgressDialog.show(Hubroid.this, "Please wait...", "Loading user data...");
	
	        m_menuList = (ListView)findViewById(R.id.lv_main_menu_list);
	        m_menuList.setAdapter(new ArrayAdapter<String>(Hubroid.this, R.layout.main_menu_item, MAIN_MENU));
	        m_menuList.setOnItemClickListener(onMenuItemSelected);
	        
	        Thread thread = new Thread(new Runnable() {
				public void run() {
					try {
						URL query = new URL("http://github.com/api/v2/json/user/show/"
											+ URLEncoder.encode(m_username)
											+ "?login="
											+ URLEncoder.encode(m_username)
											+ "&token="
											+ URLEncoder.encode(m_token));
						m_userData = Hubroid.make_api_request(query).getJSONObject("user");
	
						runOnUiThread(new Runnable() {
							public void run() {
								ImageView gravatar = (ImageView)findViewById(R.id.iv_main_gravatar);
								try {
									gravatar.setImageBitmap(Hubroid.getGravatar(m_userData.getString("gravatar_id"), 40));
									TextView username = (TextView)findViewById(R.id.tv_main_username);
									if (m_userData.getString("name").length() > 0) {
										username.setText(m_userData.getString("name"));
									} else {
										username.setText(m_username);
									}
								} catch (JSONException e) {
									e.printStackTrace();
								}
						        RelativeLayout root_layout = (RelativeLayout)findViewById(R.id.rl_main_menu_root);
						        root_layout.setVisibility(0);
								m_progressDialog.dismiss();
							}
						});
					} catch (MalformedURLException e) {
						e.printStackTrace();
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			});
	        thread.start();
		}
    }
}