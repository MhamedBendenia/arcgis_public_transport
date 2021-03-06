package com.pfe.musmed.etum;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Geodatabase;
import com.esri.arcgisruntime.data.GeodatabaseFeatureTable;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.tasks.networkanalysis.DirectionManeuver;
import com.esri.arcgisruntime.tasks.networkanalysis.Route;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteParameters;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteResult;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteTask;
import com.esri.arcgisruntime.tasks.networkanalysis.Stop;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static android.view.View.OnClickListener;
import static android.view.View.OnLayoutChangeListener;


public class MainActivity extends AppCompatActivity {
    private final String TAG = MainActivity.class.getSimpleName();
    String[] reqPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission
            .ACCESS_COARSE_LOCATION,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE};
    MapView mMapView;
    List<Stop> routeStops = new ArrayList<>();
    private int requestCode = 2;
    private LocationDisplay mLocationDisplay;
    private ProgressDialog mProgressDialog;
    private Point mSourcePoint;
    private Point mDestinationPoint;
    private Route mRoute;
    private SimpleLineSymbol mRouteSymbol;
    private GraphicsOverlay mGraphicsOverlay;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;



    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);

        setContentView(R.layout.directions_drawer);

        mMapView = findViewById(R.id.mapView);
        ArcGISMap map = new ArcGISMap(Basemap.Type.TOPOGRAPHIC, 35.931488, 0.092265, 14);
        mMapView.setMap(map);

        boolean permissionCheck1 = ContextCompat.checkSelfPermission(MainActivity.this, reqPermissions[0]) == PackageManager.PERMISSION_GRANTED;
        boolean permissionCheck2 = ContextCompat.checkSelfPermission(MainActivity.this, reqPermissions[1]) == PackageManager.PERMISSION_GRANTED;

        if (!(permissionCheck1 && permissionCheck2)) {
            // If permissions are not already granted, request permission from the user.
            ActivityCompat.requestPermissions(MainActivity.this, reqPermissions, requestCode);
        }

        // get the MapView's LocationDisplay
        mLocationDisplay = mMapView.getLocationDisplay();
        mLocationDisplay.setInitialZoomScale(100000);

        // Listen to changes in the status of the location data source.
        mLocationDisplay.addDataSourceStatusChangedListener(new LocationDisplay.DataSourceStatusChangedListener() {
            @Override
            public void onStatusChanged(LocationDisplay.DataSourceStatusChangedEvent dataSourceStatusChangedEvent) {

                // If LocationDisplay started OK, then continue.
                if (dataSourceStatusChangedEvent.isStarted())
                    return;

                // No error is reported, then continue.
                if (dataSourceStatusChangedEvent.getError() == null)
                    return;

                // If an error is found, handle the failure to start.
                // Check permissions to see if failure may be due to lack of permissions.
                boolean permissionCheck1 = ContextCompat.checkSelfPermission(MainActivity.this, reqPermissions[0]) ==
                        PackageManager.PERMISSION_GRANTED;
                boolean permissionCheck2 = ContextCompat.checkSelfPermission(MainActivity.this, reqPermissions[1]) ==
                        PackageManager.PERMISSION_GRANTED;
                boolean permissionCheck3 = ContextCompat.checkSelfPermission(MainActivity.this, reqPermissions[2]) ==
                        PackageManager.PERMISSION_GRANTED;
                boolean permissionCheck4 = ContextCompat.checkSelfPermission(MainActivity.this, reqPermissions[3]) ==
                        PackageManager.PERMISSION_GRANTED;
                if (!(permissionCheck1 && permissionCheck2 && permissionCheck3 && permissionCheck4)) {
                    // If permissions are not already granted, request permission from the user.
                    ActivityCompat.requestPermissions(MainActivity.this, reqPermissions, requestCode);
                } else {
                    // Report other unknown failure types to the user - for example, location services may not
                    // be enabled on the device.
                    String message = String.format("Erreur dans la source: %s", dataSourceStatusChangedEvent
                            .getSource().getLocationDataSource().getError().getMessage());
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();

                    mLocationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.NAVIGATION);
                    if (!mLocationDisplay.isStarted())
                        mLocationDisplay.startAsync();
                }
            }
        });
        mLocationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.NAVIGATION);
        if (!mLocationDisplay.isStarted())
            mLocationDisplay.startAsync();


        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        FloatingActionButton mDirectionFab = (FloatingActionButton) findViewById(R.id.directionFAB);

        // update UI when attribution view changes
        final FrameLayout.LayoutParams mDirectionFabParams = (FrameLayout.LayoutParams) mDirectionFab.getLayoutParams();
        mMapView.addAttributionViewLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(
                    View view, int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int heightDelta = (bottom - oldBottom);
                mDirectionFabParams.bottomMargin += heightDelta;
            }
        });

        FloatingActionButton mReset = (FloatingActionButton) findViewById(R.id.reset);

        final FrameLayout.LayoutParams mResetParams = (FrameLayout.LayoutParams) mReset.getLayoutParams();
        mMapView.addAttributionViewLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(
                    View view, int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int heightDelta = (bottom - oldBottom);
                mResetParams.bottomMargin += heightDelta;
            }
        });
        mReset.hide();


        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(getString(R.string.progress_title));
        mProgressDialog.setMessage(getString(R.string.progress_message));

        extractZipfile();

        loadGeodatabase();

        RouteTask mRouteTask = new RouteTask(getApplicationContext(), getFilesDir().getAbsolutePath() + "/default.geodatabase", "ETUM_ND");

        Toast.makeText(MainActivity.this, "Tapez pour definir un point de depart.", Toast.LENGTH_SHORT).show();
        mMapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mMapView) {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (routeStops.size() < 2) {
                    Point wgs84Point = (Point) GeometryEngine.project(mMapView.screenToLocation(new android.graphics.Point(Math.round(e.getX()), Math.round(e.getY()))), SpatialReferences.getWgs84());
                    routeStops.add(new Stop(new Point(wgs84Point.getX(), wgs84Point.getY(), SpatialReferences.getWgs84())));
                    setupSymbols();
                } else {
                    Toast.makeText(MainActivity.this, "Le nombre maximum de stops est 2.", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        setupDrawer();
        mDirectionFab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mProgressDialog.show();
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                mDirectionFab.hide();
                mReset.show();
                final ListenableFuture<RouteParameters> listenableFuture = mRouteTask.createDefaultParametersAsync();
                listenableFuture.addDoneListener(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (listenableFuture.isDone()) {
                                int i = 0;
                                RouteParameters mRouteParams = new RouteParameters();
                                try {
                                    mRouteParams = listenableFuture.get();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                } catch (ExecutionException e) {
                                    e.printStackTrace();
                                }
                                mRouteParams.setStops(routeStops);
                                mRouteParams.setPreserveLastStop(true);
                                mRouteParams.setPreserveFirstStop(true);
                                mRouteParams.setFindBestSequence(true);
                                mRouteParams.setReturnDirections(true);

                                // getDirectionManeuvers().
                                // solve

                                RouteResult result = mRouteTask.solveRouteAsync(mRouteParams).get();
                                mProgressDialog.cancel();
                                mRouteSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLUE, 5);

                                final List routes = result.getRoutes();

                                mRoute = (Route) routes.get(0);

                                // create a mRouteSymbol graphic
                                Graphic routeGraphic = new Graphic(mRoute.getRouteGeometry(), mRouteSymbol);
                                // add mRouteSymbol graphic to the map
                                mGraphicsOverlay.getGraphics().add(routeGraphic);

                                // get directions
                                final List<DirectionManeuver> directions = mRoute.getDirectionManeuvers();

                                String[] directionsArray = new String[directions.size()];

                                for (DirectionManeuver dm : directions) {
                                    directionsArray[i++] = dm.getDirectionText();
                                }

                                Toast.makeText(MainActivity.this, "Durée moyenne estimée: "+mRoute.getCost("Time")+" minutes", Toast.LENGTH_LONG).show();

                                Log.d(TAG, directions.get(0).getGeometry().getExtent().getXMin() + "");
                                Log.d(TAG, directions.get(0).getGeometry().getExtent().getYMin() + "");


                                // Set the adapter for the list view
                                mDrawerList.setAdapter(new ArrayAdapter<>(getApplicationContext(),
                                        R.layout.directions_layout, directionsArray));
                                if (mProgressDialog.isShowing()) {
                                    mProgressDialog.dismiss();
                                }

                                mDrawerList.setOnItemClickListener((parent, view, position, id) -> {

                                    if (mGraphicsOverlay.getGraphics().size() > 3) {
                                        mGraphicsOverlay.getGraphics().remove(mGraphicsOverlay.getGraphics().size() - 1);
                                    }

                                    mDrawerLayout.closeDrawers();
                                    DirectionManeuver dm = directions.get(position);
                                    Geometry gm = dm.getGeometry();
                                    Viewpoint vp = new Viewpoint(gm.getExtent(), 20);
                                    mMapView.setViewpointAsync(vp, 3);
                                    SimpleLineSymbol selectedRouteSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID,
                                            Color.GREEN, 5);
                                    Graphic selectedRouteGraphic = new Graphic(directions.get(position).getGeometry(),
                                            selectedRouteSymbol);
                                    mGraphicsOverlay.getGraphics().add(selectedRouteGraphic);
                                });

                                mDrawerLayout.openDrawer(3,true);

                            }
                        } catch (Exception e) {
                            Log.e(TAG, e.getMessage());
                            mProgressDialog.cancel();
                            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
        mReset.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mDirectionFab.show();
                mReset.hide();
                routeStops.clear();
                mMapView.getGraphicsOverlays().clear();

            }
        });
    }

    @Override
    protected void onPause() {
        mMapView.pause();
        super.onPause();
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

    private void loadGeodatabase() {

        // create a new geodatabase from local path
        final Geodatabase geodatabase = new Geodatabase(getFilesDir().getAbsolutePath() + "/default.geodatabase");
        // load the geodatabase
        geodatabase.loadAsync();
        geodatabase.addDoneLoadingListener(() -> {
            if (geodatabase.getLoadStatus() == LoadStatus.LOADED) {

                FeatureLayer featureLayer = null;
                for (int i = 0; geodatabase.getGeodatabaseFeatureTableByServiceLayerId(i) != null; i++) {
                    // access the geodatabase's feature table Trailheads
                    GeodatabaseFeatureTable geodatabaseFeatureTable = geodatabase.getGeodatabaseFeatureTable(geodatabase.getGeodatabaseFeatureTableByServiceLayerId(i).getTableName());
                    geodatabaseFeatureTable.loadAsync();
                    // create a layer from the geodatabase feature table and add to map
                    featureLayer = new FeatureLayer(geodatabaseFeatureTable);
                    // add feature layer to the map
                    mMapView.getMap().getOperationalLayers().add(featureLayer);
                }
            } else {
                Toast.makeText(MainActivity.this, "Impossible de charger la base de données.", Toast.LENGTH_LONG).show();

            }
        });
        }

    private void setupSymbols() {

        mGraphicsOverlay = new GraphicsOverlay();

        //add the overlay to the map view
        mMapView.getGraphicsOverlays().add(mGraphicsOverlay);

        if (routeStops.size() == 1) {
            BitmapDrawable startDrawable = (BitmapDrawable) ContextCompat.getDrawable(this, R.drawable.ic_source);
            final PictureMarkerSymbol pinSourceSymbol;
            try {
                pinSourceSymbol = PictureMarkerSymbol.createAsync(startDrawable).get();
                pinSourceSymbol.loadAsync();
                pinSourceSymbol.addDoneLoadingListener(new Runnable() {
                    @Override
                    public void run() {
                        //add a new graphic as start point
                        mSourcePoint = new Point(routeStops.get(0).getGeometry().getX(), routeStops.get(0).getGeometry().getY(), SpatialReferences.getWgs84());
                        Graphic pinSourceGraphic = new Graphic(mSourcePoint, pinSourceSymbol);
                        mGraphicsOverlay.getGraphics().add(pinSourceGraphic);
                    }
                });
                pinSourceSymbol.setOffsetY(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            Toast.makeText(MainActivity.this, "Taper pour definir un point de destination.", Toast.LENGTH_SHORT).show();
        } else if (routeStops.size() == 2) {


            BitmapDrawable endDrawable = (BitmapDrawable) ContextCompat.getDrawable(this, R.drawable.ic_destination);
            final PictureMarkerSymbol pinDestinationSymbol;
            try {
                pinDestinationSymbol = PictureMarkerSymbol.createAsync(endDrawable).get();
                pinDestinationSymbol.loadAsync();
                pinDestinationSymbol.addDoneLoadingListener(new Runnable() {
                    @Override
                    public void run() {
                        //add a new graphic as end point
                        mDestinationPoint = new Point(routeStops.get(1).getGeometry().getX(), routeStops.get(1).getGeometry().getY(), SpatialReferences.getWgs84());
                        Graphic destinationGraphic = new Graphic(mDestinationPoint, pinDestinationSymbol);
                        mGraphicsOverlay.getGraphics().add(destinationGraphic);
                    }
                });
                pinDestinationSymbol.setOffsetY(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            Toast.makeText(MainActivity.this, "Cliquez sur le bouton en bas a droite pour calculer l'itinéraire.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupDrawer() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.addDrawerListener(mDrawerToggle);

        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    private void extractZipfile() {
        String extractDir = getFilesDir().getAbsolutePath() + "/";
        int BUFFER = 2048;
        try {
            BufferedOutputStream dest = null;
            ZipInputStream zis = new ZipInputStream(getAssets().open("gdb.zip"));
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File file = new File(extractDir + entry.getName());

                if (file.exists()) {
                    continue;
                }
                if (entry.isDirectory()) {
                    if (!file.exists())
                        file.mkdirs();
                    continue;
                }
                int count;
                byte data[] = new byte[BUFFER];
                FileOutputStream fos = new FileOutputStream(file);
                dest = new BufferedOutputStream(fos, BUFFER);
                while ((count = zis.read(data, 0, BUFFER)) != -1) {
                    dest.write(data, 0, count);
                }
                dest.flush();
                dest.close();
            }
            zis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}