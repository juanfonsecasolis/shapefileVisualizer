/* Copyright 2019 Juan Fonseca-Solis (https://github.com/juanfonsecasolis/shapefileVisualizer)
 * Copyright 2018 ESRI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file has been modified by Juan Fonseca-Solis to incorporate a menu bar, load the
 * shapefiles from the APK instead of manually copying them from console, and present a demo
 * of a shapefile containing data from Portal de datos abiertos: Cantones de Costa Rica
 * (http://daticos-geotec.opendata.arcgis.com/datasets/741bdd9fa2ca4d8fbf1c7fe945f8c916_0)
 * 
 */

package open.juanf.shapefilevisualizer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.esri.arcgisruntime.data.ShapefileFeatureTable;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.juanf.shapefilevisualizer.R;

import android.view.Menu;
import android.view.MenuItem;
import android.content.res.Resources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    private MapView mMapView;
    private static final int MENU_ABOUT = Menu.FIRST;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // create a new map to display in the map view with a streets basemap
        mMapView = findViewById(R.id.mapView);
        //ArcGISMap map = new ArcGISMap(Basemap.createStreetsVector());
        ArcGISMap map = new ArcGISMap();
        mMapView.setMap(map);

        requestReadWritePermission();
    }

    // https://stackoverflow.com/questions/8664468/copying-raw-file-into-sdcard
    private void writeIntoExternalStorage(int resourceId, String somePathOnSdCard){
        try {
            InputStream in = getResources().openRawResource(resourceId);
            FileOutputStream out = new FileOutputStream(somePathOnSdCard);
            byte[] buff = new byte[1024];
            int read = 0;
            while ((read = in.read(buff)) > 0) {
                out.write(buff, 0, read);
            }
            in.close();
            out.close();
            Log.d(TAG,String.format("Wrote '%s'",somePathOnSdCard));
        } catch (Exception e){
            Log.e(TAG,e.getMessage());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Resources resources = getApplicationContext().getResources();
        menu.add(Menu.NONE, MENU_ABOUT, Menu.NONE, resources.getString(R.string.aboutTitle));
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ABOUT:
                startAboutActivity();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // https://stackoverflow.com/questions/2115758/how-do-i-display-an-alert-dialog-on-android
    private void startAboutActivity() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (!isFinishing()){
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Credits")
                            .setMessage("(C) 2019 Juan M. Fonseca-Solis\nWebsite: https://juanfonsecasolis.github.io\n\n" +
                                    "Built using the ArcGISMap Runtime SDK and data from 'Portal de datos abiertos, Cantones de Costa Rica' (http://daticos-geotec.opendata.arcgis.com/datasets/741bdd9fa2ca4d8fbf1c7fe945f8c916_0).")
                            .setCancelable(false)
                            .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Whatever...
                                }
                            }).show();
                }
            }
        });
    }

    /**
     * Creates a ShapefileFeatureTable from a service and, on loading, creates a FeatureLayer and add it to the map.
     */
    private void featureLayerShapefile() {
        String pathSDCard = Environment.getExternalStorageDirectory() + "/Download/"
                + getString(R.string.shapefile_name);
        writeIntoExternalStorage(R.raw.cantones_de_costa_rica_shp,String.format("%s.shp",pathSDCard));
        writeIntoExternalStorage(R.raw.cantones_de_costa_rica_shx,String.format("%s.shx",pathSDCard));
        writeIntoExternalStorage(R.raw.cantones_de_costa_rica_prj,String.format("%s.prj",pathSDCard));
        writeIntoExternalStorage(R.raw.cantones_de_costa_rica_dbf,String.format("%s.dbf",pathSDCard));
        writeIntoExternalStorage(R.raw.cantones_de_costa_rica_cpg,String.format("%s.cpg",pathSDCard));

        // load the shapefile with a local path
        ShapefileFeatureTable shapefileFeatureTable = new ShapefileFeatureTable(String.format("%s.shp",pathSDCard));

        shapefileFeatureTable.loadAsync();
        shapefileFeatureTable.addDoneLoadingListener(() -> {
            if (shapefileFeatureTable.getLoadStatus() == LoadStatus.LOADED) {

                // create a feature layer to display the shapefile
                FeatureLayer shapefileFeatureLayer = new FeatureLayer(shapefileFeatureTable);

                // add the feature layer to the map
                mMapView.getMap().getOperationalLayers().add(shapefileFeatureLayer);

                // zoom the map to the extent of the shapefile
                mMapView.setViewpointAsync(new Viewpoint(shapefileFeatureLayer.getFullExtent()));
            } else {
                String error = "Shapefile feature table failed to load: " + shapefileFeatureTable.getLoadError().toString();
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                Log.e(TAG, error);
            }
        });
    }

    /**
     * Request read permission on the device.
     */
    private void requestReadWritePermission() {
        // define permission to request
        String[] reqPermission = new String[] {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE };
        int requestCode = 2; // 2
        // For API level 23+ request permission at runtime
        if (ContextCompat.checkSelfPermission(MainActivity.this, reqPermission[0]) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(MainActivity.this, reqPermission[1]) == PackageManager.PERMISSION_GRANTED) {
            featureLayerShapefile();
        } else {
            // request permission
            ActivityCompat.requestPermissions(MainActivity.this, reqPermission, requestCode);
        }
    }

    /**
     * Handle the permissions request response.
     */
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
            && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            featureLayerShapefile();
        } else {
            // report to user that permission was denied
            Toast.makeText(MainActivity.this, getResources().getString(R.string.read_permission_denied),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.dispose();
    }
}