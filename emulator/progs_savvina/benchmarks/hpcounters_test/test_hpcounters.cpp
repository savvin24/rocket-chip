#include "HPC.h"

using namespace std;

int main()
{
    int a=2;
    volatile int arr[1000];

    HPC perfMon;
    perfMon.startMeasurement();

    // Simulate some operations
    for (int i = 0; i < 1000; ++i) {
        arr[i] = i*a;
    }

    perfMon.endMeasurement();
    perfMon.printStats();
    perfMon.printCSV();

    return 0;
}