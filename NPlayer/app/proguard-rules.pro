# Keep annotations; Media3 and Hilt ship their own consumer ProGuard rules.
-keepattributes *Annotation*

# Unused code is still removed, but the remaining classes and members keep their
# real names. This trades a larger dex for readable stack traces, which matters
# because the reflection-based paths (Hilt, Media3) are the ones that break when
# names are rewritten.
-dontobfuscate
