/*
 * Copyright (C) 2012 www.amsoft.cn
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yzy.supercleanmaster.bean;

import android.app.ActivityManager.RunningAppProcessInfo;
import android.util.ArrayMap;


// TODO: Auto-generated Javadoc

/**
 * @author
 * @version v1.0
 * @date：2013-11-13 上午9:00:52
 */
public class RunningProcInfo implements Comparable<RunningProcInfo> {

    public RunningAppProcessInfo processInfo;

    public boolean isShareProcess = false;

    /**
     * in Byte
     */
    public long memory = 0;

    /**
     * 占用的cpu.
     */
    public String cpu;

    /**
     * 进程的状态，其中S表示休眠，R表示正在运行，Z表示僵死状态，N表示该进程优先值是负数.
     */
    public String status;

    public boolean checked=true;

    /**
     * 是否是系统进程.
     */
    public boolean isSystem = false;

    /**
     * Instantiates a new ab process info.
     */
    public RunningProcInfo() {
        super();
    }

    /**
     * Instantiates a new ab process info.
     */
    public RunningProcInfo(RunningAppProcessInfo info, long memSize) {
        super();
        this.processInfo = info;
        this.memory = memSize;
    }


    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(RunningProcInfo another) {
        /*if (this.processName.compareTo(another.processName) == 0) {
            if (this.memory < another.memory) {
                return 1;
            } else if (this.memory == another.memory) {
                return 0;
            } else {
                return -1;
            }
        } else {
            return this.processName.compareTo(another.processName);
        }*/
        return 0;
    }

}
