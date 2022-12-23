+++
+++

<div style="display: flex; flex-wrap: nowrap; justify-content: center">
<div><img style="flex-grow: 1; flex-shrink: 1" src="github/star.gif"><div style="text-align:center"><mark>Star ⭐(Translation only)</mark></div></div>
<div><img style="flex-grow: 1; flex-shrink: 1" src="github/spiral.gif"><div style="text-align:center"><mark>Spiral ➰(Translation only)</mark></div></div>
<div><img style="flex-grow: 1; flex-shrink: 1" src="github/arc.gif"><div style="text-align:center"><mark>Arc 🌙 (Translation + Rotation)</mark></div></div>
</div>

This project illustrates a real-time 2D localization algorithm for off-the-shelf mobile phone hardware.
We assume that the device is constrained to have only translation and rotation in a 2D space (ex. on a computer desk).
We assume that a static planar scene exists at a far enough distance with a decent number of good features to track over time.

The underlying idea is to estimate euclidean transform of features in nth frame with that of 1st frame.
As a result,
- When the device is at rest there is translation and rotation readings stay at zero i.e. there is no drift at rest.
- When the video feed is interrupted for some time and then restarted, tracking resumes automatically.
- The speed at which device moves has no effect on tracking as change in pose is only a function of first frame and Nth frame.
- The above point also means that there will be no drift accumulation during the movements.

It is designed to be deployed in a mobile phone with a front/back camera. A simple use case is to use it as an external mouse. A rudimentary inertial odometer using accelerometer with eularian integration is provided as a baseline. 

