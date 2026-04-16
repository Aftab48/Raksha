# TFLite Model Setup

Place your YAMNet-based distress detection model here as:
  `yamnet_distress.tflite`

## How to get the model

Option A — Use YAMNet directly from TensorFlow Hub:
1. Download YAMNet from: https://tfhub.dev/google/yamnet/1
2. Convert to TFLite format
3. Fine-tune on distress sound dataset (screaming, crying, shouting for help)
4. Place the `.tflite` file in this `assets/` folder

Option B — Use a pre-converted TFLite YAMNet:
```
wget https://storage.googleapis.com/download.tensorflow.org/models/tflite/task_library/audio_classification/android/yamnet_audio_classification.tflite
mv yamnet_audio_classification.tflite yamnet_distress.tflite
```

Note: Without fine-tuning, the base YAMNet model will classify general audio events.
The distress class indices used in AudioThreatDetector.kt (73, 74, 80, 81, 82) correspond
to screaming/crying/shouting in the YAMNet ontology.

The `aaptOptions { noCompress += "tflite" }` in build.gradle.kts ensures the model
is not compressed during packaging so TFLite can memory-map it directly.
