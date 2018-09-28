package com.mapbox.mapboxandroiddemo.examples.labs;

// #-code-snippet: satellite-heatmap-event-activity full-java

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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
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
import com.mapbox.mapboxsdk.style.layers.HeatmapLayer;
import com.mapbox.mapboxsdk.style.layers.HillshadeLayer;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.RasterLayer;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.style.sources.RasterDemSource;
import com.mapbox.mapboxsdk.style.sources.RasterSource;
import com.mapbox.mapboxsdk.style.sources.Source;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import timber.log.Timber;

import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.heatmapDensity;
import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.linear;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.rgba;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.expressions.Expression.zoom;
import static com.mapbox.mapboxsdk.style.layers.Property.ICON_ANCHOR_BOTTOM;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapIntensity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapRadius;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapWeight;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.hillshadeHighlightColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.hillshadeShadowColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAnchor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.rasterOpacity;

/**
 * Use the style API to highlight different types of data.
 * In this example, parks, hotels, and attractions are displayed.
 */
public class SatelliteHeatmapEventActivity extends AppCompatActivity implements
  OnMapReadyCallback, MapboxMap.OnMapClickListener {


  private MapView mapView;
  private static final String TAG = "SatelliteHeatmapEvent";
  private static final String HIGHLIGHTED_EVENT_LAYER_ID = "HIGHLIGHTED_EVENT_LAYER_ID";
  private static final String PROPERTY_SELECTED = "selected";
  private static final String PROPERTY_EVENT_TITLE = "title";
  private static final String EVENT_GEOJSON_URL = "https://www.mapbox.com/mapbox-gl-js/assets/earthquakes.geojson";
  private static final String HEATMAP_LAYER_ID = "earthquakes-heat";
  private static final String HEATMAP_LAYER_SOURCE = "earthquakes";
  private static final String SATELLITE_RASTER_SOURCE_ID = "satellite-raster-source-id";
  private static final String SATELLITE_RASTER_LAYER_ID = "satellite-raster-layer-id";
  private static final String LAYER_ID = "hillshade-layer";
  private static final String LAYER_BELOW_ID = "waterway-river-canal-shadow";
  private static final String SOURCE_ID = "hillshade-source";
  private static final String SOURCE_URL = "mapbox://mapbox.terrain-rgb";
  private static final String HILLSHADE_HIGHLIGHT_COLOR = "#008924";
  private MapboxMap mapboxMap;
  private String HIGHLIHGTED_EVENT_GEOJSON_SOURCE_ID = "HIGHLIHGTED_EVENT_GEOJSON_SOURCE_ID";
  private GeoJsonSource source;
  private FeatureCollection highlightedEventFeatureCollection;
  private HashMap<String, View> viewMap;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

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

    mapboxMap.getLayer("water").setProperties(fillColor(Color.parseColor("#4793F0")));

    /*addHillshadeSource();
    addHillshadeLayer();*/

    for (Layer singleLayer : mapboxMap.getLayers()) {
      Log.d(TAG, "onMapReady: singleLayer id = " + singleLayer.getId());
    }

    Log.d(TAG, "onMapReady: here 1");
    new LoadHighlightedEventGeoJsonDataTask(this).execute();

    Log.d(TAG, "onMapReady: here 2");

    // Add heatmap data
    addHeatmapSource();
    Log.d(TAG, "onMapReady: 3");
    addHeatmapLayer();
    Log.d(TAG, "onMapReady: here 4");


    // Add satellite raster layer for viewing satellite photos once the camera is close enough to the map
    addSatelliteRasterSource();
    addSatelliteRasterLayer();

    /*// Animate the map camera so that it's closer to the map
    CameraPosition newCameraPosition = new CameraPosition.Builder()
      .zoom(11.047)
      .build();
    mapboxMap.animateCamera(
      CameraUpdateFactory.newCameraPosition(newCameraPosition), 2600);*/

    mapboxMap.addOnMapClickListener(this);
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
    if (mapboxMap == null) {
      return;
    }
    highlightedEventFeatureCollection = collection;
    initHighlightedEventFeatureCollection();
    initHighlightedEventLayer();
  }

  /**
   * Adds the GeoJSON source to the map
   */
  private void initHighlightedEventFeatureCollection() {
    source = new GeoJsonSource(HIGHLIHGTED_EVENT_GEOJSON_SOURCE_ID, highlightedEventFeatureCollection);
    mapboxMap.addSource(source);
  }

  /**
   * Updates the display of data on the map after the FeatureCollection has been modified
   */
  private void refreshSource() {
    if (source != null && highlightedEventFeatureCollection != null) {
      source.setGeoJson(highlightedEventFeatureCollection);
    }
  }

  /**
   * Setup a layer with Android SDK call-outs
   * <p>
   * name of the feature is used as key for the iconImage
   * </p>
   */
  private void initHighlightedEventLayer() {
    mapboxMap.addLayer(new SymbolLayer(HIGHLIGHTED_EVENT_LAYER_ID, HIGHLIHGTED_EVENT_GEOJSON_SOURCE_ID)
      .withProperties(
        /* show image with id title based on the value of the name feature property */
        iconImage("{name}"),

        /* set anchor of icon to bottom-left */
        iconAnchor(ICON_ANCHOR_BOTTOM),

        /* all info window and marker image to appear at the same time*/
        iconAllowOverlap(true),

        /* offset the info window to be above the marker */
        iconOffset(new Float[] {-2f, -25f})
      ));
    // TODO: Uncomment out code below?
    /* add a filter to show only when selected feature property is true *//*
      .withFilter(eq((get(PROPERTY_SELECTED)), literal(true))));*/
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
            .target(new LatLng(selectedFeaturePoint.latitude(), selectedFeaturePoint.longitude())) // Sets the new camera position
            .build(); // Creates a CameraPosition from the builder

          mapboxMap.animateCamera(CameraUpdateFactory
            .newCameraPosition(position), 15000);
        }
      }
    }
  }

  /**
   * Set a feature selected state.
   *
   * @param index the index of selected feature
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
   * @param index the specific Feature's index position in the FeatureCollection's list of Features.
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
    // need to store reference to views to be able to use them as hitboxes for click events.
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
          eventImageView.setImageDrawable(ContextCompat.getDrawable(activity.getApplicationContext()
            , R.drawable.heatmap_event_baseball_game));

          String name = feature.getStringProperty(PROPERTY_EVENT_TITLE);
          TextView titleTextView = highlightedEventFrameLayout.findViewById(R.id.heatmap_event_title_textview);
          titleTextView.setText(name);

          Bitmap bitmap = SymbolGenerator.generate(highlightedEventFrameLayout);
          imagesMap.put(name, bitmap);
          viewMap.put(name, highlightedEventFrameLayout);
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
   * Bitmaps can be added to the map with {@link com.mapbox.mapboxsdk.maps.MapboxMap#addImage(String, Bitmap)}
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
      mapboxMap.addSource(new GeoJsonSource(HEATMAP_LAYER_SOURCE, new URL(EVENT_GEOJSON_URL)));
    } catch (MalformedURLException malformedUrlException) {
      Timber.e(malformedUrlException, "That's not an url... ");
    }
  }

  private void addHeatmapLayer() {
    HeatmapLayer layer = new HeatmapLayer(HEATMAP_LAYER_ID, HEATMAP_LAYER_SOURCE);
    layer.setSourceLayer(HEATMAP_LAYER_SOURCE);
    layer.setProperties(

      // Color ramp for heatmap.  Domain is 0 (low) to 1 (high).
      // Begin color ramp at 0-stop with a 0-transparancy color
      // to create a blur-like effect.
      heatmapColor(
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
      ),

      // Increase the heatmap weight based on frequency and property magnitude
      heatmapWeight(
        interpolate(
          linear(), get("mag"),
          stop(0, 0),
          stop(6, 1)
        )
      ),

      // Increase the heatmap color weight weight by zoom level
      // heatmap-intensity is a multiplier on top of heatmap-weight
      heatmapIntensity(
        interpolate(
          linear(), zoom(),
          stop(0, 1),
          stop(9, 3)
        )
      ),

      // Adjust the heatmap radius by zoom level
      heatmapRadius(
        interpolate(
          linear(), zoom(),
          stop(0, 2),
          stop(9, 20)
        )
      )
    );

    if (mapboxMap.getLayer("waterway-label") == null) {
      mapboxMap.addLayer(layer);
    } else {
      mapboxMap.addLayerAbove(layer, "waterway-label");
    }
  }

  private void addSatelliteRasterSource() {
    // Create a data source for the satellite raster images
    Source satelliteRasterSource = new RasterSource(SATELLITE_RASTER_SOURCE_ID,
      "mapbox://mapbox.satellite", 512);

    // Add the source to the map
    mapboxMap.addSource(satelliteRasterSource);
  }

  private void addSatelliteRasterLayer() {
    // Create a new map layer for the satellite raster images
    RasterLayer satelliteRasterLayer = new RasterLayer(SATELLITE_RASTER_LAYER_ID,
      SATELLITE_RASTER_SOURCE_ID);

    // Use runtime styling to adjust the satellite layer's opacity based on the map camera's zoom level
    satelliteRasterLayer.withProperties(
      rasterOpacity(interpolate(linear(), zoom(),
        stop(13.5, 0),
        stop(16.5, 1)
      ))
    );

    // Add the satellite layer to the map
    mapboxMap.addLayer(satelliteRasterLayer);
  }

  private void addHillshadeSource() {
    // Add hillshade data source to map
    RasterDemSource rasterDemSource = new RasterDemSource(SOURCE_ID, SOURCE_URL);
    mapboxMap.addSource(rasterDemSource);
  }
  private void addHillshadeLayer() {


    // Create and style a hillshade layer to add to the map
    HillshadeLayer hillshadeLayer = new HillshadeLayer(LAYER_ID, SOURCE_ID).withProperties(
      hillshadeHighlightColor(Color.parseColor(HILLSHADE_HIGHLIGHT_COLOR)),
      hillshadeShadowColor(Color.BLACK)
    );

    // Add the hillshade layer to the map
    mapboxMap.addLayerBelow(hillshadeLayer, LAYER_BELOW_ID);
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
}
// #-code-snippet: satellite-heatmap-event-activity full-java
