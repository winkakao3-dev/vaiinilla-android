# Vaiinilla release shrinking policy
#
# R8 consumes the rules published by AndroidX, Hilt, Firebase, CameraX, ML Kit,
# and Kotlin serialization from their dependencies. Keep this file intentionally
# narrow: do not add blanket -dontwarn or application-wide keep rules. If a
# release-only reflective entry point is added later, document and test the
# smallest rule for that entry point here.
