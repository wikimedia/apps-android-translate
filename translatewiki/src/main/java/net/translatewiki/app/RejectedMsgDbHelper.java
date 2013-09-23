/*
 * @(#)TranslateWikiApp       1.0 15/9/2013
 *
 *  Copyright (c) 2013 Or Sagi.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */


package net.translatewiki.app;


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 * SQL db helper to save rejected messages.
 *
 * @author      Or Sagi
 * @version     %I%, %G%
 * @since       1.0
 * @see android.database.sqlite.SQLiteOpenHelper
 */
public class RejectedMsgDbHelper extends SQLiteOpenHelper {

    /* Inner class that defines the table contents */
    public static abstract class MsgEntry implements BaseColumns {
        public static final String TABLE_NAME = "entry";
        public static final String COLUMN_NAME_ENTRY_ID = "entryid";
        //public static final String COLUMN_NAME_TITLE = "title";
    }

    /* Inner class that defines the sql strings to be used */
    public final class RejectedMessagesContract {

        // Empty constructor prevents someone from accidentally instantiating the contract class
        public RejectedMessagesContract() {}

        private static final String TEXT_TYPE = " TEXT";
        private static final String COMMA_SEP = ",";
        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + MsgEntry.TABLE_NAME + " (" +
                        MsgEntry._ID + " INTEGER PRIMARY KEY," +
                        MsgEntry.COLUMN_NAME_ENTRY_ID + TEXT_TYPE + // maybe will add some more columns later
                        " )";
        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + MsgEntry.TABLE_NAME;
    }

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "RejectedMsgs.db";

    public RejectedMsgDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(RejectedMessagesContract.SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(RejectedMessagesContract.SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion); // downgrade does the same as upgrade
    }

    private SQLiteDatabase db;

    // projection that specifies which columns will actually be used after this query.
    private final String[] projection = { MsgEntry.COLUMN_NAME_ENTRY_ID  };

    // specifies how the results sorted in the resulting Cursor
    private final String sortOrder = MsgEntry.COLUMN_NAME_ENTRY_ID + " DESC";

    /**
     * query for revision search.
     *
     * @param revision revision to look for in the table
     * @return cursor holding the query result
     */
    public Cursor queryRevision(String revision) {
        if (db == null)
            db = getReadableDatabase();
        String[] vals = {revision};
        return db.query(
                RejectedMsgDbHelper.MsgEntry.TABLE_NAME, // The table to query
                projection,                              // The columns to return
                RejectedMsgDbHelper.MsgEntry.COLUMN_NAME_ENTRY_ID + "=?",  // columns for WHERE clause
                vals,                                    // values for the WHERE clause
                null,                                    // don't group the rows
                null,                                    // don't filter by row groups
                sortOrder                            );  // The sort order
    }

    /**
     * check for revision exists in the table.
     *
     * @param revision revision to look for in the table
     * @return true iff revision exist (once or more) in the table
     */
    public boolean containsRevision (String revision) {
        Cursor cursor = queryRevision(revision);
        return (cursor.getCount() != 0);
    }
}