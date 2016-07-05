package com.lab.powercounter.util;

import java.util.Vector;

/**
 * Created by Admin on 2016/7/5.
 */

/**
 * 为了减少大量对象的不断创建和销毁
 * 模拟垃圾回收器， 来回收一些对象
 *
 * @param <T> 模板 T
 */
public class Recycler<T> {
    private Vector<T> list;
    private int avail;

    public Recycler() {
        list = new Vector<>();
        avail = 0;
    }

    public synchronized T obtain() {
        if (avail == 0)
            return null;
        return list.get(--avail);
    }

    public synchronized void recycle(T a) {
        if (avail < list.size()) {
            list.set(avail++, a);
        }
        list.add(a);
    }
}
