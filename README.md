# hawkeye

### references
- <https://www.sciencedirect.com/science/article/abs/pii/S0263224104000417>
- <https://www.sciencedirect.com/science/article/pii/S1474667016426261>
- <http://nghiaho.com/?page_id=671>

## code
- This is a typical android project written in `Kotlin`.
- `opencv` library is used for feature extraction, description and matching.
- `koma` library is used for linear algebra.
- `proposal.pdf` contains the baseline algorithm.
- `report.pdf` contains all the details of the project viz. assumptions, algorithms, analysis ...

## usage
- Build the app using android studio.
- Install the app in a device with front camera.
- Give camera permission for the app from the device settings.
- Place the device on a 2D plane facing a static ceiling and reset the visual odometer.
- On moving the device now, it uses the video feed to track its 2D pose and visualizes it on screen.

## demonstration
| | |
| --- | --- |
| If device at rest no translation or rotation is detected | ![](./github/at-rest.gif) |
| Drawing a line by hand. (Translation only) | ![](./github/line.gif) |
| Drawing a square by hand. (Translation only) | ![](./github/square.gif) |
| Drawing a star by hand. (Translation only) | ![](./github/star.gif) |
| Drawing a circle by hand. (Translation only) | ![](./github/circle.gif) |
| Drawing a spiral by hand. (Translation only) | ![](./github/spiral.gif) |
| Drawing a word (CV) by hand. (Translation only) | ![](./github/cv.gif) |
| Rotating 90 degrees by hand. (Rotation only) | ![](./github/90-rot.gif) |
| Drawing an arc by hand. (Translation + Rotation) | ![](./github/arc.gif) |
| The device is occuluded rotated by 90 degrees. On unocclusion orientation is immediately estimated to be at 90 degrees. | ![](./github/occlusion.gif) |
