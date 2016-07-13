package com.nff.sqlciphertest;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import android.app.Activity;
import android.content.ContentValues;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import com.nff.sqlciphertest.R;

public class MainActivity extends Activity {
	
	private SQLiteDatabase db;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		SQLiteDatabase.loadLibs(this);
		DBHelperUtil dbHelper = new DBHelperUtil(this, "demo.db", null, 1);
		db = dbHelper.getSqLiteDatabase();
		Button addData = (Button) findViewById(R.id.add_data);
		Button queryData = (Button) findViewById(R.id.query_data);
		addData.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ContentValues values = new ContentValues();
				values.put("name", "ningfeifei");
				values.put("pages", 123);
				db.insert("Book", null, values);
			}
		});
		queryData.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Cursor cursor = db.query("Book", null, null, null, null, null, null);
				if (cursor != null) {
					while (cursor.moveToNext()) {
						String name = cursor.getString(cursor.getColumnIndex("name"));
						int pages = cursor.getInt(cursor.getColumnIndex("pages"));
						Toast.makeText(MainActivity.this, name+"========="+pages, 1000).show();
						Log.d("TAG", "book name is " + name);
						Log.d("TAG", "book pages is " + pages);
					}
				}
				cursor.close();
			}
		});
	}

}
