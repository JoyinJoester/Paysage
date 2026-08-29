# Native lpac-jni code resolves these APIs by their exact JVM names.
-keep,includedescriptorclasses class net.typeblog.lpac_jni.** { *; }
-keep,includedescriptorclasses class * implements net.typeblog.lpac_jni.ApduInterface { *; }
-keep,includedescriptorclasses class * implements net.typeblog.lpac_jni.HttpInterface { *; }
