#es-scan
* * *

An experimental app for Android that reads the code row of a orange (swiss) payment slip using the device camera.

Runs the Tesseract 3.01 OCR engine using a fork of Tesseract Tools for Android.

Most of the code making up the core structure of this project has been adapted from android-ocr (Thanks to Robert Theis). Along with Tesseract-OCR and Tesseract Tools for Android (tesseract-android-tools), several open source projects have been used in this project, including jtar.

## Requires

* Installation of [tess-two](https://github.com/rmtheis/tess-two) as a library project, to act as the OCR engine.

Installing the APK
==================

There is no apk at the moment. You have to build it by yourself.

License
=======

This project is licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)

    /*
     * Copyright 2011 Robert Theis
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     *      http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */

One of the jar files in the android/libs directory is licensed under the [GNU Lesser GPL](http://www.gnu.org/licenses/lgpl.html).
