/*
 *  This software is in the public domain because it contains materials 
 *  that originally came from the United States Geological Survey, 
 *  an agency of the United States Department of Interior. For more 
 *  information, see the official USGS copyright policy at 
 *  http://www.usgs.gov/visual-id/credit_usgs.html#copyright
 */
package gov.usgs.traveltime;


import java.io.File;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.PrintStream;

/** Static classes related to properties.
 *
 * @author U.S. Geological Survey  &lt;ketchum at usgs.gov&gt;
 */
public class CWBProperties {

  public static final String ps = System.getProperty("path.separator");
  public static final String fs = System.getProperty("file.separator");
  public static final String PS = System.getProperty("path.separator");
  public static final String FS = System.getProperty("file.separator");
  public static final String homedir
          = System.getProperty("user.home").substring(0, System.getProperty("user.home").
                  lastIndexOf(System.getProperty("file.separator")) + 1);
  public static final String OS = System.getProperty("os.name");
  public static final String userdir = System.getProperty("user.dir");
  public static final String userhome = System.getProperty("user.home");
  public static final String username = System.getProperty("user.name");

  public static String propfilename;      // Propery file last used by loadProperties with "user.home/" in front
  //public static String propertyFile;
  private static final Properties defprops = new Properties();
  private static final Properties prop = new Properties(defprops);

  /**
   * add a default property
   *
   * @param tag The key of the property
   * @param value The value of the default property
   */
  public static void addDefaultProperty(String tag, String value) {
    defprops.setProperty(tag, value);
  }

  public static String getPropertyFilename() {
    return propfilename;
  }

  /**
   * return the properties
   *
   * @return The Properties
   */
  public static Properties getProperties() {
    return prop;
  }

  /**
   * set a property pere key value pair
   *
   * @param tag The key in the property
   * @param val The value to set it to
   */
  public static void setProperty(String tag, String val) {

    prop.setProperty(tag, val);
  }

  /**
   * Because this is run before the Util.prt out variable is set, it cannot use prt(). Load the
   * properties from a file. First the file is check to see if it exists in user.home and then in
   * user.home/Properties.
   *
   * @param filename The file to load from
   */
  public static void loadProperties(String filename) {

    if (filename.equals("")) {
      return;
    }
    propfilename = userhome + FS + filename;
    File f = new File(propfilename);
    if (f.exists()) {
      //loadProperties(propfilename);
    } else {
      propfilename = userhome + FS + "Properties" + FS + filename;
      f = new File(propfilename);
      if (f.exists()) {
        //loadProperties(propfilename);
      }
    }

    try {
      try (FileInputStream i = new FileInputStream(propfilename)) {
        prop.load(i);
        //prtProperties();
      }
    } catch (FileNotFoundException e) {
      System.out.println(" ** Properties file not found=" + propfilename + " userhome=" + userhome + " " + System.getProperty("user.home"));
      saveProperties();
    } catch (IOException e) {
      System.out.println("IOException reading properties file=" + propfilename);
      System.exit(1);
    }
  }

  /**
   * print out the current Property pairs
   */
  public static void prtProperties() {
    prop.list(System.out);
  }

  /**
   * print out the current Property pairs
   *
   * @param out2 A print stream to print it on
   */
  public static void prtProperties(PrintStream out2) {
    prop.list(System.out);
  }

  /**
   * return the value of a given property
   *
   * @param key The name/key of the property
   * @return the value associated with the key
   */
  public static String getProperty(String key) {
    if (prop == null) {
      return null;
    }
    return prop.getProperty(key);
  }

  /**
   * save the properties to a file
   */
  public static void saveProperties() {
    //if(debug_flag) 
    //System.out.println(Util.asctime() + " Saving properties to " + propfilename + " nkeys=" + prop.size());
    try {
      try (FileOutputStream o = new FileOutputStream(propfilename)) {
        prop.store(o, propfilename + " via Util.saveProperties() cp="
                + System.getProperties().getProperty("java.class.path"));
      }
    } catch (FileNotFoundException e) {
      System.out.println("Could not write properties to " + propfilename);
      System.exit(1);
    } catch (IOException e) {
      System.out.println("Write error on properties to " + propfilename);
      System.exit(1);
    }
  }
}
