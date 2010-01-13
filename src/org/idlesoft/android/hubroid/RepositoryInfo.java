package org.idlesoft.android.hubroid;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class RepositoryInfo extends TabActivity {
	public ProgressDialog m_progressDialog;
	public JSONObject m_jsonData;
	public Intent m_intent;
	public ForkListAdapter m_forkListAdapter;
	public JSONArray m_jsonForkData;
	public int m_position;

	private Runnable threadProc_userInfo = new Runnable() {
		public void run() {
			TextView tv = (TextView)findViewById(R.id.repository_owner);
			m_intent = new Intent(RepositoryInfo.this, UserInfo.class);
			m_intent.putExtra("username", tv.getText());
			RepositoryInfo.this.startActivity(m_intent);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			runOnUiThread(new Runnable() {
				public void run() {
					m_progressDialog.dismiss();
				}
			});
		}
	};

	private Runnable threadProc_forkedRepoInfo = new Runnable() {
		public void run() {
			TextView tv = (TextView)findViewById(R.id.repository_fork_of);
			m_intent = new Intent(RepositoryInfo.this, RepositoryInfo.class);
			m_intent.putExtra("username", tv.getText().toString().split("/")[0]);
			m_intent.putExtra("repo_name", tv.getText().toString().split("/")[1]);
			RepositoryInfo.this.startActivity(m_intent);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			runOnUiThread(new Runnable() {
				public void run() {
					m_progressDialog.dismiss();
				}
			});
		}
	};

	View.OnClickListener username_onClickListener = new View.OnClickListener() {
		public void onClick(View v) {
			m_progressDialog = ProgressDialog.show(RepositoryInfo.this, "Please wait...", "Loading User Information...");
			Thread thread = new Thread(null, threadProc_userInfo);
			thread.start();
		}
	};

	View.OnClickListener forkedRepo_onClickListener = new View.OnClickListener() {
		public void onClick(View v) {
			m_progressDialog = ProgressDialog.show(RepositoryInfo.this, "Please wait...", "Loading Repository Information...");
			Thread thread = new Thread(null, threadProc_forkedRepoInfo);
			thread.start();
		}
	};

	private Runnable threadProc_itemClick = new Runnable() {
		public void run() {
			try {
	        	m_intent = new Intent(RepositoryInfo.this, RepositoryInfo.class);
	        	m_intent.putExtra("repo_name", m_jsonForkData.getJSONObject(m_position).getString("name"));
	        	m_intent.putExtra("username", m_jsonForkData.getJSONObject(m_position).getString("owner"));
			} catch (JSONException e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			RepositoryInfo.this.startActivity(m_intent);

			runOnUiThread(new Runnable() {
				public void run() {
					m_progressDialog.dismiss();
				}
			});
		}
	};

	private OnItemClickListener m_onForkListItemClick = new OnItemClickListener() {
		public void onItemClick(AdapterView parent, View v, int position, long id) {
			m_position = position;
			m_progressDialog = ProgressDialog.show(RepositoryInfo.this, "Please wait...", "Loading Repository's Network...", true);
			Thread thread = new Thread(null, threadProc_itemClick);
			thread.start();
		}
	};

	@Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.repository_info);

        TabHost tabhost = getTabHost();

        tabhost.addTab(tabhost.newTabSpec("tab1").setIndicator("Repository Info").setContent(R.id.repo_info_tab));
        tabhost.addTab(tabhost.newTabSpec("tab2").setIndicator("Commit Log").setContent(R.id.repo_commits_tab));
        tabhost.addTab(tabhost.newTabSpec("tab3").setIndicator("Network").setContent(R.id.repo_forks_tab));

        tabhost.setCurrentTab(0);        

        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
			try {
				URL repo_query = new URL("http://github.com/api/v2/json/repos/show/"
						+ URLEncoder.encode(extras.getString("username"))
						+ "/"
						+ URLEncoder.encode(extras.getString("repo_name")));
				m_jsonData = Hubroid.make_api_request(repo_query).getJSONObject("repository");
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}

			try {
				TextView repo_name = (TextView)findViewById(R.id.repository_name);
				repo_name.setText(m_jsonData.getString("name"));
				TextView repo_desc = (TextView)findViewById(R.id.repository_description);
				repo_desc.setText(m_jsonData.getString("description"));
				TextView repo_owner = (TextView)findViewById(R.id.repository_owner);
				repo_owner.setText(m_jsonData.getString("owner"));
				TextView repo_fork_count = (TextView)findViewById(R.id.repository_fork_count);
				repo_fork_count.setText(m_jsonData.getString("forks"));
				TextView repo_watcher_count = (TextView)findViewById(R.id.repository_watcher_count);
				repo_watcher_count.setText(m_jsonData.getString("watchers"));

				m_jsonForkData = Hubroid.make_api_request(new URL("http://github.com/api/v2/json/repos/show/"
						+ URLEncoder.encode(extras.getString("username"))
						+ "/"
						+ URLEncoder.encode(extras.getString("repo_name"))
						+ "/network")).getJSONArray("network");

				if (m_jsonData.getBoolean("fork") == true) {
					// Find out what this is a fork of...
					String forked_user = m_jsonForkData.getJSONObject(0).getString("owner");
					String forked_repo = m_jsonForkData.getJSONObject(0).getString("name");
					// Show "Fork of:" label, it's value, and the button
					TextView repo_fork_of_label = (TextView)findViewById(R.id.repository_fork_of_label);
					repo_fork_of_label.setVisibility(0);
					TextView repo_fork_of = (TextView)findViewById(R.id.repository_fork_of);
					repo_fork_of.setText(forked_user + "/" + forked_repo);
					repo_fork_of.setVisibility(0);
					Button goto_forked_repository_btn = (Button)findViewById(R.id.goto_forked_repository_btn);
					goto_forked_repository_btn.setVisibility(0);
					// Set the onClick listener for the button
					goto_forked_repository_btn.setOnClickListener(forkedRepo_onClickListener);
				}

				m_forkListAdapter = new ForkListAdapter(RepositoryInfo.this, m_jsonForkData);
				ListView repo_list = (ListView)findViewById(R.id.repo_forks_list);
				repo_list.setAdapter(m_forkListAdapter);
				repo_list.setOnItemClickListener(m_onForkListItemClick);
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}

			Button user_info_btn = (Button)findViewById(R.id.goto_repo_owner_info_btn);
			user_info_btn.setOnClickListener(username_onClickListener);
        }
    }
}
