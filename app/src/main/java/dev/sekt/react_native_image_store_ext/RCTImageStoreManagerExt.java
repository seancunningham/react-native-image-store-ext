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
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.StringBuilder;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;

public class RCTImageStoreManagerExt extends ReactContextBaseJavaModule {
  final ReactApplicationContext reactContext;
  public RCTImageStoreManagerExt(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    return;
  }
  
  @Override
  public String getName() {
    return "RCTImageStoreManagerExt";
  }

  @ReactMethod
  public Boolean removeExifMetadata(String input_file_uri, String out_file_uri) throws IOException, URISyntaxException, ImageReadException, ImageWriteException{
    try{
      File in_file = new File(new URI(input_file_uri));
      File out_file = new File(new URI(out_file_uri));  
      _removeExifMetadata(in_file, out_file);
      removeImageForTag(input_file_uri);
      in_file.delete();
      return true;
    } catch (URISyntaxException e) {
      return false;
    } 
  }

  private void _removeExifMetadata(final File jpegImageFile, final File dst)
    throws IOException, ImageReadException, ImageWriteException {
      try (FileOutputStream fos = new FileOutputStream(dst); OutputStream os = new BufferedOutputStream(fos)) {
        new ExifRewriter().removeExifMetadata(jpegImageFile, os);
      }
    }
  
  @ReactMethod
  public Boolean removeImageForTag(String file_uri){
    File file = null;
    try {
      file = new File(new URI(file_uri));
      file.delete();
      return true;
    } catch (URISyntaxException e) {
      e.printStackTrace();
      return false;
    }
  }
}
