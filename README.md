# Baby Cry Monitor (Android)

Simple Android app that:
1. User enters the phone number.
2. User activates monitoring.
3. App continuously listens for crying using a TFLite model.
4. If crying is detected with a strict threshold over multiple windows, app immediately places a **call** and sends an **SMS**.

## Important accuracy note
Zero false alarms are not realistically possible in real-world audio. This implementation minimizes false alarms using:
- high confidence threshold (`>= 0.95`)
- 3 consecutive positive windows required
- cooldown between alerts

For production-grade accuracy, train and validate your model with your home environment audio and test on target phones.

## Setup
- Add your trained TensorFlow Lite model file as:
  `app/src/main/assets/baby_cry_model.tflite`
- Build and install from Android Studio.
- Grant microphone, call, and SMS permissions.

## Limitations
- Direct `ACTION_CALL` requires runtime permission and may be impacted by OEM restrictions.
- Android background execution rules vary by device.
