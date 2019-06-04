ROOT:=		../../..
COMMON_DIR:=	$(ROOT)/lightcrafts
include		$(COMMON_DIR)/mk/platform.mk

HIGH_PERFORMANCE:=	1
ifeq ($(PLATFORM),MacOSX)
  USE_ICC_HERE:=	1
endif

TARGET_BASE:=		LCTIFF

# Uncomment to compile in debug mode.
#DEBUG:=		true

JNI_EXTRA_INCLUDES:=	$(shell $(PKGCFG) --cflags libtiff-4)
JNI_EXTRA_LINK:=	$(shell $(PKGCFG) --libs-only-l libtiff-4)
JNI_EXTRA_LDFLAGS:=	$(shell $(PKGCFG) --libs-only-L libtiff-4)
JNI_WINDOWS_LINK:=	-Wl,-Bdynamic -lLCJNI -Wl,-Bstatic -lstdc++
JNI_LINUX_LINK:=	-lLCJNI -lstdc++
JNI_MACOSX_LINK:=	../jniutils/libLCJNI.a
JNI_MACOSX_INCLUDES:=	-I/usr/local/include
JNI_MACOSX_LDFLAGS:=	-L/usr/local/lib

JNI_EXTRA_DISTCLEAN:=	autom4te.cache configure config.* tif_config.h

JAVAH_CLASSES:=		com.lightcrafts.image.libs.LCTIFFCommon \
			com.lightcrafts.image.libs.LCTIFFReader \
			com.lightcrafts.image.libs.LCTIFFWriter

include			../jni.mk

# vim:set noet sw=8 ts=8:
