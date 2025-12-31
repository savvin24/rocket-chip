#define _GNU_SOURCE
#include <unistd.h>
#include <sys/ioctl.h>
#include <linux/perf_event.h>
#include <sys/syscall.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>

static long perf_event_open(struct perf_event_attr *hw_event, pid_t pid,
                            int cpu, int group_fd, unsigned long flags)
{
    return syscall(__NR_perf_event_open, hw_event, pid, cpu, group_fd, flags);
}

int main(void)
{
    struct perf_event_attr pe;
    memset(&pe, 0, sizeof(pe));
    pe.type = PERF_TYPE_RAW;
    pe.size = sizeof(pe);
    pe.config = 0x200; /* cycles */
    pe.disabled = 1;
    pe.exclude_kernel = 0;
    pe.exclude_hv = 0;

    int fd = perf_event_open(&pe, 0, -1, -1, 0);
    if (fd == -1) { perror("perf_event_open"); return 1; }

    ioctl(fd, PERF_EVENT_IOC_RESET, 0);
    ioctl(fd, PERF_EVENT_IOC_ENABLE, 0);

    sleep(1);

    ioctl(fd, PERF_EVENT_IOC_DISABLE, 0);
    uint64_t count;
    if (read(fd, &count, sizeof(count)) != sizeof(count)) {
        perror("read");
        return 1;
    }
    printf("cycles: %llu\n", (unsigned long long)count);
    close(fd);
    return 0;
}
