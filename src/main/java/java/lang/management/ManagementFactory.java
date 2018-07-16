package java.lang.management;

import java.util.ArrayList;
import java.util.List;
import javax.management.MBeanServer;
import javax.management.ThreadMXBean;
import javax.management.GarbageCollectorMXBean;

public class ManagementFactory {

    public static synchronized MBeanServer getPlatformMBeanServer() {
        return new MBeanServer();
    }
    
    public static ThreadMXBean getThreadMXBean() {
        return new ThreadMXBean();
    }

    public static List<GarbageCollectorMXBean> getGarbageCollectorMXBeans() {
        return new ArrayList<>();
    }
    
    public static OperatingSystemMXBean getOperatingSystemMXBean() {
        return new OperatingSystemMXBean();
    }
}
