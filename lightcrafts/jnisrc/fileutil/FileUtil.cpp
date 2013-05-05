/* Copyright (C) 2005-2011 Fabio Riccardi */

/**
 * FileUtil
 * 
 * Paul J. Lucas [paul@lightcrafts.com]
 */

// standard
#include <jni.h>
#define _POSIX_C_SOURCE                 /* makes stat have the right fields */
#include <sys/stat.h>                   /* for stat(2) */

#ifdef DEBUG
#include <iostream>
#endif

// local
#include "LC_CPPUtils.h"
#include "LC_JNIUtils.h"
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_utils_file_FileUtil.h"
#endif

using namespace std;
using namespace LightCrafts;

////////// JNI ////////////////////////////////////////////////////////////////

#define FileUtil_METHOD(method) \
        name4(Java_,com_lightcrafts_utils_file_FileUtil,_,method)

/**
 * Get the last access time of a file.
 */
JNIEXPORT jlong JNICALL FileUtil_METHOD(getLastAccessTime)
    ( JNIEnv *env, jclass, jbyteArray jFileName )
{
    jbyteArray_to_c const cFileName( env, jFileName );
    struct stat s;
    if ( ::stat( cFileName, &s ) == -1 ) {
        LC_throwIOException( env, cFileName );
        return 0;
    }
    return s.st_atime;
}

/* vim:set et sw=4 ts=4: */
