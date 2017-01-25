package com.fenchtose.mapsdemo;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.fenchtose.mapsdemo.db.AbstractDao;
import com.fenchtose.mapsdemo.db.DatabaseHandler;
import com.fenchtose.mapsdemo.db.LocationDbItem;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;


public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, OnMapReadyCallback{

    private static final String TAG = "MainActivity";
    private static final int REQUEST_LOCATION_PERMISSION = 21;

    private GoogleApiClient googleApiClient;
    private Location lastLocation;

    private MapFragment mapFragment;
    private GoogleMap googleMap;

    private Marker currentMarker;
    private List<Marker> markers;
    private List<LatLng> locations;

    private LatLng currentLatLng;

    private ImageView centerImageView;

    private FloatingActionButton fab;

    private PublishSubject<Boolean> mapMovementSubject;
    private Subscription mapMovementSubscription;

    private DatabaseHandler dbHandler;
    private AbstractDao<LocationDbItem> locationDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHandler = new DatabaseHandler(getApplicationContext());
        locationDao = dbHandler.getLocationDao();

        setContentView(R.layout.activity_main);
        mapFragment = (MapFragment)getFragmentManager().findFragmentById(R.id.map);
        centerImageView = (ImageView)findViewById(R.id.center_imageview);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addLocation();
            }
        });
        fab.setVisibility(View.GONE);

        markers = new ArrayList<>();
        locations = new ArrayList<>();

        buildGoogleApiClient();
        mapFragment.getMapAsync(this);
    }

    private synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapMovementSubject != null) {
            mapMovementSubject.onCompleted();
        }

        if (mapMovementSubscription != null) {
            mapMovementSubscription.unsubscribe();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.delete_locations) {
            deleteAllLocations();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        getLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "connection failed: " + connectionResult.getErrorMessage());
        // TODO show error
    }

    private void getLocation() {
        if (!hasPermissions()) {
            askForPermissions();
            return;
        }

        //noinspection MissingPermission
        lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        Log.d(TAG, "last location: " + lastLocation);
        if (lastLocation != null) {
            setupMaps(lastLocation);
        }
    }

    private boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void askForPermissions() {
//        boolean showRationale = shouldShowRequestPermissionRationale(Manifest.);
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissions.length == 1 && grantResults.length == 1
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocation();
            return;
        }

        // TODO show error
    }

    @Override
    public void onMapReady(GoogleMap map) {

        Log.d(TAG, "onMapReady");
        googleMap = map;

        if (lastLocation == null) {
            getLocation();
            return;
        }

        setupMaps(lastLocation);
    }

    private void setupMovementObservable(final GoogleMap map) {
        mapMovementSubject = PublishSubject.create();

        map.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                mapMovementSubject.onNext(true);
            }
        });

        map.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int i) {
                mapMovementSubject.onNext(false);
            }
        });

        mapMovementSubscription = mapMovementSubject
                .doOnNext(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean finished) {
                        if (!finished) {
                            removeCurrentMarker();
                            hideFab(true);
                        }
                    }
                })
                .debounce(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .filter(new Func1<Boolean, Boolean>() {
                    @Override
                    public Boolean call(Boolean aBoolean) {
                        return aBoolean;
                    }
                })
                .map(new Func1<Boolean, LatLng>() {
                    @Override
                    public LatLng call(Boolean aBoolean) {
                        if (map != null) {
                            return map.getCameraPosition().target;
                        }

                        return null;
                    }
                })
                .subscribe(new Action1<LatLng>() {
                    @Override
                    public void call(LatLng latLng) {
                        if (latLng != null) {
                            currentLatLng = latLng;
                            addCurrentMarker(map, latLng);
                            showFab(true);
                        }
                    }
                });
    }

    @SuppressWarnings("MissingPermission")
    private void setupMaps(Location location) {
        if (googleMap == null) {
            Log.e(TAG, "map is null");
            return;
        }

        setupMovementObservable(googleMap);

        // Gives my location on the map
        googleMap.setMyLocationEnabled(true);

        LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 14));

        List<LocationDbItem> items = locationDao.getAll();
        if (items != null) {
            for (LocationDbItem item : items) {
                addLocationOnMap(new LatLng(item.getLatitude(), item.getLongitude()));
            }
        }

        showFab(true);
    }

    private void removeCurrentMarker() {
        if (currentMarker != null) {
            currentMarker.remove();
            currentMarker = null;
        }
    }

    private void addCurrentMarker(@NonNull GoogleMap map, @NonNull LatLng position) {

        removeCurrentMarker();

        currentMarker = map.addMarker(new MarkerOptions()
                .title("You")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_add_location_grey_600_48dp))
                .position(position)
        );

    }

    private Marker addMarker(@NonNull GoogleMap map, @NonNull LatLng position) {
        return map.addMarker(new MarkerOptions()
                .title("You")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location_on_grey_600_24dp))
                .position(position));
    }

    private void addLocation() {
        if (currentLatLng == null || googleMap == null) {
            Log.e(TAG, "current latlng is null");
            // show error
            return;
        }

        if (addLocationOnMap(currentLatLng)) {
            locationDao.add(LocationDbItem.newInstance(currentLatLng));
        }
    }

    private void deleteAllLocations() {
        Observable.just(deleteLocationsFromDB())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        for (Marker marker : markers) {
                            marker.remove();
                        }

                        markers.clear();
                        locations.clear();
                    }
                });

    }

    private boolean deleteLocationsFromDB() {
        List<LocationDbItem> items = locationDao.getAll();
        if (items != null) {
            for (LocationDbItem item : items) {
                locationDao.delete(item);
            }

            return true;
        }

        return false;
    }

    private boolean addLocationOnMap(@NonNull LatLng latLng) {
        if (googleMap == null) {
            return false;
        }

        double minDistance = getMinDistance(locations, latLng);

        // 10^-6
        if (minDistance < 0.000001) {
            return false;
        }

        locations.add(latLng);
        Marker marker = addMarker(googleMap, latLng);
        markers.add(marker);

        return true;
    }

    private double getMinDistance(@NonNull List<LatLng> locations, @NonNull LatLng location) {

        double minDistance = location.latitude * location.latitude + location.longitude * location.longitude;

        for (LatLng latLng : locations) {
            double distance = Math.pow((location.latitude - latLng.latitude), 2) + Math.pow((location.longitude - latLng.longitude), 2);
            if (distance < minDistance) {
                minDistance = distance;
            }
        }

        return minDistance;
    }

    private void showFab(boolean animate) {

        if (!animate) {
            fab.setVisibility(View.VISIBLE);
            return;
        }

        if (fab.getVisibility() == View.VISIBLE) {
            // already visible
            return;
        }

        // cancel animations

        if (fab.getAnimation() != null) {
            fab.getAnimation().cancel();
        }

        fab.setVisibility(View.VISIBLE);
        ViewCompat.animate(fab).scaleX(1).scaleY(1).setDuration(200).withLayer()
                // Add listener because there is some problem there
                .setListener(new ViewPropertyAnimatorListener() {
                    @Override
                    public void onAnimationStart(View view) {

                    }

                    @Override
                    public void onAnimationEnd(View view) {
                    }

                    @Override
                    public void onAnimationCancel(View view) {

                    }
                })
                .start();
    }

    private void hideFab(boolean animate) {

        if (!animate) {
            fab.setVisibility(View.GONE);
            return;
        }

        if (fab.getVisibility() == View.GONE) {
            return;
        }

        if (fab.getAnimation() != null) {
            fab.getAnimation().cancel();
        }

        ViewCompat.animate(fab).scaleX(0).scaleY(0).setDuration(200).withLayer()
                .setListener(new ViewPropertyAnimatorListener() {
                    @Override
                    public void onAnimationStart(View view) {

                    }

                    @Override
                    public void onAnimationEnd(View view) {
                        fab.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationCancel(View view) {

                    }
                }).start();
    }

}
