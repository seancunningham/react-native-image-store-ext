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
import com.facebook.react.bridge.Promise;

import android.database.Cursor;
import android.net.Uri;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.graphics.BitmapFactory;
import android.content.Context;
import android.annotation.TargetApi;
import android.os.Build;
import android.provider.DocumentsContract;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.StringBuilder;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedInputStream;

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

  @ReactMethod
  public void checkAndFixOrientation(String uri_string, Promise promise){
    ExifInterface exif = null;
      InputStream is = null;
        String path = null;
      try {
      Uri uri = Uri.parse(uri_string);

      String res = "";
      String[] proj = { MediaStore.Images.Media.DATA };
      Cursor cursor = this.reactContext.getContentResolver().query(uri, proj, null, null, null);
      if (cursor != null) {
          if (cursor.moveToFirst()) {
              int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
              res = cursor.getString(column_index);
          }
          cursor.close();
      } else {
          path = uri.getPath();
      }
      path = res;
      exif = new ExifInterface(path);
    } catch (IOException e) {
      promise.reject(e.getMessage());
    }
    int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
    System.out.println("bitmap uri " + uri_string + " " + rotation);

    FileOutputStream outStream = null;
    if (rotation != 0f){
      try {
        int rotationInDegrees = exifToDegrees(rotation);
        Matrix matrix = new Matrix();
        Bitmap original = BitmapFactory.decodeFile(path);
        matrix.preRotate(rotationInDegrees);

        Bitmap rotated = Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), matrix, true);
        File outputDir = this.reactContext.getCacheDir();
        String filename = uri_string.substring(uri_string.lastIndexOf("/")+1);
        File outputFile = File.createTempFile(filename, ".jpg", outputDir);
        outStream = new FileOutputStream(outputFile);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        rotated.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        outStream.write(byteArray);
        outStream.close();
        String outpath = Uri.fromFile(outputFile).toString();
        copyExif(path, outputFile.getAbsolutePath());
        promise.resolve(outpath);
      } catch (IOException e) {
        try {
          if(outStream != null){
            outStream.close();
          }
        } catch (IOException a) {

        }
        promise.reject(e.getMessage());
      }
    }else{
        String filename = uri_string;
        promise.resolve(filename);
    }

}

  public static void copyExif(String oldPath, String newPath) throws IOException {
    ExifInterface oldExif = new ExifInterface(oldPath);

    String[] attributes = new String[]
    {
            ExifInterface.TAG_APERTURE,
            ExifInterface.TAG_DATETIME,
            ExifInterface.TAG_DATETIME_DIGITIZED,
            ExifInterface.TAG_EXPOSURE_TIME,
            ExifInterface.TAG_FLASH,
            ExifInterface.TAG_FOCAL_LENGTH,
            ExifInterface.TAG_GPS_ALTITUDE,
            ExifInterface.TAG_GPS_ALTITUDE_REF,
            ExifInterface.TAG_GPS_DATESTAMP,
            ExifInterface.TAG_GPS_LATITUDE,
            ExifInterface.TAG_GPS_LATITUDE_REF,
            ExifInterface.TAG_GPS_LONGITUDE,
            ExifInterface.TAG_GPS_LONGITUDE_REF,
            ExifInterface.TAG_GPS_PROCESSING_METHOD,
            ExifInterface.TAG_GPS_TIMESTAMP,
            ExifInterface.TAG_IMAGE_LENGTH,
            ExifInterface.TAG_IMAGE_WIDTH,
            ExifInterface.TAG_ISO,
            ExifInterface.TAG_MAKE,
            ExifInterface.TAG_MODEL,
            ExifInterface.TAG_SUBSEC_TIME,
            ExifInterface.TAG_SUBSEC_TIME_DIG,
            ExifInterface.TAG_SUBSEC_TIME_ORIG,
            ExifInterface.TAG_WHITE_BALANCE
    };

    ExifInterface newExif = new ExifInterface(newPath);
    for (int i = 0; i < attributes.length; i++)
    {
        String value = oldExif.getAttribute(attributes[i]);
        if (value != null)
            newExif.setAttribute(attributes[i], value);
    }
    newExif.saveAttributes();
  }

  private static int exifToDegrees(int exifOrientation) {
    if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
    return 90;
    }
    else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
    return 180; }
    else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
    return 270; }
    return 0;
  }

  /**
  * Gets the real path from file
  * @param context
  * @param contentUri
  * @return path
  */
  public static String getRealPathFromURI(Context context, Uri contentUri) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
          return getPathForV19AndUp(context, contentUri);
      } else {
          return getPathForPreV19(context, contentUri);
      }
  }

  /**
  * Handles pre V19 uri's
  * @param context
  * @param contentUri
  * @return
  */
  public static String getPathForPreV19(Context context, Uri contentUri) {
      String res = null;

      String[] proj = { MediaStore.Images.Media.DATA };
      Cursor cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
      if(cursor.moveToFirst()){;
          int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
          res = cursor.getString(column_index);
      }
      cursor.close();

      return res;
  }

  /**
  * Handles V19 and up uri's
  * @param context
  * @param contentUri
  * @return path
  */
  @TargetApi(Build.VERSION_CODES.KITKAT)
  public static String getPathForV19AndUp(Context context, Uri contentUri) {
      String wholeID = DocumentsContract.getDocumentId(contentUri);

      // Split at colon, use second item in the array
      String id = wholeID.split(":")[1];
      String[] column = { MediaStore.Images.Media.DATA };

      // where id is equal to
      String sel = MediaStore.Images.Media._ID + "=?";
      Cursor cursor = context.getContentResolver().
              query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                      column, sel, new String[]{ id }, null);

      String filePath = "";
      int columnIndex = cursor.getColumnIndex(column[0]);
      if (cursor.moveToFirst()) {
          filePath = cursor.getString(columnIndex);
      }

      cursor.close();
      return filePath;
}
}
