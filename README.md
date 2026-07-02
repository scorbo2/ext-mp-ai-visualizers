# ext-mp-template

This is a template project that can be used as a starting point for creating and packaging a new
extension for the [MusicPlayer](https://github.com/scorbo2/musicplayer/) application.

## How do I use this template?

Copy this repository to a new directory, and then you should be able to go through it
and change it as described below to describe your new extension. Then, you can add the
code required to implement your extension's functionality.

### pom.xml

The `pom.xml` file contains the following configuration:

```xml
<groupId>ca.corbett</groupId>
<artifactId>ext-mp-template</artifactId>
<version>1.0-SNAPSHOT</version>
```

Change the `groupId` and `artifactId` to match your new extension's package and name. 
The `version` can be changed to whatever version you want to release.

### extInfo.json

Next, find `extInfo.json` in the `src/main/resources` directory. This file contains the following configuration:

```json
{
  "name": "EXTENSION_NAME_GOES_HERE",
  "author": "AUTHOR_NAME_GOES_HERE",
  "version": "4.0.0",
  "targetAppName": "MusicPlayer",
  "targetAppVersion": "4.0",
  "shortDescription": "EXTENSION_SHORT_DESCRIPTION_HERE",
  "longDescription": "EXTENSION_LONG_DESCRIPTION_HERE"
}
```

Replace the all-caps placeholders with values that describe your extension.

**The `targetAppName` and `targetAppVersion` properties must remain as-is**.
These properties are used by the MusicPlayer extension manager to determine whether your 
extension is compatible with the current version of MusicPlayer.

It is strongly recommended that your extension version should follow the `major.minor` 
of the application version that you are targeting, as described in the 
[documentation](https://www.corbett.ca/swing-extras-book/app-extensions/ExtensionVersioning.html).
This is not enforced by the code, but it makes it MUCH easier to manage extension releases if
you follow this convention. In the example above, we are targeting MusicPlayer `4.0`, and so
we number the first release of our extension `4.0.0`. If we later release a new version of our
extension, we can differentiate it by bumping the patch number, leaving the major and minor
numbers the same. For example: `4.0.1`, `4.0.2`, and so on. Minor releases of the application
are guaranteed not to break compatible extensions, so all MusicPlayer `4.x` releases will be
compatible with your extension, without recompiling/repackaging it. A major application release
will likely require recompiling/repackaging all extensions, which should then target that release version.
For example, MusicPlayer `5.0` would require a `targetAppVersion` of `5.0`, and you should
consider bumping the extension version to `5.0.0` to match it.

### Change the package name/path

The example code lives in `src/main/java/ca/corbett/musicplayer/extensions/template`. You should
change this to something that better matches your extension.

Example: `src/main/java/com/example/myextension`.

### Rename the extension classes

Two template classes are provided: `ExtensionTemplate` and `VisualizerTemplate`. You should consider
renaming these to something that better describes your extension. 
You can also remove the `VisualizerTemplate` class if your extension does not provide a visualizer.

Example: `MyAmazingExtension` and `SuperCoolVisualizer`.

### Implement extension methods

Now you can go through the two supplied template classes and modify them to implement whatever
your extension does. Javadocs are provided so you can decide which method(s) you need to provide
an implementation for.

### Optional - supply a visualizer

The `VisualizerTemplate` class provides a template for implementing a full-screen visualizer. 
If your extension does not provide a visualizer, you can delete this class.

Your extension can provide multiple visualizers! Just copy and rename the class as needed.
When your Visualizers have been implemented, visit the `getCustomVisualizers()` method in the
`ExtensionTemplate` class and add an instance of each of your visualizers to the returned list.
This will register them with the application so that the user can select them.

### Package your extension.

Running `mvn package` with the supplied `pom.xml` will package your extension into a thin jar
file. Note that the MusicPlayer dependency is listed as `<scope>provided</scope>` in the `pom.xml` file,
so the resulting jar will only contain your extension code, not the application code. This is fine.
Your jar can be copied to the extensions directory of any compatible MusicPlayer installation,
and the application will load it on startup. Your extension jar can also be published to a website
for automatic download via the ExtensionManager dialog, but that is beyond the scope of this document.
Refer to the [ExtPackager](https://github.com/scorbo2/ext-packager) application if you wish to
package your extension for automatic download and installation via the MusicPlayer ExtensionManager dialog.
