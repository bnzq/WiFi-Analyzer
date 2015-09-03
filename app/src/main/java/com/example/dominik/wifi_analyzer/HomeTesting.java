package com.example.dominik.wifi_analyzer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class HomeTesting extends Fragment
{
    public static final String LOG_TAG = HomeTesting.class.getSimpleName();

    private static final int TEXT_ID = 0;

    private WifiManager mWifiManager;
    private MyDBHandler mMyDBHandler;
    private HomeTestingAdapter mHomeTestingAdapter;
    private int mPositionSelected = -1;
    private String mNameSelected = "";
    private ViewHolder viewHolder;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mWifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
        Utility.enableWifi(mWifiManager);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.home_testing_tab, container, false);

        viewHolder = new ViewHolder(rootView);

        mMyDBHandler = new MyDBHandler(getActivity());

        mMyDBHandler.deleteAllAndRestoreDefaultRooms();

        updateView();

        return rootView;
    }

    //update View - Info Bar + Rooms
    private void updateView()
    {
        Utility.enableWifi(mWifiManager);

        updateTestingRooms();
        updateInfoBar();
    }

    //update rooms from datebase to view
    private void updateTestingRooms()
    {
        List<String[]> list = new ArrayList<String[]>();

        List<RoomInfo> roomInfoList = mMyDBHandler.getAllRoomsInfos();

        for (int i = 0; i < roomInfoList.size(); i++)
        {
            String[] wifiDetails = new String[HomeTestingAdapter.SIZE_TAB];

            wifiDetails[HomeTestingAdapter.ROOM_NAME_TAB] = roomInfoList.get(i).getRoomName();
            wifiDetails[HomeTestingAdapter.LINK_SPEED_TAB] = Float.toString(roomInfoList.get(i).getLinkSpeed());
            wifiDetails[HomeTestingAdapter.RSSI_TAB] = Float.toString(roomInfoList.get(i).getRssi());
            wifiDetails[HomeTestingAdapter.LAST_LINK_SPEED_TAB] = Float.toString(roomInfoList.get(i).getLastLinkSpeed());;
            wifiDetails[HomeTestingAdapter.LAST_RSSI_TAB] = Float.toString(roomInfoList.get(i).getLastRssi());

            list.add(wifiDetails);
        }

        mHomeTestingAdapter = new HomeTestingAdapter(getActivity(), list);
        viewHolder.mListView.setAdapter(mHomeTestingAdapter);
    }

    //update info about connected info in simple bar
    private void updateInfoBar()
    {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        viewHolder.connectedView.setText(String.format(getActivity().getResources().getString(R.string.connected_bar), wifiInfo.getSSID()));
    }

    //test wifi parameters and edit in datebase
    private void testRoom(String nameRoom)
    {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        RoomInfo roomInfo = new RoomInfo(
                wifiInfo.getRssi(),
                wifiInfo.getLinkSpeed(),
                nameRoom
        );

        mMyDBHandler.editRoomInfo(nameRoom, roomInfo);
    }

    //create dialog to add new room and add created room to datebase
    private Dialog createAddRoomDialog()
    {

        final String title = getString(R.string.ht_add_room_dialog_title);
        final String message = getString(R.string.ht_add_room_dialog_message);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title);
        builder.setMessage(message);

        final EditText input = new EditText(getActivity());
        input.setId(TEXT_ID);
        builder.setView(input);

        builder.setPositiveButton(getString(R.string.ok_text), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int whichButton)
            {
                String value = input.getText().toString();
                mMyDBHandler.addRoomInfo(new RoomInfo(0, 0, value));
                updateView();

                return;
            }
        });

        builder.setNegativeButton(getString(R.string.cancel_text), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                return;
            }
        });

        return builder.create();
    }

    //create dialog to edit room and edit room in datebase
    private Dialog createEditRoomDialog()
    {

        final String title = getString(R.string.ht_edit_room_dialog_title);
        final String message = getString(R.string.ht_edit_room_dialog_message);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title);
        builder.setMessage(message);

        final EditText input = new EditText(getActivity());
        input.setId(TEXT_ID);
        builder.setView(input);

        builder.setPositiveButton(getString(R.string.ok_text), new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int whichButton)
                {
                    String value = input.getText().toString();
                    mMyDBHandler.editOnlyRoomNameInRoomInfo(mNameSelected, value);
                    updateView();
                    restoreSelected();

                    return;
                }
            });

        builder.setNegativeButton(getString(R.string.cancel_text), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                return;
            }
        });

        return builder.create();
    }

    //restore to default selected view
    private void restoreSelected()
    {
        mNameSelected = "";
        mPositionSelected = -1;
    }

    private class MyDBHandler extends SQLiteOpenHelper
    {
        private static final int DATABASE_VERSION = 1;
        private static final String DATABASE_NAME = "hometestingDB.db";
        private static final String TABLE_ROOMS = "rooms";

        private static final String COLUMN_ID = "_id";
        private static final String COLUMN_ROOM_NAME = "roomname";
        private static final String COLUMN_LINK_SPEED = "linkspeed";
        private static final String COLUMN_RSSI = "rssi";
        private static final String COLUMN_LAST_LINK_SPEED = "lastlinkspeed";
        private static final String COLUMN_LAST_RSSI = "lastrssi";

        public MyDBHandler(Context context)
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db)
        {
            final String CREATE_TABLE_ROOMS =
                    "CREATE TABLE " + TABLE_ROOMS + " ("
                            + COLUMN_ID + " INTEGER PRIMARY KEY, "
                            + COLUMN_ROOM_NAME + " TEXT, "
                            + COLUMN_LINK_SPEED + " REAL, "
                            + COLUMN_RSSI + " REAL, "
                            + COLUMN_LAST_LINK_SPEED + " REAL, "
                            + COLUMN_LAST_RSSI + " REAL"
                            + ")";

            db.execSQL(CREATE_TABLE_ROOMS);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_ROOMS);
        }

        //insert new room to datebase
        public void addRoomInfo(RoomInfo roomInfo)
        {
            ContentValues values = new ContentValues();
            values.put(COLUMN_ROOM_NAME, roomInfo.getRoomName());
            values.put(COLUMN_LINK_SPEED, roomInfo.getLinkSpeed());
            values.put(COLUMN_RSSI, roomInfo.getRssi());
            values.put(COLUMN_LAST_LINK_SPEED, roomInfo.getLastLinkSpeed());
            values.put(COLUMN_LAST_RSSI, roomInfo.getLastRssi());

            SQLiteDatabase db = this.getWritableDatabase();
            db.insert(TABLE_ROOMS, null, values);
            close();
        }

        //get each room from datebase
        public ArrayList<RoomInfo> getAllRoomsInfos()
        {
            final String query[] = new String[]{
                    MyDBHandler.COLUMN_ID,
                    MyDBHandler.COLUMN_ROOM_NAME,
                    MyDBHandler.COLUMN_LINK_SPEED,
                    MyDBHandler.COLUMN_RSSI,
                    MyDBHandler.COLUMN_LAST_LINK_SPEED,
                    MyDBHandler.COLUMN_LAST_RSSI
            };

            SQLiteDatabase db = this.getWritableDatabase();

            ArrayList<RoomInfo> result = new ArrayList<RoomInfo>();

            Cursor cursor = db.query(
                    MyDBHandler.TABLE_ROOMS,
                    query,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            while (cursor.moveToNext())
            {
                RoomInfo roomInfoHelp = new RoomInfo();

                roomInfoHelp.setId(cursor.getInt(0));
                roomInfoHelp.setRoomName(cursor.getString(1));
                roomInfoHelp.setLinkSpeed(cursor.getFloat(2));
                roomInfoHelp.setRssi(cursor.getFloat(3));
                roomInfoHelp.setLastLinkSpeed(cursor.getFloat(4));
                roomInfoHelp.setLastRssi(cursor.getFloat(5));

                result.add(roomInfoHelp);
            }

            cursor.close();
            db.close();

            return result;
        }

        //find room from datebase
        public RoomInfo findRoomInfo(String roomName)
        {
            String query = "Select * FROM " + TABLE_ROOMS + " WHERE " + COLUMN_ROOM_NAME + " =  \"" + roomName + "\"";

            SQLiteDatabase db = this.getWritableDatabase();

            Cursor cursor = db.rawQuery(query, null);

            RoomInfo roomInfoHelp = new RoomInfo();

            if (cursor.moveToFirst()) {
                cursor.moveToFirst();
                roomInfoHelp.setId(cursor.getInt(0));
                roomInfoHelp.setRoomName(cursor.getString(1));
                roomInfoHelp.setLinkSpeed(cursor.getFloat(2));
                roomInfoHelp.setRssi(cursor.getFloat(3));
                roomInfoHelp.setLastLinkSpeed(cursor.getFloat(4));
                roomInfoHelp.setLastRssi(cursor.getFloat(5));

                cursor.close();
            } else {
                roomInfoHelp = null;
            }
            db.close();

            return  roomInfoHelp;
        }

        //edit column RoomName to new RoomName, other columns are the same
        public boolean editOnlyRoomNameInRoomInfo(String oldRoomName, String newRoomName)
        {
            boolean result = false;

            final String query = "Select * from " + TABLE_ROOMS + " where " + COLUMN_ROOM_NAME + " =  \"" + oldRoomName + "\"";

            SQLiteDatabase db = this.getWritableDatabase();

            Cursor cursor = db.rawQuery(query, null);

            RoomInfo roomInfo = new RoomInfo();

            if (cursor.moveToFirst())
            {
                roomInfo.setId(cursor.getInt(0));

                ContentValues values = new ContentValues();
                values.put(COLUMN_ROOM_NAME, newRoomName);

                db.update(TABLE_ROOMS, values, COLUMN_ID + " = " + String.valueOf(roomInfo.getId()), null);

                cursor.close();
                result = true;
            }

            db.close();

            return result;
        }

        //edit all columns where RoomName
        public boolean editRoomInfo(String roomName, RoomInfo roomInfo)
        {
            boolean result = false;

            final String query = "Select * from " + TABLE_ROOMS + " where " + COLUMN_ROOM_NAME + " =  \"" + roomName + "\"";

            RoomInfo roomInfoToLast = findRoomInfo(roomName);
            roomInfo.setLastLinkSpeed(roomInfoToLast.getLinkSpeed());
            roomInfo.setLastRssi(roomInfoToLast.getRssi());

            SQLiteDatabase db = this.getWritableDatabase();

            Cursor cursor = db.rawQuery(query, null);

            RoomInfo roomInfoHelp = new RoomInfo();

            if (cursor.moveToFirst())
            {
                roomInfoHelp.setId(cursor.getInt(0));

                ContentValues values = new ContentValues();
                values.put(COLUMN_ROOM_NAME, roomInfo.getRoomName());
                values.put(COLUMN_LINK_SPEED, roomInfo.getLinkSpeed());
                values.put(COLUMN_RSSI, roomInfo.getRssi());
                values.put(COLUMN_LAST_LINK_SPEED, roomInfo.getLastLinkSpeed());
                values.put(COLUMN_LAST_RSSI, roomInfo.getLastRssi());

                db.update(TABLE_ROOMS, values, COLUMN_ID + " = " + String.valueOf(roomInfoHelp.getId()), null);

                cursor.close();
                result = true;
            }

            db.close();

            return result;
        }

        //clear datebase and restore default rooms
        public void deleteAllAndRestoreDefaultRooms()
        {
            final int sizeDefaultRooms = 5;

            final String [] roomTab = {
                    "OFFICE",
                    "BEDROOM",
                    "LIVING ROOM",
                    "KITCHEN",
                    "BATHROOM"
            };

            SQLiteDatabase db = this.getWritableDatabase();

            db.delete(MyDBHandler.TABLE_ROOMS, "1", null);

            db.close();

            for (int i = 0; i < sizeDefaultRooms; i++ )
            {
                mMyDBHandler.addRoomInfo(new RoomInfo(0, 0, roomTab[i]));
            }
        }

        //delete from datebase where RoomName
        public boolean deleteRoomInfo(String roomName)
        {
            boolean result = false;

            final String query = "Select * from " + TABLE_ROOMS + " where " + COLUMN_ROOM_NAME + " =  \"" + roomName + "\"";

            SQLiteDatabase db = this.getWritableDatabase();

            Cursor cursor = db.rawQuery(query, null);

            RoomInfo roomInfo = new RoomInfo();

            if (cursor.moveToFirst())
            {
                roomInfo.setId(cursor.getInt(0));
                db.delete(TABLE_ROOMS, COLUMN_ID + " = ?",
                        new String[]{String.valueOf(roomInfo.getId())});
                cursor.close();
                result = true;
            }

            db.close();

            return result;
        }

    }

    private class RoomInfo
    {
        private String roomName;
        private float linkSpeed;
        private float rssi;
        private float lastRssi;
        private float lastLinkSpeed;
        private int id;

        public RoomInfo()
        {
            this.rssi = 0.0f;
            this.linkSpeed = 0.0f;
            this.roomName = "";
            this.lastLinkSpeed = 0.0f;
            this.lastRssi = 0.0f;
        }

        public RoomInfo(float rssi, float linkSpeed, String roomName)
        {
            this.rssi = rssi;
            this.linkSpeed = linkSpeed;
            this.roomName = roomName;
            this.lastLinkSpeed = 0.0f;
            this.lastRssi = 0.0f;
        }

        public RoomInfo(float rssi, float linkSpeed, String roomName,float lastRssi, float lastLinkSpeed)
        {
            this.rssi = rssi;
            this.linkSpeed = linkSpeed;
            this.roomName = roomName;
            this.lastLinkSpeed = lastLinkSpeed;
            this.lastRssi = lastRssi;
        }

        public float getLastRssi()
        {
            return lastRssi;
        }

        public void setLastRssi(float lastRssi)
        {
            this.lastRssi = lastRssi;
        }

        public float getLastLinkSpeed()
        {
            return lastLinkSpeed;
        }

        public void setLastLinkSpeed(float lastLinkSpeed)
        {
            this.lastLinkSpeed = lastLinkSpeed;
        }

        public int getId()
        {
            return id;
        }

        public void setId(int id)
        {
            this.id = id;
        }

        public float getRssi()
        {
            return rssi;
        }

        public void setRssi(float rssi)
        {
            this.rssi = rssi;
        }

        public float getLinkSpeed()
        {
            return linkSpeed;
        }

        public void setLinkSpeed(float linkSpeed)
        {
            this.linkSpeed = linkSpeed;
        }

        public String getRoomName()
        {
            return roomName;
        }

        public void setRoomName(String roomName)
        {
            this.roomName = roomName;
        }

        @Override
        public String toString()
        {
            return getRoomName() + " " + getLinkSpeed() + " " + getRssi() + " / " + getId();
        }
    }

    public class ViewHolder
    {
        public final ImageButton addButton;
        public final ImageButton delButton;
        public final ImageButton editButton;
        public final ImageButton defButton;
        public final Button testButton;

        public final TextView connectedView;

        public final ListView mListView;

        public ViewHolder(View rootView)
        {
            addButton = (ImageButton) rootView.findViewById(R.id.add_room_button);
            delButton = (ImageButton) rootView.findViewById(R.id.del_room_button);
            editButton = (ImageButton) rootView.findViewById(R.id.edit_room_button);
            defButton = (ImageButton) rootView.findViewById(R.id.def_room_button);
            testButton = (Button) rootView.findViewById(R.id.home_testing_test_button);

            connectedView = (TextView) rootView.findViewById(R.id.ht_connected_textview);

            mListView = (ListView) rootView.findViewById(R.id.home_rooms_listview);

            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                {
                    view.setSelected(true);
                    mPositionSelected = position;
                    TextView textView = (TextView) view.findViewById(R.id.room_name_textView);
                    mNameSelected = (String) textView.getText();
                }
            });

            testButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    if (mPositionSelected != -1) {
                        testRoom(mNameSelected);
                        updateView();
                    }
                }
            });

            addButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Dialog dialog = createAddRoomDialog();
                    dialog.show();
                }
            });

            editButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    if(mPositionSelected != -1)
                    {
                        Dialog dialog = createEditRoomDialog();
                        dialog.show();
                    }
                }
            });

            delButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    if(mPositionSelected != -1)
                    {
                        mMyDBHandler.deleteRoomInfo(mNameSelected);
                        updateView();
                        restoreSelected();
                    }
                }
            });

            defButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    mMyDBHandler.deleteAllAndRestoreDefaultRooms();
                    updateView();
                }
            });

        }
    }
}
