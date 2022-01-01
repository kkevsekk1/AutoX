#include "com_stardust_autojs_runtime_api_SevenZip.h"
#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>
#include "android/log.h"

#define LOG_ENABLE
#ifdef LOG_ENABLE
#define LOG_TAG "p7zip_jni"
#define LOGI(...) do {__android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__);} while(0)
#define LOGD(...) do {__android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__);} while(0)
#define LOGE(...) do {__android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__);} while(0)
#else
...
#endif

#define ARGV_LEN_MAX 256
#define ARGC_MAX 32

extern int MY_CDECL main(
#ifndef _WIN32
        int numArgs, const char *args[]
#endif
);

static bool str2args(const char* s,char argv[][ARGV_LEN_MAX],int* argc) {
    bool in_token, in_container, escaped;
    bool result;
    char container_start, c;
    int len, i;
    int index = 0;
    int arg_count = 0;

    result = true;
    container_start = 0;
    in_token = false;
    in_container = false;
    escaped = false;

    len = strlen(s);
    for (i = 0; i < len; i++) {
        c = s[i];
        switch (c) {
            case ' ':
            case '\t':
            case '\n':

                if (!in_token)
                    continue;
                if (in_container) {
                    argv[arg_count][index++] = c;
                    continue;
                }
                if (escaped) {
                    escaped = false;
                    argv[arg_count][index++] = c;
                    continue;
                }
                /* if reached here, we're at end of token */
                in_token = false;
                argv[arg_count++][index] = '\0';
                index = 0;
                break;
                /* handle quotes */
            case '\'':
            case '\"':

                if (escaped) {
                    argv[arg_count][index++] = c;
                    escaped = false;
                    continue;
                }
                if (!in_token) {
                    in_token = true;
                    in_container = true;
                    container_start = c;
                    continue;
                }
                if (in_container) {
                    if (c == container_start) { //container end
                        in_container = false;
                        in_token = false;
                        argv[arg_count++][index] = '\0';
                        index = 0;
                        continue;
                    } else { //not the same as contain start char
                        argv[arg_count][index++] = c;
                        continue;
                    }
                }
                LOGE("Parse Error! Bad quotes\n");
                result = false;
                break;
            case '\\':

                if (in_container && s[i + 1] != container_start) {
                    argv[arg_count][index++] = c;
                    continue;
                }
                if (escaped) {
                    argv[arg_count][index++] = c;
                    continue;
                }
                escaped = true;
                break;
            default: //normal char

                if (!in_token) {
                    in_token = true;
                }
                argv[arg_count][index++] = c;
                if (i == len - 1) { //reach the end
                    argv[arg_count++][index++] = '\0';
                }
                break;
        }
    }
    *argc = arg_count;

    if (in_container) {
        LOGE("Parse Error! Still in container\n");
        result = false;
    }
    if (escaped) {
        LOGE("Parse Error! Unused escape (\\)\n");
        result = false;
    }
    return result;
}


extern "C"
JNIEXPORT jint JNICALL
Java_com_stardust_autojs_runtime_api_SevenZip_cmdExec(JNIEnv *env, jclass clazz, jstring cmd_str) {
    int result = -1;
    const char* tmp_cmd = (const char*)env->GetStringUTFChars(cmd_str,NULL);

    LOGI("start[%s]",tmp_cmd);

    int argc = 0;
    char temp[ARGC_MAX][ARGV_LEN_MAX] = {0};
    char* argv[ARGC_MAX] = {0};

    if (str2args(tmp_cmd,temp,&argc)==false) {
        return 7;
    }

    for (int i=0;i<argc;i++) {
        argv[i] = temp[i];
        LOGD("arg[%d]:[%s]",i,argv[i]);
    }

    result = main(argc,(const char**)argv);

    LOGI("end[%s]",tmp_cmd);

    env->ReleaseStringUTFChars(cmd_str,tmp_cmd);

    return result;
}
