#include <string.h>
#include <malloc.h>
#include "interface-wrapper.h"

jmethodID method_apdu_connect;
jmethodID method_apdu_disconnect;
jmethodID method_apdu_logical_channel_open;
jmethodID method_apdu_logical_channel_close;
jmethodID method_apdu_transmit;

jmethodID method_http_transmit;

jfieldID field_resp_rcode;
jfieldID field_resp_data;

void interface_wrapper_init() {
    LPAC_JNI_SETUP_ENV;
    jclass apdu_class = (*env)->FindClass(env, "net/typeblog/lpac_jni/ApduInterface");
    method_apdu_connect = (*env)->GetMethodID(env, apdu_class, "connect", "()V");
    method_apdu_disconnect = (*env)->GetMethodID(env, apdu_class, "disconnect", "()V");
    method_apdu_logical_channel_open = (*env)->GetMethodID(env, apdu_class, "logicalChannelOpen",
                                                           "([B)I");
    method_apdu_logical_channel_close = (*env)->GetMethodID(env, apdu_class, "logicalChannelClose",
                                                            "(I)V");
    method_apdu_transmit = (*env)->GetMethodID(env, apdu_class, "transmit", "(I[B)[B");

    jclass http_class = (*env)->FindClass(env, "net/typeblog/lpac_jni/HttpInterface");
    method_http_transmit = (*env)->GetMethodID(env, http_class, "transmit",
                                               "(Ljava/lang/String;[B[Ljava/lang/String;)Lnet/typeblog/lpac_jni/HttpInterface$HttpResponse;");

    jclass resp_class = (*env)->FindClass(env, "net/typeblog/lpac_jni/HttpInterface$HttpResponse");
    field_resp_rcode = (*env)->GetFieldID(env, resp_class, "rcode", "I");
    field_resp_data = (*env)->GetFieldID(env, resp_class, "data", "[B");
}

static int apdu_interface_connect(struct euicc_ctx *ctx) {
    LPAC_JNI_SETUP_ENV;
    (*env)->CallVoidMethod(env, LPAC_JNI_CTX(ctx)->apdu_interface, method_apdu_connect);
    LPAC_JNI_EXCEPTION_RETURN;
    return 0;
}

static void apdu_interface_disconnect(struct euicc_ctx *ctx) {
    LPAC_JNI_SETUP_ENV;
    (*env)->CallVoidMethod(env, LPAC_JNI_CTX(ctx)->apdu_interface, method_apdu_disconnect);
}

static int
apdu_interface_logical_channel_open(struct euicc_ctx *ctx, const uint8_t *aid, uint8_t aid_len) {
    LPAC_JNI_SETUP_ENV;
    jbyteArray jbarr = (*env)->NewByteArray(env, aid_len);
    if (jbarr == NULL) {
        return -1;
    }
    (*env)->SetByteArrayRegion(env, jbarr, 0, aid_len, (const jbyte *) aid);
    LPAC_JNI_EXCEPTION_RETURN;
    jint ret = (*env)->CallIntMethod(env, LPAC_JNI_CTX(ctx)->apdu_interface,
                                     method_apdu_logical_channel_open, jbarr);
    LPAC_JNI_EXCEPTION_RETURN;
    (*env)->DeleteLocalRef(env, jbarr);
    LPAC_JNI_CTX(ctx)->logical_channel_id = ret;
    return ret;
}

static void apdu_interface_logical_channel_close(struct euicc_ctx *ctx,
                                                 __attribute__((unused)) uint8_t channel) {
    LPAC_JNI_SETUP_ENV;
    jint logical_channel_id = LPAC_JNI_CTX(ctx)->logical_channel_id;
    (*env)->CallVoidMethod(env, LPAC_JNI_CTX(ctx)->apdu_interface,
                           method_apdu_logical_channel_close, logical_channel_id);
    (*env)->ExceptionClear(env);
}

static int
apdu_interface_transmit(struct euicc_ctx *ctx, uint8_t **rx, uint32_t *rx_len, const uint8_t *tx,
                        uint32_t tx_len) {
    const int logic_channel = LPAC_JNI_CTX(ctx)->logical_channel_id;
    LPAC_JNI_SETUP_ENV;
    jbyteArray txArr = (*env)->NewByteArray(env, tx_len);
    if (txArr == NULL) {
        return -1;
    }
    (*env)->SetByteArrayRegion(env, txArr, 0, tx_len, (const jbyte *) tx);
    LPAC_JNI_EXCEPTION_RETURN;
    jbyteArray ret = (jbyteArray) (*env)->CallObjectMethod(
            env, LPAC_JNI_CTX(ctx)->apdu_interface,
            method_apdu_transmit, logic_channel, txArr
    );
    LPAC_JNI_EXCEPTION_RETURN;
    if (ret == NULL) {
        (*env)->DeleteLocalRef(env, txArr);
        return -1;
    }
    *rx_len = (*env)->GetArrayLength(env, ret);
    LPAC_JNI_EXCEPTION_RETURN;
    *rx = calloc(*rx_len, sizeof(uint8_t));
    if (*rx == NULL && *rx_len > 0) {
        (*env)->DeleteLocalRef(env, txArr);
        (*env)->DeleteLocalRef(env, ret);
        return -1;
    }
    (*env)->GetByteArrayRegion(env, ret, 0, *rx_len, (jbyte *) *rx);
    LPAC_JNI_EXCEPTION_RETURN;
    (*env)->DeleteLocalRef(env, txArr);
    (*env)->DeleteLocalRef(env, ret);
    return 0;
}

static int
http_interface_transmit(struct euicc_ctx *ctx, const char *url, uint32_t *rcode, uint8_t **rx,
                        uint32_t *rx_len, const uint8_t *tx, uint32_t tx_len,
                        const char **headers) {
    LPAC_JNI_SETUP_ENV;
    jstring jurl = toJString(env, url);
    jbyteArray txArr = (*env)->NewByteArray(env, tx_len);
    if (jurl == NULL || txArr == NULL) {
        return -1;
    }
    (*env)->SetByteArrayRegion(env, txArr, 0, tx_len, (const jbyte *) tx);
    LPAC_JNI_EXCEPTION_RETURN;

    int num_headers = 0;
    while (headers[num_headers] != NULL) {
        num_headers++;
    }
    jobjectArray headersArr = (*env)->NewObjectArray(env, num_headers, string_class, NULL);
    if (headersArr == NULL) {
        (*env)->DeleteLocalRef(env, txArr);
        (*env)->DeleteLocalRef(env, jurl);
        return -1;
    }
    for (int i = 0; i < num_headers; i++) {
        jstring header = toJString(env, headers[i]);
        if (header == NULL) {
            (*env)->DeleteLocalRef(env, txArr);
            (*env)->DeleteLocalRef(env, headersArr);
            (*env)->DeleteLocalRef(env, jurl);
            return -1;
        }
        (*env)->SetObjectArrayElement(env, headersArr, i, header);
        if ((*env)->ExceptionCheck(env) == JNI_TRUE) {
            (*env)->DeleteLocalRef(env, header);
            (*env)->DeleteLocalRef(env, txArr);
            (*env)->DeleteLocalRef(env, headersArr);
            (*env)->DeleteLocalRef(env, jurl);
            (*env)->ExceptionClear(env);
            return -1;
        }
        (*env)->DeleteLocalRef(env, header);
    }

    jobject ret = (*env)->CallObjectMethod(env, LPAC_JNI_CTX(ctx)->http_interface,
                                           method_http_transmit, jurl, txArr, headersArr);
    LPAC_JNI_EXCEPTION_RETURN;
    if (ret == NULL) {
        (*env)->DeleteLocalRef(env, txArr);
        (*env)->DeleteLocalRef(env, headersArr);
        (*env)->DeleteLocalRef(env, jurl);
        return -1;
    }
    *rcode = (*env)->GetIntField(env, ret, field_resp_rcode);
    LPAC_JNI_EXCEPTION_RETURN;
    jbyteArray rxArr = (jbyteArray) (*env)->GetObjectField(env, ret, field_resp_data);
    LPAC_JNI_EXCEPTION_RETURN;
    if (rxArr == NULL) {
        (*env)->DeleteLocalRef(env, txArr);
        (*env)->DeleteLocalRef(env, headersArr);
        (*env)->DeleteLocalRef(env, jurl);
        (*env)->DeleteLocalRef(env, ret);
        return -1;
    }
    *rx_len = (*env)->GetArrayLength(env, rxArr);
    LPAC_JNI_EXCEPTION_RETURN;
    *rx = calloc(*rx_len, sizeof(uint8_t));
    if (*rx == NULL && *rx_len > 0) {
        (*env)->DeleteLocalRef(env, txArr);
        (*env)->DeleteLocalRef(env, rxArr);
        (*env)->DeleteLocalRef(env, headersArr);
        (*env)->DeleteLocalRef(env, jurl);
        (*env)->DeleteLocalRef(env, ret);
        return -1;
    }
    (*env)->GetByteArrayRegion(env, rxArr, 0, *rx_len, (jbyte *) *rx);
    LPAC_JNI_EXCEPTION_RETURN;
    (*env)->DeleteLocalRef(env, txArr);
    (*env)->DeleteLocalRef(env, rxArr);
    (*env)->DeleteLocalRef(env, headersArr);
    (*env)->DeleteLocalRef(env, jurl);
    (*env)->DeleteLocalRef(env, ret);
    return 0;
}

struct euicc_apdu_interface lpac_jni_apdu_interface = {
        .connect = &apdu_interface_connect,
        .disconnect = &apdu_interface_disconnect,
        .logic_channel_open = &apdu_interface_logical_channel_open,
        .logic_channel_close = &apdu_interface_logical_channel_close,
        .transmit = &apdu_interface_transmit
};

struct euicc_http_interface lpac_jni_http_interface = {
        .transmit = &http_interface_transmit
};
