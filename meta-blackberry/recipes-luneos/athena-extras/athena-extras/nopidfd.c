/* LD_PRELOAD shim for athena (4.19 kernel): Android's backport of pidfd_open
 * exists, but waitid(P_PIDFD) does not (that is 5.4+). glib >= 2.76 sees a
 * working pidfd_open, then fails every child-watch reap with EINVAL. Making
 * pidfd_open report ENOSYS sends glib down its SIGCHLD/waitpid fallback. */
#define _GNU_SOURCE
#include <dlfcn.h>
#include <errno.h>
#include <stdarg.h>

#define NR_PIDFD_OPEN 434

long syscall(long n, ...)
{
	static long (*real)(long, ...);
	va_list ap;
	long a, b, c, d, e, f;

	if (n == NR_PIDFD_OPEN) {
		errno = ENOSYS;
		return -1;
	}
	if (!real)
		real = (long (*)(long, ...))dlsym(RTLD_NEXT, "syscall");
	va_start(ap, n);
	a = va_arg(ap, long); b = va_arg(ap, long); c = va_arg(ap, long);
	d = va_arg(ap, long); e = va_arg(ap, long); f = va_arg(ap, long);
	va_end(ap);
	return real(n, a, b, c, d, e, f);
}
