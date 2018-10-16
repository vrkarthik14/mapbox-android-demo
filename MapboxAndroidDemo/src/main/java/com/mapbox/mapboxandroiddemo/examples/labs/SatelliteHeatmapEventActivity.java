package com.mapbox.mapboxandroiddemo.examples.labs;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxandroiddemo.R;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.HeatmapLayer;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.RasterLayer;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.style.sources.RasterSource;
import com.mapbox.mapboxsdk.style.sources.Source;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import timber.log.Timber;

import static com.mapbox.mapboxsdk.camera.CameraUpdateFactory.newCameraPosition;
import static com.mapbox.mapboxsdk.style.expressions.Expression.heatmapDensity;
import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.linear;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.rgb;
import static com.mapbox.mapboxsdk.style.expressions.Expression.rgba;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.expressions.Expression.zoom;
import static com.mapbox.mapboxsdk.style.layers.Property.ICON_ANCHOR_BOTTOM;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapIntensity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapRadius;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAnchor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.rasterOpacity;

/**
 * Use the style API to highlight different types of data.
 * In this example, parks, hotels, and attractions are displayed.
 */
public class SatelliteHeatmapEventActivity extends AppCompatActivity implements
  OnMapReadyCallback, MapboxMap.OnMapClickListener, MapboxMap.OnCameraMoveListener {

  private static final String TAG = "SatelliteHeatmapEvent";
  private static final String POI_SOURCE_ID = "mapbox.poi";
  private static final String POI_LAYER_ID = "mapbox.poi";
  private static final String HIGHLIGHTED_EVENT_LAYER_ID = "HIGHLIGHTED_EVENT_LAYER_ID";
  private static final String PROPERTY_SELECTED = "selected";
  private static final String PROPERTY_EVENT_TITLE = "title";
  private static final String HEATMAP_LAYER_ID = "earthquakes-heat";
  private static final String HEATMAP_LAYER_SOURCE_ID = "earthquakes";
  private static final String SATELLITE_RASTER_SOURCE_ID = "satellite-raster-source-id";
  private static final String SATELLITE_RASTER_LAYER_ID = "satellite-raster-layer-id";
  private static final String HIGHLIGHTED_EVENT_GEOJSON_SOURCE_ID = "HIGHLIGHTED_EVENT_GEOJSON_SOURCE_ID";
  private static String HEATMAP_URL;
  private static String HIGHLIGHTED_EVENT_URL;
  private MapboxMap mapboxMap;
  private MapView mapView;
  private GeoJsonSource highlightedEventSource;
  private GeoJsonSource poiSymbolLayerSource;
  private Layer poiLayer;
  private SymbolLayer highlightedEventSymbolLayer;
  private FeatureCollection highlightedEventFeatureCollection;
  private HashMap<String, View> viewMap;
  private Expression[] listOfHeatmapColors;
  private Expression[] listOfHeatmapRadiusStops;
  private Float[] listOfHeatmapIntensityStops;
  private int heatmapSwitchIndex;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    heatmapSwitchIndex = 0;

    HEATMAP_URL =
      "https://api.mapbox.com/datasets/v1/appsatmapboxcom/"
        + "cjmmk40py05kv2vnki7g6ikrd/features?&access_token="
        + getString(R.string.access_token);

    HIGHLIGHTED_EVENT_URL =
      "https://api.mapbox.com/datasets/v1/appsatmapboxcom/"
        + "cjn9nigoz38ru2wnvkco35bv6/features?&access_token="
        + getString(R.string.access_token);


    // Mapbox access token is configured here. This needs to be called either in your application
    // object or in the same activity which contains the mapview.
    Mapbox.getInstance(this, getString(R.string.access_token));

    // This contains the MapView in XML and needs to be called after the access token is configured.
    setContentView(R.layout.activity_satellite_event_heatmap);

    mapView = findViewById(R.id.mapView);
    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);
  }

  @Override
  public void onMapReady(MapboxMap mapboxMap) {

    this.mapboxMap = mapboxMap;

    for (Layer singleLayer : mapboxMap.getLayers()) {
      Log.d(TAG, "onMapReady: singleLayer id = " + singleLayer.getId());
    }

    mapboxMap.addOnCameraMoveListener(this);

    if (mapboxMap.getLayer("water") != null) {
      mapboxMap.getLayer("water").setProperties(fillColor(Color.parseColor("#4793F0")));
    }
    if (mapboxMap.getLayer("park") != null) {
      mapboxMap.getLayer("park").setProperties(fillColor(Color.parseColor("#8CD174")));
    }
    if (mapboxMap.getLayer("landcover_grass") != null) {
      mapboxMap.getLayer("landcover_grass").setProperties(fillColor(Color.parseColor("#519B37")));
    }
    if (mapboxMap.getLayer("landcover_crop") != null) {
      mapboxMap.getLayer("landcover_crop").setProperties(fillColor(Color.parseColor("#7DFB51")));
    }

    setUpData(FeatureCollection.fromFeatures(new Feature[] {}));
    mapboxMap.addOnMapClickListener(this);
//    initRecyclerView();
  }

  private void setUpPoiSource() {
    /*try {
      mapboxMap.addSource(new GeoJsonSource(POI_SOURCE_ID, ));
    } catch (MalformedURLException malformedUrlException) {
      Timber.e(malformedUrlException, "That's not an url... ");
    }*/
  }

  private void setUpPoiLayer() {
    SymbolLayer poiSymbolLayer = new SymbolLayer(POI_LAYER_ID, POI_SOURCE_ID)
      .withProperties(
        iconImage("{poi}-15"),
        /* allows show all icons */
        iconAllowOverlap(true),
        iconIgnorePlacement(true)
      );
    poiSymbolLayer.setMinZoom(14.5f);
    mapboxMap.addLayer(poiSymbolLayer);
  }

  @Override
  public void onMapClick(@NonNull LatLng point) {
    handleClickIcon(mapboxMap.getProjection().toScreenLocation(point));
  }

  /**
   * Sets up all of the sources and layers needed for this example
   *
   * @param collection the FeatureCollection to set equal to the globally-declared FeatureCollection
   */
  public void setUpData(final FeatureCollection collection) {
    Log.d(TAG, "addData: starting");

    if (mapboxMap == null) {
      return;
    }

    highlightedEventFeatureCollection = collection;

    /*addHillshadeSource();
    addHillshadeLayer();*/


//    new LoadHighlightedEventGeoJsonDataTask(this).execute();
    Log.d(TAG, "onMapReady: starting heatmap additions");
    addHighlightedEvent();


    addHeatmap();

    // Add satellite raster layer for viewing satellite photos once the camera is close enough to the mapboxMap
    addSatellite();

    // Add POI
    addPoi();

    // Add separate layer of POI text so that POIs appear when zoomed in close enough to the mapboxMap
    addHighlightedEvent();
  }

  /**
   * Add data to the map and display it in a heatmap layer
   */
  private void addHeatmap() {
    initHeatmapColors();
    initHeatmapRadiusStops();
    initHeatmapIntensityStops();
    addHeatmapSource();
    addHeatmapLayer();
  }

  /**
   * Add a satellite raster layer that's displayed when the camera is close enough to the map
   */
  private void addSatellite() {
    addSatelliteRasterSource();
    addSatelliteRasterLayer();
  }

  /**
   * Add a SymbolLayer with POI data that's displayed when the camera is close enough to the map
   */
  private void addPoi() {
    /*setPoiLayer();
    setUpPoiSource();
    setUpPoiLayer();*/
  }

  /**
   * Add a SymbolLayer with special event icons
   */
  private void addHighlightedEvent() {
    initHighlightedEventFeatureCollection();
    initHighlightedEventLayer();
  }

  /**
   * Callback for when the map camera moves.
   */
  @Override
  public void onCameraMove() {
//    Log.d(TAG, "onCameraMove: camera zoom = " + mapboxMap.getCameraPosition().zoom);

    /*if (mapboxMap.getCameraPosition().zoom >= 14) {
      mapboxMap.addLayer(poiLayer);
    }
    if (mapboxMap.getCameraPosition().zoom < 14) {
      mapboxMap.removeLayer(poiLayer);
    }*/
  }

  /**
   * Adds the GeoJSON source to the mapboxMap
   */
  private void initHighlightedEventFeatureCollection() {
    try {
      highlightedEventSource = new GeoJsonSource(HIGHLIGHTED_EVENT_GEOJSON_SOURCE_ID, new URL(
        HIGHLIGHTED_EVENT_URL));
      mapboxMap.addSource(highlightedEventSource);
    } catch (MalformedURLException malformedUrlException) {
      Timber.e(malformedUrlException, "That's not an url... ");
    }
  }

  /**
   * Setup a layer with Android SDK call-outs
   * <p>
   * name of the feature is used as key for the iconImage
   * </p>
   */
  private void initHighlightedEventLayer() {
    highlightedEventSymbolLayer = new SymbolLayer(HIGHLIGHTED_EVENT_LAYER_ID, HIGHLIGHTED_EVENT_GEOJSON_SOURCE_ID)
      .withProperties(
        /* show image with id title based on the value of the name feature property */
        iconImage("{name}"),

        /* set anchor of icon to bottom-left */
        iconAnchor(ICON_ANCHOR_BOTTOM),

        /* all info window and marker image to appear at the same time*/
        iconAllowOverlap(true),

        /* offset the info window to be above the marker */
        iconOffset(new Float[] {-2f, -25f})
      );

    mapboxMap.addLayer(highlightedEventSymbolLayer);
    // TODO: Uncomment out code below?
    /* add a filter to show only when selected feature property is true *//*
      .withFilter(eq((get(PROPERTY_SELECTED)), literal(true))));*/
  }


  /**
   * Updates the display of data on the mapboxMap after the FeatureCollection has been modified
   */
  private void refreshSource() {
    Log.d(TAG, "refreshSource: refreshSource()");
    if (highlightedEventSource != null && highlightedEventFeatureCollection != null) {
      highlightedEventSource.setGeoJson(highlightedEventFeatureCollection);
      Log.d(TAG, "refreshSource:       source.setGeoJson(highlightedEventFeatureCollection);\n");
    }
  }

  /**
   * This method handles click events for SymbolLayer symbols.
   * <p>
   * When a SymbolLayer icon is clicked, we moved that feature to the selected state.
   * </p>
   *
   * @param screenPoint the point on screen clicked
   */
  private void handleClickIcon(PointF screenPoint) {
    List<Feature> features = mapboxMap.queryRenderedFeatures(screenPoint, HIGHLIGHTED_EVENT_LAYER_ID);
    if (!features.isEmpty()) {
      String name = features.get(0).getStringProperty(PROPERTY_EVENT_TITLE);
      List<Feature> featureList = highlightedEventFeatureCollection.features();
      for (int i = 0; i < featureList.size(); i++) {
        if (featureList.get(i).getStringProperty(PROPERTY_EVENT_TITLE).equals(name)) {
          if (featureSelectStatus(i)) {
            setFeatureSelectState(featureList.get(i), false);
          } else {
            setSelected(i);
          }

          Point selectedFeaturePoint = (Point) featureList.get(i).geometry();
          CameraPosition position = new CameraPosition.Builder()
            .target(new LatLng(selectedFeaturePoint.latitude(),
              selectedFeaturePoint.longitude())) // Sets the new camera position
            .build(); // Creates a CameraPosition from the builder

          mapboxMap.animateCamera(newCameraPosition(position), 15000);
        }
      }
    }
  }

  /**
   * Set a feature selected state.
   *
   * @param index the heatmapSwitchIndex of selected feature
   */
  private void setSelected(int index) {
    Feature feature = highlightedEventFeatureCollection.features().get(index);
    setFeatureSelectState(feature, true);
    refreshSource();
  }

  /**
   * Selects the state of a feature
   *
   * @param feature the feature to be selected.
   */
  private void setFeatureSelectState(Feature feature, boolean selectedState) {
    feature.properties().addProperty(PROPERTY_SELECTED, selectedState);
    refreshSource();
  }

  /**
   * Checks whether a Feature's boolean "selected" property is true or false
   *
   * @param index the specific Feature's heatmapSwitchIndex position in the FeatureCollection's list of Features.
   * @return true if "selected" is true. False if the boolean property is false.
   */
  private boolean featureSelectStatus(int index) {
    if (highlightedEventFeatureCollection == null) {
      return false;
    }
    return highlightedEventFeatureCollection.features().get(index).getBooleanProperty(PROPERTY_SELECTED);
  }

  /**
   * Invoked when the bitmaps have been generated from a view.
   */
  public void setImageGenResults(HashMap<String, View> viewMap, HashMap<String, Bitmap> imageMap) {
    if (mapboxMap != null) {
      // calling addImages is faster as separate addImage calls for each bitmap.
      mapboxMap.addImages(imageMap);
    }
    // need to store reference to views to be able to use them as hit boxes for click events.
    this.viewMap = viewMap;
  }

  /**
   * AsyncTask to load data from the assets folder.
   */
  private static class LoadHighlightedEventGeoJsonDataTask extends AsyncTask<Void, Void, FeatureCollection> {

    private final WeakReference<SatelliteHeatmapEventActivity> activityRef;

    LoadHighlightedEventGeoJsonDataTask(SatelliteHeatmapEventActivity activity) {
      this.activityRef = new WeakReference<>(activity);
    }

    @Override
    protected FeatureCollection doInBackground(Void... params) {
      SatelliteHeatmapEventActivity activity = activityRef.get();

      if (activity == null) {
        return null;
      }

      // TODO: Add appropriate GeoJSON data for newsworthy event
      String geoJson = loadGeoJsonFromAsset(activity, "sf_poi.geojson");
      return FeatureCollection.fromJson(geoJson);
    }

    @Override
    protected void onPostExecute(FeatureCollection highlightedEventFeatureCollection) {
      super.onPostExecute(highlightedEventFeatureCollection);
      SatelliteHeatmapEventActivity activity = activityRef.get();
      if (highlightedEventFeatureCollection == null || activity == null) {
        return;
      }
      activity.setUpData(highlightedEventFeatureCollection);
      new GenerateViewIconTask(activity).execute(highlightedEventFeatureCollection);
    }

    static String loadGeoJsonFromAsset(Context context, String filename) {
      try {
        // Load GeoJSON file from local asset folder
        InputStream is = context.getAssets().open(filename);
        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();
        return new String(buffer, "UTF-8");
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
    }
  }

  /**
   * AsyncTask to generate Bitmap from Views to be used as iconImage in a SymbolLayer.
   * <p>
   * Call be optionally be called to update the underlying data source after execution.
   * </p>
   * <p>
   * Generating Views on background thread since we are not going to be adding them to the view hierarchy.
   * </p>
   */
  private static class GenerateViewIconTask extends AsyncTask<FeatureCollection, Void, HashMap<String, Bitmap>> {

    private final HashMap<String, View> viewMap = new HashMap<>();
    private final WeakReference<SatelliteHeatmapEventActivity> activityRef;
    private final boolean refreshSource;

    GenerateViewIconTask(SatelliteHeatmapEventActivity activity, boolean refreshSource) {
      this.activityRef = new WeakReference<>(activity);
      this.refreshSource = refreshSource;
    }

    GenerateViewIconTask(SatelliteHeatmapEventActivity activity) {
      this(activity, false);
    }

    @SuppressWarnings("WrongThread")
    @Override
    protected HashMap<String, Bitmap> doInBackground(FeatureCollection... params) {
      SatelliteHeatmapEventActivity activity = activityRef.get();
      if (activity != null) {
        HashMap<String, Bitmap> imagesMap = new HashMap<>();
        LayoutInflater inflater = LayoutInflater.from(activity);

        FeatureCollection highlightedEventFeatureCollection = params[0];

        for (Feature feature : highlightedEventFeatureCollection.features()) {

          FrameLayout highlightedEventFrameLayout = (FrameLayout)
            inflater.inflate(R.layout.symbol_layer_satellite_heatmap_event_callout, null);

          ImageView eventImageView = highlightedEventFrameLayout.findViewById(R.id.heatmap_event_imageview);
          eventImageView.setImageDrawable(ContextCompat.getDrawable(activity.getApplicationContext(),
            R.drawable.heatmap_event_baseball_game));

          String eventTitle = feature.getStringProperty(PROPERTY_EVENT_TITLE);
          Log.d(TAG, "doInBackground: eventTitle = " + eventTitle);
          TextView titleTextView = highlightedEventFrameLayout.findViewById(R.id.heatmap_event_title_textview);
          titleTextView.setText(eventTitle);

          Bitmap bitmap = SymbolGenerator.generate(highlightedEventFrameLayout);
          imagesMap.put(eventTitle, bitmap);
          viewMap.put(eventTitle, highlightedEventFrameLayout);
        }
        return imagesMap;
      } else {
        return null;
      }
    }

    @Override
    protected void onPostExecute(HashMap<String, Bitmap> bitmapHashMap) {
      super.onPostExecute(bitmapHashMap);
      SatelliteHeatmapEventActivity activity = activityRef.get();
      if (activity != null && bitmapHashMap != null) {
        activity.setImageGenResults(viewMap, bitmapHashMap);
        if (refreshSource) {
          activity.refreshSource();
        }
      }
    }
  }

  /**
   * Utility class to generate Bitmaps for Symbol.
   * <p>
   * Bitmaps can be added to the mapboxMap with {@link com.mapbox.mapboxsdk.maps.MapboxMap#addImage(String, Bitmap)}
   * </p>
   */
  private static class SymbolGenerator {

    /**
     * Generate a Bitmap from an Android SDK View.
     *
     * @param view the View to be drawn to a Bitmap
     * @return the generated bitmap
     */
    static Bitmap generate(@NonNull View view) {
      int measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
      view.measure(measureSpec, measureSpec);

      int measuredWidth = view.getMeasuredWidth();
      int measuredHeight = view.getMeasuredHeight();

      view.layout(0, 0, measuredWidth, measuredHeight);
      Bitmap bitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888);
      bitmap.eraseColor(Color.TRANSPARENT);
      Canvas canvas = new Canvas(bitmap);
      view.draw(canvas);
      return bitmap;
    }
  }

  private void addHeatmapSource() {
    try {
      mapboxMap.addSource(new GeoJsonSource(HEATMAP_LAYER_SOURCE_ID, new URL(
        HEATMAP_URL)));
    } catch (MalformedURLException malformedUrlException) {
      Timber.e(malformedUrlException, "That's not an url... ");
    }
  }

  private void addHeatmapLayer() {
    HeatmapLayer heatmapLayer = new HeatmapLayer(HEATMAP_LAYER_ID, HEATMAP_LAYER_SOURCE_ID);
    heatmapLayer.setSourceLayer(HEATMAP_LAYER_SOURCE_ID);
    heatmapLayer.setProperties(
      heatmapColor(listOfHeatmapColors[heatmapSwitchIndex]),

      // Increase the heatmap color weight weight by zoom level
      // heatmap-intensity is a multiplier on top of heatmap-weight
      heatmapIntensity(listOfHeatmapIntensityStops[heatmapSwitchIndex]),

      // Adjust the heatmap radius by zoom level
      heatmapRadius(listOfHeatmapRadiusStops[heatmapSwitchIndex]
      ),

      heatmapOpacity(1f)
    );

    if (mapboxMap.getLayer("waterway-label") == null) {
      mapboxMap.addLayer(heatmapLayer);
    } else {
      mapboxMap.addLayerAbove(heatmapLayer, "waterway-label");
    }
  }

  private void addSatelliteRasterSource() {
    // Create a data source for the satellite raster images
    Source satelliteRasterSource = new RasterSource(SATELLITE_RASTER_SOURCE_ID,
      "mapbox://mapbox.satellite", 512);

    // Add the source to the mapboxMap
    mapboxMap.addSource(satelliteRasterSource);
  }

  private void addSatelliteRasterLayer() {
    // Create a new mapboxMap layer for the satellite raster images
    RasterLayer satelliteRasterLayer = new RasterLayer(SATELLITE_RASTER_LAYER_ID,
      SATELLITE_RASTER_SOURCE_ID);

    // Use runtime styling to adjust the satellite layer's opacity based on the mapboxMap camera's zoom level
    satelliteRasterLayer.withProperties(
      rasterOpacity(interpolate(linear(), zoom(),
        stop(13.5, 0),
        stop(16.5, 1)
      ))
    );

    // Add the satellite layer to the mapboxMap
    mapboxMap.addLayer(satelliteRasterLayer);
  }

  private void setPoiLayer() {
    Log.d(TAG, "setPoiLayer: setPoiLayer");
    // Create a data source for the satellite raster images
    poiLayer = mapboxMap.getLayer("poi-scalerank4-l1");
  }

  private void initHeatmapColors() {
    listOfHeatmapColors = new Expression[] {
      // 0
      interpolate(
        linear(), heatmapDensity(),
        literal(0.01), rgba(0, 0, 0, 0.01),
        literal(0.1), rgba(0, 2, 114, .1),
        literal(0.2), rgba(0, 6, 219, .15),
        literal(0.3), rgba(0, 74, 255, .2),
        literal(0.4), rgba(0, 202, 255, .25),
        literal(0.5), rgba(73, 255, 154, .3),
        literal(0.6), rgba(171, 255, 59, .35),
        literal(0.7), rgba(255, 197, 3, .4),
        literal(0.8), rgba(255, 82, 1, 0.7),
        literal(0.9), rgba(196, 0, 1, 0.8),
        literal(0.95), rgba(121, 0, 0, 0.8)
      ),
      // 1
      interpolate(
        linear(), heatmapDensity(),
        literal(0.01), rgba(0, 0, 0, 0.01),
        literal(0.1), rgba(0, 2, 114, .1),
        literal(0.2), rgba(0, 6, 219, .15),
        literal(0.3), rgba(0, 74, 255, .2),
        literal(0.4), rgba(0, 202, 255, .25),
        literal(0.5), rgba(73, 255, 154, .3),
        literal(0.6), rgba(171, 255, 59, .35),
        literal(0.7), rgba(255, 197, 3, .4),
        literal(0.8), rgba(255, 82, 1, 0.7),
        literal(0.9), rgba(196, 0, 1, 0.8),
        literal(0.95), rgba(121, 0, 0, 0.8)
      ),
      // 2
      interpolate(
        linear(), heatmapDensity(),
        literal(0.01), rgba(0, 0, 0, 0.01),
        literal(0.1), rgba(0, 2, 114, .1),
        literal(0.2), rgba(0, 6, 219, .15),
        literal(0.3), rgba(0, 74, 255, .2),
        literal(0.4), rgba(0, 202, 255, .25),
        literal(0.5), rgba(73, 255, 154, .3),
        literal(0.6), rgba(171, 255, 59, .35),
        literal(0.7), rgba(255, 197, 3, .4),
        literal(0.8), rgba(255, 82, 1, 0.7),
        literal(0.9), rgba(196, 0, 1, 0.8),
        literal(0.95), rgba(121, 0, 0, 0.8)
      ),
      // 3
      interpolate(
        linear(), heatmapDensity(),
        literal(0.01), rgba(0, 0, 0, 0.25),
        literal(0.25), rgba(229, 12, 1, .7),
        literal(0.30), rgba(244, 114, 1, .7),
        literal(0.40), rgba(255, 205, 12, .7),
        literal(0.50), rgba(255, 229, 121, .8),
        literal(1), rgba(255, 253, 244, .8)
      ),
      // 4
      interpolate(
        linear(), heatmapDensity(),
        literal(0.01), rgba(0, 0, 0, 0.01),
        literal(0.25), rgba(224, 176, 63, 0.5),
        literal(0.5), rgb(247, 252, 84),
        literal(0.75), rgb(186, 59, 30),
        literal(0.9), rgb(255, 0, 0)
      ),
      // 5
      interpolate(
        linear(), heatmapDensity(),
        literal(0.01), rgba(255, 255, 255, 0.4),
        literal(0.25), rgba(4, 179, 183, 1.0),
        literal(0.5), rgba(204, 211, 61, 1.0),
        literal(0.75), rgba(252, 167, 55, 1.0),
        literal(1), rgba(255, 78, 70, 1.0)
      ),
      // 6
      interpolate(
        linear(), heatmapDensity(),
        literal(0.01), rgba(12, 182, 253, 0.0),
        literal(0.25), rgba(87, 17, 229, 0.5),
        literal(0.5), rgba(255, 0, 0, 1.0),
        literal(0.75), rgba(229, 134, 15, 0.5),
        literal(1), rgba(230, 255, 55, 0.6)
      ),
      // 7
      interpolate(
        linear(), heatmapDensity(),
        literal(0.01), rgba(135, 255, 135, 0.2),
        literal(0.5), rgba(255, 99, 0, 0.5),
        literal(1), rgba(47, 21, 197, 0.2)
      ),
      // 8
      interpolate(
        linear(), heatmapDensity(),
        literal(0.01), rgba(4, 0, 0, 0.2),
        literal(0.25), rgba(229, 12, 1, 1.0),
        literal(0.30), rgba(244, 114, 1, 1.0),
        literal(0.40), rgba(255, 205, 12, 1.0),
        literal(0.50), rgba(255, 229, 121, 1.0),
        literal(1), rgba(255, 253, 244, 1.0)
      ),
      // 9
      interpolate(
        linear(), heatmapDensity(),
        literal(0.01), rgba(0, 0, 0, 0.01),
        literal(0.05), rgba(0, 0, 0, 0.05),
        literal(0.4), rgba(254, 142, 2, 0.7),
        literal(0.5), rgba(255, 165, 5, 0.8),
        literal(0.8), rgba(255, 187, 4, 0.9),
        literal(0.95), rgba(255, 228, 173, 0.8),
        literal(1), rgba(255, 253, 244, .8)
      ),
      // 10
      interpolate(
        linear(), heatmapDensity(),
        literal(0.01), rgba(0, 0, 0, 0.01),
        literal(0.3), rgba(82, 72, 151, 0.4),
        literal(0.4), rgba(138, 202, 160, 1.0),
        literal(0.5), rgba(246, 139, 76, 0.9),
        literal(0.9), rgba(252, 246, 182, 0.8),
        literal(1), rgba(255, 255, 255, 0.8)
      ),

      // 11
      interpolate(
        linear(), heatmapDensity(),
        literal(0.01), rgba(0, 0, 0, 0.01),
        literal(0.1), rgba(0, 2, 114, .1),
        literal(0.2), rgba(0, 6, 219, .15),
        literal(0.3), rgba(0, 74, 255, .2),
        literal(0.4), rgba(0, 202, 255, .25),
        literal(0.5), rgba(73, 255, 154, .3),
        literal(0.6), rgba(171, 255, 59, .35),
        literal(0.7), rgba(255, 197, 3, .4),
        literal(0.8), rgba(255, 82, 1, 0.7),
        literal(0.9), rgba(196, 0, 1, 0.8),
        literal(0.95), rgba(121, 0, 0, 0.8)
      )
    };
  }

  private void initHeatmapRadiusStops() {
    listOfHeatmapRadiusStops = new Expression[] {

      // 0
      interpolate(
        linear(), zoom(),
        literal(1), literal(10),
        literal(8), literal(200)
      ),
      // 1
      interpolate(
        linear(), zoom(),
        literal(1), literal(10),
        literal(8), literal(200)
      ),
      // 2
      interpolate(
        linear(), zoom(),
        literal(1), literal(10),
        literal(8), literal(200)
      ),
      // 3
      interpolate(
        linear(), zoom(),
        literal(1), literal(10),
        literal(8), literal(200)
      ),
      // 4
      interpolate(
        linear(), zoom(),
        literal(6), literal(50),
        literal(20), literal(100)
      ),
      // 5
      interpolate(
        linear(), zoom(),
        literal(12), literal(70),
        literal(20), literal(100)
      ),
      // 6
      interpolate(
        linear(), zoom(),
        literal(1), literal(7),
        literal(5), literal(50)
      ),
      // 7
      interpolate(
        linear(), zoom(),
        literal(1), literal(7),
        literal(5), literal(50)
      ),
      // 8
      interpolate(
        linear(), zoom(),
        literal(1), literal(7),
        literal(5), literal(50)
      ),
      // 9
      interpolate(
        linear(), zoom(),
        literal(1), literal(7),
        literal(15), literal(200)
      ),
      // 10
      interpolate(
        linear(), zoom(),
        literal(1), literal(10),
        literal(8), literal(70)
      ),
      // 11
      interpolate(
        linear(), zoom(),
        literal(1), literal(10),
        literal(8), literal(200)
      )
    };
  }

  private void initHeatmapIntensityStops() {
    listOfHeatmapIntensityStops = new Float[] {
      // 0
      0.25f,
      // 1
      0.8f,
      // 2
      0.25f,
      // 3
      0.5f,
      // 4
      0.6f,
      // 5
      0.3f,
      // 6
      1f,
      // 7
      1f,
      // 8
      1f,
      // 9
      1f,
      // 10
      1.5f,
      // 11
      0.8f
    };
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_satellite_heatmap_event, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == R.id.heatmap_style_switch) {
      heatmapSwitchIndex++;
      if (heatmapSwitchIndex == listOfHeatmapColors.length - 1) {
        heatmapSwitchIndex = 0;
      }
      if (mapboxMap.getLayer(HEATMAP_LAYER_ID) != null) {
        mapboxMap.getLayer(HEATMAP_LAYER_ID).setProperties(
          heatmapColor(listOfHeatmapColors[heatmapSwitchIndex]),
          heatmapRadius(listOfHeatmapRadiusStops[heatmapSwitchIndex]),
          heatmapIntensity(listOfHeatmapIntensityStops[heatmapSwitchIndex])
        );
      }
    }
    return super.onOptionsItemSelected(item);
  }

  private void initRecyclerView() {
    RecyclerView recyclerView = findViewById(R.id.rv_on_top_of_map);
    LocationRecyclerViewAdapter locationAdapter =
      new LocationRecyclerViewAdapter(createRecyclerViewLocations(), mapboxMap);
    recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext(),
      LinearLayoutManager.HORIZONTAL, true));
    recyclerView.setItemAnimator(new DefaultItemAnimator());
    recyclerView.setAdapter(locationAdapter);
    SnapHelper snapHelper = new LinearSnapHelper();
    snapHelper.attachToRecyclerView(recyclerView);
  }

  private List<SingleHighlightedEvent> createRecyclerViewLocations() {
    ArrayList<SingleHighlightedEvent> locationList = new ArrayList<>();
    for (Feature singleFeature : highlightedEventFeatureCollection.features()) {
      Point singleFeatureCoordinates = (Point) singleFeature.geometry();
      SingleHighlightedEvent singleLocation = new SingleHighlightedEvent();
      singleLocation.setName(singleFeature.getStringProperty("name"));
      singleLocation.setCoordinates(new LatLng(
        singleFeatureCoordinates.latitude(), singleFeatureCoordinates.longitude()));
      locationList.add(singleLocation);
    }
    return locationList;
  }

  // Add the mapView lifecycle to the activity's lifecycle methods
  @Override
  public void onResume() {
    super.onResume();
    mapView.onResume();
  }

  @Override
  protected void onStart() {
    super.onStart();
    mapView.onStart();
  }

  @Override
  protected void onStop() {
    super.onStop();
    mapView.onStop();
  }

  @Override
  public void onPause() {
    super.onPause();
    mapView.onPause();
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mapView.onDestroy();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }

  /**
   * POJO model class for a single location in the recyclerview
   */
  class SingleHighlightedEvent {

    private String highlightedEventName;
    private LatLng highlightedEventCoordinates;

    public String getHighlightedEventName() {
      return highlightedEventName;
    }

    public void setName(String highlightedEventName) {
      this.highlightedEventName = highlightedEventName;
    }

    public LatLng getHighlightedEventCoordinates() {
      return highlightedEventCoordinates;
    }

    public void setCoordinates(LatLng highlightedEventCoordinates) {
      this.highlightedEventCoordinates = highlightedEventCoordinates;
    }
  }

  static class LocationRecyclerViewAdapter extends
    RecyclerView.Adapter<LocationRecyclerViewAdapter.MyViewHolder> {

    private List<SingleHighlightedEvent> locationList;
    private MapboxMap mapboxMap;

    public LocationRecyclerViewAdapter(List<SingleHighlightedEvent> locationList, MapboxMap mapBoxMap) {
      this.locationList = locationList;
      this.mapboxMap = mapBoxMap;
    }

    @Override
    public LocationRecyclerViewAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      View itemView = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.rv_on_top_of_map_card, parent, false);
      return new LocationRecyclerViewAdapter.MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(LocationRecyclerViewAdapter.MyViewHolder holder, int position) {
      SingleHighlightedEvent singleRecyclerViewLocation = locationList.get(position);
      holder.name.setText(singleRecyclerViewLocation.getHighlightedEventName());
      holder.setClickListener(new ItemClickListener() {
        @Override
        public void onClick(View view, int position) {
          LatLng selectedLocationLatLng = locationList.get(position)
            .getHighlightedEventCoordinates();
          CameraPosition newCameraPosition = new CameraPosition.Builder()
            .target(selectedLocationLatLng)
            .build();
          mapboxMap.animateCamera(newCameraPosition(newCameraPosition));
        }
      });
    }

    @Override
    public int getItemCount() {
      return locationList.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
      TextView name;
      TextView numOfBeds;
      CardView singleCard;
      ItemClickListener clickListener;

      MyViewHolder(View view) {
        super(view);
        name = view.findViewById(R.id.location_title_tv);
        numOfBeds = view.findViewById(R.id.location_num_of_beds_tv);
        singleCard = view.findViewById(R.id.single_location_cardview);
        singleCard.setOnClickListener(this);
      }

      public void setClickListener(ItemClickListener itemClickListener) {
        this.clickListener = itemClickListener;
      }

      @Override
      public void onClick(View view) {
        clickListener.onClick(view, getLayoutPosition());
      }
    }
  }

  public interface ItemClickListener {
    void onClick(View view, int position);
  }
}