package dev.sekt.react_native_image_store_ext;

/**
 * Created by sekt on 2016-05-18.
 */
 
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

public class ImageStoreExt extends ReactContextBaseJavaModule {
  final ReactApplicationContext reactContext;
  public ImageStoreExt(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    return;
  }
  
  @Override
  public String getName() {
    return "ImageStoreExt";
  }
  
  @ReactMethod
  public void removeImageForTag(String file_uri){
    File file = null;
    try {
      file = new File(new URI(file_uri));
      file.delete();
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
  }
}
