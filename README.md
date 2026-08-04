# GneissTools

A field toolkit for structural geology and hydrogeology, shipped as a single Android APK.
Black-and-white, publication-ready graphics; everything works offline except the map tiles.

---

## Download the app

<div align="center">

### [&#11015;&#65039; DOWNLOAD THE APK &#11015;&#65039;](https://github.com/3m0ra/gneisstools/releases/latest/download/GneissTools-release.apk)

[![Download GneissTools APK](https://img.shields.io/badge/GneissTools-Download%20latest%20APK-000000?style=for-the-badge&logo=android&logoColor=white)](https://github.com/3m0ra/gneisstools/releases/latest/download/GneissTools-release.apk)

**One tap. No account, no store, no build tools.**

</div>

### Install in three steps

| | Step | What to do |
|---|---|---|
| 1 | **Download** | Open the button above **on your Android phone**. The file is *GneissTools-release.apk*. |
| 2 | **Allow the install** | Android will warn about an unknown source. Choose **Settings**, enable **Allow from this source**, then go back. |
| 3 | **Open GneissTools** | Grant **location** when asked, and allow the **camera** the first time you store a photo sample. |

That link always points at the newest build. If the download does not start, use the alternatives:

- **All builds and release notes:** [Releases](https://github.com/3m0ra/gneisstools/releases/latest)
- **Debug variant** (verbose, for troubleshooting): [GneissTools-debug.apk](https://github.com/3m0ra/gneisstools/releases/latest/download/GneissTools-debug.apk)
- **Per-commit artifacts:** [Actions](https://github.com/3m0ra/gneisstools/actions) &rarr; latest *Build APK* run &rarr; **Artifacts** &rarr; *GneissTools-apk*

Both APKs are signed with the Android debug key, so they install straight away but cannot be published on the Play Store. To distribute your own build, create a keystore and replace the signingConfig in app/build.gradle.kts.

---

## Projects

Every field project owns its own dataset: measurements, extracted sets, structural lines and photo samples are stored separately and nothing is mixed between projects. A circular project carousel sits on the home screen between the title block and the instruments: swipe it with an inertial, snapping scroll and tap a disc to switch project. The centred disc is drawn full size while its neighbours sit slightly behind it, turned away and smaller, and the last disc, marked with a plus, opens the field where a new project is named. A slider in General settings scales every text of the application at once, titles and paragraphs keeping their proportion. Projects can also be created, opened and renamed from the first entry of the main menu; the active project is shown on the home screen and printed in the header of the A4 report.

## What is inside

The app opens on a main menu and never buries a function more than two taps deep.

    Main menu
    |-- 01 Projects                  Create, choose and rename a field project
    |-- 02 Tools
    |     |-- 01 Stereonet          Measure / Plot / Sets
    |     |-- 02 Field map          GeoCover annotation
    |-- 03 Data                     Table / Photos / Traces
    |-- 04 General settings         Projection, declination, theme, text size, sensors
    |-- 05 Tutorials                Five guided workflows
    |-- 06 About                    Build, data sources, conventions

### Tool 01 - Stereonet

- Lay the phone on the surface and record a **plane** (dip / dip direction) or a **lineation** (plunge / trend).
- **Freeze** captures the reading on the rock so you can read the screen comfortably; **Average 2 s** takes a windowed mean and stores the angular spread as a quality figure.
- Manual entry, per-station site, structure type and note, optional GPS tagging of every reading.
- Lower-hemisphere plots: equal-area (Schmidt) or equal-angle (Wulff), poles, great circles, mean planes, small circles.
- **Three density estimators**, cycled by tapping the stereonet itself: Schmidt counting circle (1 per cent of the hemisphere area), Kamb counting circle and a Fisher kernel. The name of the active method flashes on the plate and is written into the caption.
- **Kamb counting-circle density** (Kamb, 1959): the counting area is set so that the expected count of a uniform distribution equals three standard deviations, cos(alpha) = 1 - 9 / (9 + n). Contours are drawn at 20 / 40 / 60 / 80 per cent of the maximum and the counting half-angle, the sample size and the distance of the maximum from a uniform count are printed in the caption.
- Density is filled with the **viridis ramp by default**, with a scale bar in per cent of the maximum. One button in General settings switches the whole density rendering back to hairline black and white.
- Strike rose and dip histogram, both in the same hairline black-and-white idiom.
- **Set extraction** by axial k-means, number of sets chosen automatically by silhouette score, with Fisher k, the 95 per cent confidence cone and the intersection line of every set pair.
- One-tap PNG export of the whole plate on white ground.
- **Undo** for the last action, including the last recorded plane, on the measure screen, in the data table and on the map.
- **One-page A4 report**, laid out automatically: header, stereonet with density, strike rose, dip histogram, a table of the extracted sets and a written summary of the results. The text is assembled on the device from the computed statistics; it describes the results only and offers no interpretation.

### Tool 02 - Field map (swisstopo GeoCover)

- Streams swisstopo **WMTS** basemaps (grey topographic, colour topographic, orthophoto) with the **GeoCover vector geological map** as an overlay, rendered monochrome by default so the sheet stays publication ready.
- **Long-press** any point to query the GeoCover polygon through api3.geo.admin.ch and read its attributes; one tap copies the unit name into the next annotation.
- **Dip symbols:** tap the outcrop, accept the live sensor reading or type the values. Drawn in standard cartographic form - strike bar, dip tick, dip value - and written into the same dataset as the stereonet.
- **Fault traces:** tap the vertices, then close the line and pick its type - fault, normal with ticks, thrust with triangles, inferred as a dashed line, or lithological contact.
- **Photo samples:** tap the sample point, take the photograph, and it is pinned to that coordinate as a square symbol. Images are held in IndexedDB on the device.
- Live GPS position with its accuracy circle, metric scale bar, a follow mode and a **centre on my position** button that recentres the view on the current fix.
- Manually drawn fault traces are **solid red**; inferred faults are dashed red, lithological contacts keep the black hairline idiom.

> Coverage note: the GeoCover geological map is a swisstopo product and exists for **Switzerland only**. Outside Switzerland the stereonet, the annotation layers and the export all keep working, only the tiles will be blank.

### Data out

| Format | Contents |
|---|---|
| **CSV** | one row per reading: dip, dip direction, strike, trend, plunge, quality, site, coordinates, note |
| **GeoJSON** | orientations, structural lines and photo points in WGS 84, ready for QGIS |
| **JSON** | full backup, re-importable |
| **PNG** | captioned stereonet plate on white ground, or the one-page A4 report |
| **JPEG** | the photo samples themselves |

Exports are written to the phone's **Downloads** folder through a native bridge.

---

## Build it yourself

Android Studio or the command-line SDK, plus JDK 17:

    ./gradlew assembleDebug

The APK lands in app/build/outputs/apk/debug/app-debug.apk.
Pushing to main also triggers the **Build APK** workflow, which republishes the rolling *latest* release used by the download button above.

## Repository layout

| Path | Role |
|---|---|
| app/src/main/assets/index.html | the entire application: UI, geology maths, map engine |
| app/src/main/java/com/geostruct/field/MainActivity.java | WebView host, file export, camera and back-button bridge |
| app/src/main/AndroidManifest.xml | sensor, location and camera declarations |
| .github/workflows/build-apk.yml | automatic build and rolling release |

The app is served from https://appassets.androidplatform.net/ through WebViewAssetLoader. That real secure origin is what makes the accelerometer, magnetometer and geolocation available; the same file opened from file:// is rejected with NotAllowedError.

To change the application, edit **index.html**, raise versionCode in app/build.gradle.kts and rebuild.

## Methods and references

Pole density is counted with the Kamb counting circle, the counting area being set for an expected count of three standard deviations. Sets are extracted by axial k-means, the number of sets being chosen by the silhouette score. Mean orientation, the concentration parameter k and the 95 per cent confidence cone follow Fisher statistics for a sphere. Terzaghi weighting is available for scanline surveys. The same statements and the reference list below are reproduced in the About screen of the application and in the footer of the A4 report.

- Kamb, W. B. (1959) Ice petrofabric observations from Blue Glacier, Washington, in relation to theory and experiment. *Journal of Geophysical Research* 64, 1891-1909.
- Fisher, R. A. (1953) Dispersion on a sphere. *Proceedings of the Royal Society A* 217, 295-305.
- Terzaghi, R. D. (1965) Sources of error in joint surveys. *Geotechnique* 15, 287-304.
- Rousseeuw, P. J. (1987) Silhouettes: a graphical aid to the interpretation and validation of cluster analysis. *Journal of Computational and Applied Mathematics* 20, 53-65.

## Conventions and sources

Planes are stored as dip and dip direction, lineations as plunge and trend, azimuths as true north after the magnetic declination correction entered in General settings. All plots use the lower hemisphere.
Basemaps and the GeoCover vector geological map are served by **swisstopo** (wmts.geo.admin.ch, api3.geo.admin.ch); their terms of use apply. Orientation data are computed on the device.

## Credits

GneissTools is developed by **3m0ra**. Every exported sheet carries the line *done with GneissTools by 3m0ra* in its footer, and the credit is repeated at the foot of the home screen.
