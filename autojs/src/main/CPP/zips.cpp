#include "com_stardust_autojs_runtime_api_SevenZip.h"
#include <jni.h>
#include <string.h>
#include <7zTypes.h>
#include <android/log.h>
#include <stdlib.h>
#include <stdbool.h>

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "7za",__VA_ARGS__);

extern int MY_CDECL main
        (
#ifndef _WIN32
        int numArgs, char *args[]
#endif
);

void strArgs(const char *cmd, int &args, char pString[][1024]);

extern "C"
JNIEXPORT jint JNICALL
Java_com_stardust_autojs_runtime_api_SevenZip_cmd(JNIEnv *env, jclass type, jstring cmd_) {
    const char *cmd = env->GetStringUTFChars(cmd_, 0);
    int numArgs;
    char temp[64][1024] = {0};
    strArgs(cmd, numArgs, temp);
    char *args[] = {0};
    for (int i = 0; i < numArgs; ++i) {
        args[i] = temp[i];
        LOGE("%s", args[i]);
    }

    env->ReleaseStringUTFChars(cmd_, cmd);
    return main(numArgs, args);
}

void strArgs(const char *cmd, int &numArgs, char argv[][1024]) {
    int size = strlen(cmd);
    int a = 0, b = 0;
    int space = 0;
    for (int i = 0; i < size; ++i) {
        char c = cmd[i];
        switch (c) {
            case ' ':
            case '\t':
                if (space) {
                    argv[a][b++] = '\0';
                    a++;
                    b = 0;
                    space = 0;
                }
                break;
            default:
                space = 1;
                argv[a][b++] = c;
                break;
        }
    }
    if (cmd[size - 1] != ' ' && cmd[size - 1] != '\t') {
        argv[a][b] = '\0';
        a++;
    }
    numArgs = a;
}
