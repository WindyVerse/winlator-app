/* Server side of libxshmfence for DRI3 FenceFromFD. The bundled Termux libxshmfence
 * is the process-shared pthread variant; both processes use Android's bionic, so the
 * X server triggers the client's fence exactly as xshmfence_trigger does. */
#include <jni.h>
#include <pthread.h>
#include <stddef.h>
#include <stdint.h>
#include <sys/mman.h>
#include <unistd.h>

struct xshmfence {
    pthread_mutex_t lock;
    pthread_cond_t wakeup;
    int value;
    int waiting;
};

#if defined(__aarch64__)
/* Offsets observed in the bundled libxshmfence.so's xshmfence_trigger. */
_Static_assert(offsetof(struct xshmfence, wakeup) == 0x28, "xshmfence condition offset changed");
_Static_assert(offsetof(struct xshmfence, value) == 0x58, "xshmfence value offset changed");
_Static_assert(offsetof(struct xshmfence, waiting) == 0x5c, "xshmfence waiting offset changed");
#endif

JNIEXPORT jlong JNICALL
Java_com_winlator_xserver_extensions_ShmFence_nativeMap(JNIEnv *env, jclass type, jint fd) {
    (void)env; (void)type;
    if (fd < 0) return 0;
    void *fence = mmap(NULL, sizeof(struct xshmfence), PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
    close(fd);
    return fence == MAP_FAILED ? 0 : (jlong)(intptr_t)fence;
}

JNIEXPORT void JNICALL
Java_com_winlator_xserver_extensions_ShmFence_nativeTrigger(JNIEnv *env, jclass type, jlong address) {
    (void)env; (void)type;
    struct xshmfence *fence = (struct xshmfence *)(intptr_t)address;
    pthread_mutex_lock(&fence->lock);
    if (fence->value == 0) {
        fence->value = 1;
        if (fence->waiting) {
            fence->waiting = 0;
            pthread_cond_broadcast(&fence->wakeup);
        }
    }
    pthread_mutex_unlock(&fence->lock);
}

JNIEXPORT void JNICALL
Java_com_winlator_xserver_extensions_ShmFence_nativeUnmap(JNIEnv *env, jclass type, jlong address) {
    (void)env; (void)type;
    munmap((void *)(intptr_t)address, sizeof(struct xshmfence));
}
