[![Codacy Badge](https://api.codacy.com/project/badge/Grade/04492b4350e744c6bf2ecc3560a66a5a)](https://app.codacy.com/app/jpatton-USGS/neic-traveltime?utm_source=github.com&utm_medium=referral&utm_content=usgs/neic-traveltime&utm_campaign=badger)
[![Build Status](https://travis-ci.org/usgs/neic-traveltime.svg?branch=master)](https://travis-ci.org/usgs/neic-traveltime)
[![Documentation](https://usgs.github.io/neic-traveltime/codedocumented.svg)](https://usgs.github.io/neic-traveltime/)

# neic-traveltime
The neic-traveltime GitHub project is a direct port of the Buland and Chapman travel-time implementation from FORTRAN to Java. 

The algorithm is described in “The Computation of Seismic Travel Times”, [BSSA, 1983, vol. 72, pp 1271-1301](https://pubs.geoscienceworld.org/ssa/bssa/article/73/5/1271/118430/the-computation-of-seismic-travel-times). In short, the algorithm uses a very specific interpolation of tau as a function of ray parameter to make the computation of travel times explicit (rather than implicit as for shooting rays). Although more complex, this algorithm has many advantages over traditional travel time versus distance and depth tables, particularly in terms of precision and ease of use. The dominant approximations made in the Buland and Chapman algorithm are Earth flattening and the Mohoroviĉić and Bullen power law for slowness versus depth allowing the tau and distance integrals to be evaluated in closed form.

The [TauP](https://github.com/crotwell/TauP) code developed by Phillip Crotwell uses a similar algorithm and is also written in Java inviting comparison. The primary difference is that the Buland and Chapman implementation was built from the ground up for high volume, global scale, bulletin work. It currently produces all local, regional, and teleseismic phases determined by the [United States Geolgical Survey National Earthquake Information Center](https://earthquake.usgs.gov/contactus/golden/neic.php) to be useful (58 phases in all). Note that many of these phases are not used in event location, but associating them avoids splitting events. The added complexity needed to produce all these phases efficiently is significant and makes the process of adding and vetting new Earth models challenging. The other significant difference between the packages is that TauP uses linear interpolation for computational stability, while neic-traveltime uses a modified cubic spline interpolation to better capture triplications due to slowness gradients.

The neic-traveltime package also includes a variety of non-geometric phases, travel-time corrections, and earthquake
location metadata needed for bulletin work. In addition to diffracted phases (Pdif, Sdif, and PKPdif extending PKPbc), estimates for Lg, high frequency, regional LR, pwP (pP reflected at the surface of the ocean), and PKPpre (the PKP precursor) are provided. Travel-time corrections include elevation, ellipticity, and bounce point (correcting for the ocean depth at the bounce point for surface reflections such as pP and PP). Travel-time metadata includes phase statistics, phase group information (e.g., for dealing with generic phase reports), and usage information (e.g., usage in the location process). The
neic-traveltime package currently supports both standalone (single user) and multi-threaded server implementations.

Dependencies
------
* neic-traveltime was written in Oracle Java 1.8
* neic-traveltime is built with [Apache Ant](http://ant.apache.org/), and was
written using Eclipse and netbeans.  Netbeans project files, source files,
and an ant build.xml are included in this project

Building
------
The steps to get and build neic-traveltime.jar using ant are as follows:

1. Clone neic-traveltime.
2. Open a command window and change directories to /neic-traveltime/
3. To build the jar file, run the command `ant jar`
4. To generate javadocs, run the command `ant javadoc`
5. To compile, generate javadocs, build jar, run the command `ant all`

Using
-----
Once you are able to build the neic-traveltime jar, simply include the jar
file in your application.

Further Information and Documentation
------
For further information and documentation please check out the [neic-traveltime Documentation Site](https://usgs.github.io/neic-traveltime/).

File bug reports, feature requests and questions using [GitHub Issues](https://github.com/usgs/neic-traveltime/issues)
