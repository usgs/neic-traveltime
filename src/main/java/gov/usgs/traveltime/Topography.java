package gov.usgs.traveltime;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * The Topography class handles corrections for reflections from the free surface. These may be
 * either positive (i.e., reflecting under a mountain) or negative (i.e., reflecting from the bottom
 * of the ocean). The pwP time (relative to pP) is also computed.
 *
 * @author Ray Buland
 */
public class Topography implements Serializable {
  /** A long containing the version id used in serialization */
  private static final long serialVersionUID = 1L;

  /**
   * A two dimensional array of shorts containing the global topography on a 20" grid in kilometers
   */
  private short[][] topographyGrid;

  /** A TopographicLongitudes object holding the virtual array of longitude sample points */
  private TopographicLongitudes topographicLongitudes;

  /** A TopographicLatitudes object holding the virtual array of latitude sample points */
  private TopographicLatitudes topographicLatitudes;

  /**
   * The Topography constructor reads in the topography file.
   *
   * @param topographyFilePath A String containing the path to the topography file
   * @throws IOException On any I/O error or data mismatch
   */
  public Topography(String topographyFilePath) throws IOException {
    // Read the topography data.
    readTopographyFile(topographyFilePath);

    // Set up the virtual arrays of latitude and longitude sample points.
    topographicLongitudes = new TopographicLongitudes();
    topographicLatitudes = new TopographicLatitudes();
  }

  /**
   * Function to read the topography file.
   *
   * @param topographyFilePath A String containing the path to the topography file
   * @throws IOException On any I/O error or data mismatch
   */
  private void readTopographyFile(String topographyFilePath) throws IOException {
    @SuppressWarnings("unused")
    int dataCount, numLongitudes, numLatitudes;
    @SuppressWarnings("unused")
    double dLatdLon,
        minimumLongitude,
        longitudeStep,
        maximumLongitude,
        minimumLatitude,
        latitudeStep,
        maximumLatitude;

    // Set up the byte buffer.
    byte[] byteArray = new byte[2164];
    ByteBuffer byteBuf = ByteBuffer.wrap(byteArray);
    byteBuf.order(ByteOrder.LITTLE_ENDIAN);
    ShortBuffer shorts = byteBuf.asShortBuffer();

    // Open the topo file.
    BufferedInputStream in = new BufferedInputStream(new FileInputStream(topographyFilePath));

    // Read the record header.
    int bytesRead = in.read(byteArray, 0, 4);

    int recLen = 0;
    if (bytesRead == 4) {
      recLen = byteBuf.getInt();
    } else {
      System.out.println("Unable to read header record length.");
      in.close();
      throw new IOException();
    }

    // Read the header record.
    byteBuf.clear();
    bytesRead = in.read(byteArray, 0, recLen + 4);

    if (bytesRead >= recLen + 4) {
      dataCount = byteBuf.getInt();
      numLongitudes = byteBuf.getInt();
      numLatitudes = byteBuf.getInt();
      dLatdLon = byteBuf.getFloat();
      minimumLongitude = byteBuf.getFloat();
      longitudeStep = byteBuf.getFloat();
      maximumLongitude = byteBuf.getFloat();
      minimumLatitude = byteBuf.getFloat();
      latitudeStep = byteBuf.getFloat();
      maximumLatitude = byteBuf.getFloat();

      //		System.out.format("Dims: %4d %4d %4d rat: %4.1f X: %9.4f %6.4f "+
      //				"%8.4f X: %8.4f %6.4f %7.4f\n", dataCount, numLongitudes, numLatitudes, dLatdLon,
      //				minimumLongitude, longitudeStep, maximumLongitude, minimumLatitude, latitudeStep,
      // maximumLatitude);
      // Check the record length.
      int recLast = byteBuf.getInt();

      if (recLast != recLen) {
        System.out.println("Header record length mismatch.");
        in.close();
        throw new IOException();
      }
    } else {
      System.out.println("Insufficient data for header record.");
      in.close();
      throw new IOException();
    }

    // Allocate the topography storage.  Make the longitude two bigger
    // to accommodate the wrap around at +/-180 degrees.
    topographyGrid = new short[numLongitudes + 2][numLatitudes];

    // Loop over the latitudes.
    for (int j = 0; j < numLatitudes; j++) {
      // Get the record length.
      byteBuf.clear();
      bytesRead = in.read(byteArray, 0, 4);

      if (bytesRead == 4) {
        recLen = byteBuf.getInt();
      } else {
        System.out.println("Unable to read data record " + j + " length.");
        in.close();
        throw new IOException();
      }

      // Read the data record.
      byteBuf.clear();
      bytesRead = in.read(byteArray, 0, recLen + 4);

      if (bytesRead == recLen + 4 && recLen == numLongitudes * 2) {
        // Transfer the data.
        for (int i = 0; i < numLongitudes; i++) {
          topographyGrid[i + 1][j] = shorts.get(i);
        }

        // Handle the wrap around.
        topographyGrid[0][j] = topographyGrid[numLongitudes][j];
        topographyGrid[numLongitudes + 1][j] = topographyGrid[1][j];

        //		if(j%100 == 0) System.out.format("Rec %4d: %5d %5d\n",
        //				j, topographyGrid[1][j], topographyGrid[numLongitudes][j]);
        // Check the record length.
        byteBuf.position(recLen);
        int recLast = byteBuf.getInt();

        if (recLast != recLen) {
          System.out.println("Data record " + j + " length mismatch.");
          in.close();
          throw new IOException();
        }
      } else {
        System.out.println("Insufficient data for data record " + j + ".");
        in.close();
        throw new IOException();
      }
    }

    // Close the topography file.
    in.close();
  }

  /**
   * Function to get the elevation at any point on Earth. Note that under the ocean, this will be
   * minus the ocean depth.
   *
   * @param latitude A double containing the geographic latitude in degrees
   * @param longitude A double containing the longitude in degrees
   * @return A double containing the elevation in kilometers
   */
  public double getElevation(double latitude, double longitude) {
    // The data is stored as meters of elevation in short integers.
    return 0.001d
        * TauUtilities.biLinearInterpolation(
            longitude, latitude, topographicLongitudes, topographicLatitudes, topographyGrid);
  }
}
