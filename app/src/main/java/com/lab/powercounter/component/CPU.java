package com.lab.powercounter.component;

import com.lab.powercounter.service.PowerData;
import com.lab.powercounter.util.Recycler;

import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Created by Admin on 2016/7/5.
 */
public class CPU {
    public static class CpuData extends PowerData {
        private static Recycler<CpuData> recycler = new Recycler<>();

        public static CpuData obtain() {
            CpuData result = recycler.obtain();
            if (result != null)
                return result;
            return new CpuData();
        }

        public void recycle() {
            recycler.recycle(this);
        }

        public double sysPerc;      //Sys time
        public double usrPerc;      //User time
        public double freq;

        private CpuData() {

        }

        public void init(double sysPerc, double usrPerc, double freq) {
            this.sysPerc = sysPerc;
            this.usrPerc = usrPerc;
            this.freq = freq;
        }

        @Override
        public void writeLogDataInfo(OutputStreamWriter out) throws IOException {
            StringBuilder res = new StringBuilder();
            res.append("CPU-sys ").append((long) Math.round(sysPerc))
                    .append("\nCPU-usr ").append((long) Math.round(usrPerc))
                    .append("\nCPU-freq ").append(freq)
                    .append("\n");
            out.write(res.toString());
        }
    }

    private static final String TAG = "CPU";
    private static final String CPU_FREQ_FILE = "/proc/cpuinfo";
    private static final String STAT_FILE = "/proc/stat";


}
