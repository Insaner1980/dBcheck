# YAMNet Attribution

Bundled files:

- `app/src/main/assets/sound_detection/yamnet.tflite`
  - Source: Google YAMNet TF Lite classification model, version 1.
  - Kaggle Models: https://www.kaggle.com/models/google/yamnet/tfLite/classification-tflite/1
  - Legacy TFHub URL: https://tfhub.dev/google/lite-model/yamnet/classification/tflite/1
  - SHA-256: `10C95EA3EB9A7BB4CB8BDDF6FEB023250381008177AC162CE169694D05C317DE`
  - License: Apache License 2.0.

- `app/src/main/assets/sound_detection/yamnet_class_map.csv`
  - Source: TensorFlow Models `research/audioset/yamnet/yamnet_class_map.csv`.
  - URL: https://github.com/tensorflow/models/blob/master/research/audioset/yamnet/yamnet_class_map.csv
  - SHA-256: `CDF24D193E196D9E95912A2667051AE203E92A2BA09449218CCB40EF787C6DF2`
  - License: Apache License 2.0, as provided by the TensorFlow Models repository.

Reference notes:

- The TensorFlow YAMNet tutorial describes YAMNet as an AudioSet classifier with 521 classes.
- The model expects mono waveform input at 16 kHz.
- Later classifier work must continue to avoid persisting raw audio.
