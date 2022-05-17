package gov.usgs.traveltime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;

/**
 * The FileChanged class manages when to rebuild a file that has been aggregated from other files.
 * This class tests to see if any of the component files is newer than the aggregated file.
 *
 * @author Ray Buland
 */
public class FileChanged {
  /**
   * Function to check if any of the component files has a later modified time than the aggregated
   * file.
   *
   * @param aggregatedFile A String containing the path to the aggregated file
   * @param componentFiles An array of strings containing the paths to the component files that have
   *     been aggregated
   * @return A boolean flag, true if any of the component files has changed since the aggregated
   *     file was last modified
   */
  public static boolean isChanged(String aggregatedFile, String[] componentFiles) {
    boolean componentsChanged = false;

    // Get the last modified time for the aggregated file.
    FileTime lastAggregatedFileUpdate;
    try {
      lastAggregatedFileUpdate = Files.getLastModifiedTime(Paths.get(aggregatedFile));
    } catch (IOException e) {
      // System.out.println("Serialized file " + aggregatedFile + " does not exist.");
      return true;
    }

    // Loop over the component files to see if anything has changed.
    for (int j = 0; j < componentFiles.length; j++) {
      FileTime lastComponentFileUpdate;
      try {
        lastComponentFileUpdate = Files.getLastModifiedTime(Paths.get(componentFiles[j]));
      } catch (IOException e) {
        e.printStackTrace();
        return false;
      }

      if (lastAggregatedFileUpdate.compareTo(lastComponentFileUpdate) < 0) {
        componentsChanged = true;
        break;
      }
    }

    return componentsChanged;
  }
}
