/*
 *  lcmsjnilib.c.c
 *
 *
 *  Created by Fabio Riccardi on 7/21/06.
 *  Copyright 2006 Light Crafts, Inc.. All rights reserved.
 *
 */
#include <ctype.h>

#ifndef AUTO_DEP
#include "javah/com_lightcrafts_utils_LCMS.h"
#endif

#include "LC_JNIUtils.h"
#include "lcms2.h"

#define DCRaw_METHOD(method) \
name4(Java_,com_lightcrafts_utils_LCMS,_,method)

JNIEXPORT jlong JNICALL Java_com_lightcrafts_utils_LCMS_cmsOpenProfileFromMem
  (JNIEnv *env, jclass clazz, jbyteArray jdata, jint size)
{
    char *data = (char *) (*env)->GetPrimitiveArrayCritical(env, jdata, 0);

    cmsHPROFILE result = cmsOpenProfileFromMem(data, size);

    (*env)->ReleasePrimitiveArrayCritical(env, jdata, data, 0);

    return (jlong) result;
}

JNIEXPORT jlong JNICALL Java_com_lightcrafts_utils_LCMS_cmsCreateRGBProfile
  (JNIEnv *env, jclass clazz, jdoubleArray jWhitePoint, jdoubleArray jPrimaries, jdouble gamma)
{
      double *WhitePoint = (double *) (*env)->GetPrimitiveArrayCritical(env, jWhitePoint, 0);
      double *Primaries = (double *) (*env)->GetPrimitiveArrayCritical(env, jPrimaries, 0);
      int i;
      cmsHPROFILE result;

      cmsCIExyY w = { WhitePoint[0], WhitePoint[1], WhitePoint[2] };
      
      cmsCIExyYTRIPLE p = {
        {Primaries[0], Primaries[1], Primaries[2]},
        {Primaries[3], Primaries[4], Primaries[5]},
        {Primaries[6], Primaries[7], Primaries[8]}
      };

      cmsToneCurve* gammaTable[3];
      const int context = 1337;

      gammaTable[0] = gammaTable[1] = gammaTable[2] = cmsBuildGamma(gamma == 1 ? &context : 0, gamma);

      result = cmsCreateRGBProfile(&w, &p, gammaTable);
      
      // _cmsSaveProfile( result, "/Stuff/matrixRGB.icc" );
      
      // cmsFreeToneCurve(gammaTable[0]);
      
      (*env)->ReleasePrimitiveArrayCritical(env, jWhitePoint, WhitePoint, 0);
      (*env)->ReleasePrimitiveArrayCritical(env, jPrimaries, Primaries, 0);

      return (jlong) result;
}

JNIEXPORT jlong JNICALL Java_com_lightcrafts_utils_LCMS_cmsCreateLab2Profile
  (JNIEnv *env, jclass clazz)
{
    return (jlong) cmsCreateLab2Profile(NULL);
}

JNIEXPORT jboolean JNICALL Java_com_lightcrafts_utils_LCMS_cmsCloseProfile
  (JNIEnv *env, jclass clazz, jlong jhProfile)
{
    cmsHPROFILE hProfile = (cmsHPROFILE) jhProfile;
    return cmsCloseProfile(hProfile);
}

JNIEXPORT jlong JNICALL Java_com_lightcrafts_utils_LCMS_cmsCreateTransform
  (JNIEnv *env, jclass clazz, jlong inputProfile, jint inputFormat,
   jlong outputProfile, jint outputFormat, jint intent, jint flags)
{
    return (jlong) cmsCreateTransform((cmsHPROFILE) inputProfile, inputFormat,
                                      (cmsHPROFILE) outputProfile, outputFormat,
                                      intent, flags);
}

JNIEXPORT jlong JNICALL Java_com_lightcrafts_utils_LCMS_cmsCreateProofingTransform
  (JNIEnv *env, jclass clazz, jlong inputProfile, jint inputFormat,
   jlong outputProfile, jint outputFormat, jlong proofingProfile,
   jint intent, jint proofingIntent, jint flags)
{
    return (jlong) cmsCreateProofingTransform((cmsHPROFILE) inputProfile, inputFormat,
					      (cmsHPROFILE) outputProfile, outputFormat,
					      (cmsHPROFILE) proofingProfile,
					      intent, proofingIntent, flags);
}


JNIEXPORT void JNICALL Java_com_lightcrafts_utils_LCMS_cmsDeleteTransform
  (JNIEnv *env, jclass clazz, jlong hTransform)
{
    cmsDeleteTransform((cmsHTRANSFORM) hTransform);
}

void cmsDoTransformGeneric
  (JNIEnv *env, jclass clazz, jlong hTransform, jbyteArray jinputBuffer, jbyteArray joutputBuffer, jint size)
{
    char *inputBuffer = (char *) (*env)->GetPrimitiveArrayCritical(env, jinputBuffer, 0);
    char *outputBuffer = (char *) (*env)->GetPrimitiveArrayCritical(env, joutputBuffer, 0);

    cmsDoTransform((cmsHTRANSFORM) hTransform, inputBuffer, outputBuffer, size);

    (*env)->ReleasePrimitiveArrayCritical(env, jinputBuffer, inputBuffer, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, joutputBuffer, outputBuffer, 0);
}

JNIEXPORT void JNICALL Java_com_lightcrafts_utils_LCMS_cmsDoTransform__J_3B_3BI
  (JNIEnv *env, jclass clazz, jlong hTransform, jbyteArray jinputBuffer, jbyteArray joutputBuffer, jint size)
{
    cmsDoTransformGeneric(env, clazz, hTransform, jinputBuffer, joutputBuffer, size);
}

JNIEXPORT void JNICALL Java_com_lightcrafts_utils_LCMS_cmsDoTransform__J_3S_3SI
  (JNIEnv *env, jclass clazz, jlong hTransform, jbyteArray jinputBuffer, jbyteArray joutputBuffer, jint size)
{
    cmsDoTransformGeneric(env, clazz, hTransform, jinputBuffer, joutputBuffer, size);
}

JNIEXPORT void JNICALL Java_com_lightcrafts_utils_LCMS_cmsDoTransform__J_3D_3DI
  (JNIEnv *env, jclass clazz, jlong hTransform, jbyteArray jinputBuffer, jbyteArray joutputBuffer, jint size)
{
    cmsDoTransformGeneric(env, clazz, hTransform, jinputBuffer, joutputBuffer, size);
}

