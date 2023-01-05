#include "com_github_ljbo82_jserial_NativeSerialPort.h"

#define STATIC_LIB
#include <serial.h>

#ifndef LIB_VERSION
	#warning "LIB_VERSION is not defined"
	#define LIB_VERSION "unknown"
#endif

#define __MIN(a,b) (a) < (b) ? (a) : (b)

JNIEXPORT jstring JNICALL Java_com_github_ljbo82_jserial_NativeSerialPort_get_1native_1lib_1version(JNIEnv* env, jclass cls) {
	static jstring mVersion = NULL;
	if (!mVersion) {
		mVersion = (*env)->NewStringUTF(env, LIB_VERSION);
	}

	return mVersion;
}

JNIEXPORT jobjectArray JNICALL Java_com_github_ljbo82_jserial_NativeSerialPort_get_1port_1names(JNIEnv* env, jclass cls) {
	static serial_list_t* mList = NULL;

	if (!mList) {
		mList = serial_list_new();
		if (!mList) {
			goto error;
		}
	}

	if (!serial_list_ports(mList))
		goto error;

	jobjectArray result = (*env)->NewObjectArray(env, serial_list_size(mList), (*env)->FindClass(env, "java/lang/String"), NULL);
	for (size_t i = 0; i < serial_list_size(mList); i++) {
		(*env)->SetObjectArrayElement(env, result, i, (*env)->NewStringUTF(env, serial_list_item(mList, i)));
	}

	return result;

error:
	return NULL;
}

JNIEXPORT jint JNICALL Java_com_github_ljbo82_jserial_NativeSerialPort_get_1current_1error(JNIEnv* env, jclass cls) {
	return errno;
}

JNIEXPORT void JNICALL Java_com_github_ljbo82_jserial_NativeSerialPort_clear_1current_1error(JNIEnv* env, jclass cls) {
	errno = 0;
}

JNIEXPORT jlong JNICALL Java_com_github_ljbo82_jserial_NativeSerialPort_open(JNIEnv* env, jclass cls, jstring portName) {
	return (jlong)serial_open((*env)->GetStringUTFChars(env, portName, NULL));
}

JNIEXPORT jboolean JNICALL Java_com_github_ljbo82_jserial_NativeSerialPort_close(JNIEnv* env, jclass cls, jlong nativePort) {
	return serial_close((serial_t*)nativePort);
}

JNIEXPORT jboolean JNICALL Java_com_github_ljbo82_jserial_NativeSerialPort_config(JNIEnv* env, jclass cls, jlong nativePort, jint baud, jint dataBits, jint parity, jint stopBits) {
	serial_config_t cfg = {
		.baud     = baud,
		.dataBits = dataBits,
		.parity   = parity,
		.stopBits = stopBits
	};

	return serial_config((serial_t*)nativePort, &cfg);
}

JNIEXPORT jboolean JNICALL Java_com_github_ljbo82_jserial_NativeSerialPort_purge(JNIEnv* env, jclass cls, jlong nativePort, jint purgeType) {
	return serial_purge((serial_t*)nativePort, purgeType);
}

JNIEXPORT jint JNICALL Java_com_github_ljbo82_jserial_NativeSerialPort_get_1baud(JNIEnv* env, jclass cls, jlong nativePort) {
	serial_config_t cfg;
	serial_get_config((serial_t*)nativePort, &cfg);
	return cfg.baud;
}

JNIEXPORT jint JNICALL Java_com_github_ljbo82_jserial_NativeSerialPort_get_1data_1bits(JNIEnv* env, jclass cls, jlong nativePort) {
	serial_config_t cfg;
	serial_get_config((serial_t*)nativePort, &cfg);
	return cfg.dataBits;
}

JNIEXPORT jint JNICALL Java_com_github_ljbo82_jserial_NativeSerialPort_get_1parity(JNIEnv* env, jclass cls, jlong nativePort) {
	serial_config_t cfg;
	serial_get_config((serial_t*)nativePort, &cfg);
	return cfg.parity;
}

JNIEXPORT jint JNICALL Java_com_github_ljbo82_jserial_NativeSerialPort_get_1stop_1bits(JNIEnv* env, jclass cls, jlong nativePort) {
	serial_config_t cfg;
	serial_get_config((serial_t*)nativePort, &cfg);
	return cfg.stopBits;
}

JNIEXPORT jboolean JNICALL Java_com_github_ljbo82_jserial_NativeSerialPort_set_1read_1timeout(JNIEnv* env, jclass cls, jlong nativePort, jlong millis) {
	return serial_set_read_timeout((serial_t*)nativePort, millis);
}

JNIEXPORT jlong JNICALL Java_com_github_ljbo82_jserial_NativeSerialPort_get_1read_1timeout(JNIEnv* env, jclass cls, jlong nativePort) {
	return serial_get_read_timeout((serial_t*)nativePort);
}

JNIEXPORT jint JNICALL Java_com_github_ljbo82_jserial_NativeSerialPort_read(JNIEnv* env, jclass cls, jlong nativePort, jbyteArray out, jint off, jint len) {
	if (off < 0) {
		errno = SERIAL_ERROR_INVALID_PARAM;
		return -1;
	}

	if (len < 0) {
		errno = SERIAL_ERROR_INVALID_PARAM;
		return -1;
	}

	if (off + len > (*env)->GetArrayLength(env, out)) {
		errno = SERIAL_ERROR_INVALID_PARAM;
		return -1;
	}

	jbyte mBuffer[1024];
	int32_t mRead = serial_read((serial_t*)nativePort, mBuffer, __MIN(sizeof(mBuffer), len));

	if (mRead < 0)
		return -1;

	(*env)->SetByteArrayRegion(env, out, off, mRead, mBuffer);
	return mRead;
}

JNIEXPORT jboolean JNICALL Java_com_github_ljbo82_jserial_NativeSerialPort_write(JNIEnv* env, jclass cls, jlong nativePort, jbyteArray in, jint off, jint len) {
	if (off < 0) {
		errno = SERIAL_ERROR_INVALID_PARAM;
		return -1;
	}

	if (len < 0) {
		errno = SERIAL_ERROR_INVALID_PARAM;
		return -1;
	}

	if (off + len > (*env)->GetArrayLength(env, in)) {
		errno = SERIAL_ERROR_INVALID_PARAM;
		return -1;
	}

	jbyte mBuffer[1024];
	jsize remaining = len;
	jsize cursor = off;

	while (remaining > 0) {
		len = __MIN(remaining, sizeof(mBuffer));
		(*env)->GetByteArrayRegion(env, in, cursor, len, mBuffer);

		if (!serial_write((serial_t*)nativePort, mBuffer, len)) {
			return false;
		}

		remaining -= len;
		cursor += len;
	}

	return true;
}

JNIEXPORT jboolean JNICALL Java_com_github_ljbo82_jserial_NativeSerialPort_flush(JNIEnv* env, jclass cls, jlong nativePort) {
	return serial_flush((serial_t*)nativePort);
}
