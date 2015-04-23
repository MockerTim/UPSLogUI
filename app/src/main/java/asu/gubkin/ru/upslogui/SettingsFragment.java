package asu.gubkin.ru.upslogui;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileInfo;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

/**
 * Created by timur on 08.04.15.
 */
public class SettingsFragment extends Fragment {
    private static final String appKey = "mndotd21cnge6ko";//"p78hpiyefxd4p9z";
    private static final String appSecret = "jsfwbopus38tlhv";//"hwjzoa1qoxjoqft";

    private static final int REQUEST_LINK_TO_DBX = 0;

    private TextView mTestOutput;
    private Button mLinkButton;
    private DbxAccountManager mDbxAcctMgr;

    private TreeMap<Date, Integer> dateValueMap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_main, menu);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dropbox_login, container, false);

        mTestOutput = (TextView) rootView.findViewById(R.id.test_output);
        mLinkButton = (Button) rootView.findViewById(R.id.link_button);
        mLinkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickLinkToDropbox();
            }
        });

        mDbxAcctMgr = DbxAccountManager.getInstance(this.getActivity().getApplicationContext(), appKey, appSecret);

        return rootView;
    }

    public void onResume() {
        super.onResume();
        if (mDbxAcctMgr.hasLinkedAccount()) {
            showLinkedView();
            doDropboxTest();
        } else {
            showUnlinkedView();
        }
    }

    private void showLinkedView() {
        mLinkButton.setVisibility(View.GONE);
        mTestOutput.setVisibility(View.VISIBLE);
    }

    private void showUnlinkedView() {
        mLinkButton.setVisibility(View.VISIBLE);
        mTestOutput.setVisibility(View.GONE);
    }

    private void onClickLinkToDropbox() {
        mDbxAcctMgr.startLink(this, REQUEST_LINK_TO_DBX);
    }

    private void doDropboxTest() {
        mTestOutput.setText("Dropbox Sync API Version " + DbxAccountManager.SDK_VERSION_NAME + "\n");
        try {
            final String TEST_DATA = "Hello Dropbox";
            final String TEST_FILE_NAME = "hello_dropbox.txt";
            DbxPath testPath = new DbxPath(DbxPath.ROOT, TEST_FILE_NAME);

            // Create DbxFileSystem for synchronized file access.
            DbxFileSystem dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());

            // Print the contents of the root folder.  This will block until we can
            // sync metadata the first time.
            List<DbxFileInfo> infos = dbxFs.listFolder(DbxPath.ROOT);
            mTestOutput.append("\nContents of app folder:\n");
            for (DbxFileInfo info : infos) {
                mTestOutput.append("    " + info.path + ", " + info.modifiedTime + '\n');
            }

            // Create a test file only if it doesn't already exist.
            if (!dbxFs.exists(testPath)) {
                DbxFile testFile = dbxFs.create(testPath);
                try {
                    testFile.writeString(TEST_DATA);
                } finally {
                    testFile.close();
                }
                mTestOutput.append("\nCreated new file '" + testPath + "'.\n");
            }

            // Read and print the contents of test file.  Since we're not making
            // any attempt to wait for the latest version, this may print an
            // older cached version.  Use getSyncStatus() and/or a listener to
            // check for a new version.
            if (dbxFs.isFile(testPath)) {
                String resultData;
                DbxFile testFile = dbxFs.open(testPath);
                try {
                    resultData = testFile.readString();
                } finally {
                    testFile.close();
                }
                mTestOutput.append("\nRead file '" + testPath + "' and got data:\n    " + resultData);
            } else if (dbxFs.isFolder(testPath)) {
                mTestOutput.append("'" + testPath.toString() + "' is a folder.\n");
            }

            DbxFile upsLogFile = dbxFs.open(new DbxPath("logups22"));
            try {
//                upsLogFile.writeString("Hello Dropbox!");
                InputStream in = upsLogFile.getReadStream();

                BufferedReader fIn = new BufferedReader(new InputStreamReader(in));

                String lineStr;// = fIn.readLine();

//                String upsLogJson = "{points: {";
                dateValueMap = new TreeMap<>();

                String pairStr;
                while((lineStr = fIn.readLine()) != null) {
                    pairStr = lineStr.substring(1, lineStr.lastIndexOf("]"));
                    String[] pairArray = pairStr.split(", ");

                    String someDateStr = pairArray[0].substring(1, pairArray[0].lastIndexOf("\""));
//                    mTestOutput.setText("someDateStr: {" + someDateStr + "}");
                    Integer someInt = Integer.parseInt(pairArray[1]);
                    DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z");
                    Date someDate = (Date)formatter.parse(someDateStr);

                    dateValueMap.put(someDate, someInt);
                }

                mTestOutput.setText("activity: " + getActivity());

//                Intent intent = this.getActivity().getIntent();
//
//                intent.putExtra("ARRAY", dateValueMap);
//
//                startActivity(intent);
            } finally {
                upsLogFile.close();
            }
        } catch (ParseException e) {
            mTestOutput.setText("Dropbox test failed: " + e);
        } catch (IOException e) {
            mTestOutput.setText("Dropbox test failed: " + e);
        }
    }
}
