package gov.usgs.traveltime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;

/**
 * We need to know when to rebuild a file that has been aggregated from other files. This class
 * tests to see if any of the component files is newer than the aggregated file.
 *
 * @author Ray Buland
 */
public class FileChanged {

  /**
   * See if any of the component files has a later modified time than the aggregated file.
   *
   * @param aggFile Path string to the aggregated file
   * @param compFiles Array of path strings to the component files
   * @return True if any of the component files has changed since the aggregated file was last
   *     modified
   */
  public static boolean isChanged(String aggFile, String[] compFiles) {
    boolean changed = false;
    Path aggPath, compPath;
    FileTime aggTime, compTime;

    // Get the last modified time for the aggregated file.
    try {
      aggPath = Paths.get(aggFile);
      aggTime = Files.getLastModifiedTime(aggPath);
    } catch (IOException e) {
      System.out.println("Serialized file " + aggFile + " does not exist.");
      return true;
    }

    // Loop over the component files to see if anything has changed.
    for (int j = 0; j < compFiles.length; j++) {
      try {
        compPath = Paths.get(compFiles[j]);
        compTime = Files.getLastModifiedTime(compPath);
      } catch (IOException e) {
        e.printStackTrace();
        return false;
      }
      if (aggTime.compareTo(compTime) < 0) {
        changed = true;
        break;
      }
    }
    return changed;
  }
}
