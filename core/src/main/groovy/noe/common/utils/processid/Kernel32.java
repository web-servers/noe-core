/* Copyright (c) 2007, 2013 Timothy Wall, Markus Karg, All Rights Reserved
 *
 * The contents of this file is dual-licensed under 2
 * alternative Open Source/Free licenses: LGPL 2.1 or later and
 * Apache License 2.0. (starting with JNA version 4.0.0).
 *
 * You can freely decide which license you want to apply to
 * the project.
 *
 * You may obtain a copy of the LGPL License at:
 *
 * http://www.gnu.org/licenses/licenses.html
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "LGPL2.1".
 *
 * You may obtain a copy of the Apache License at:
 *
 * http://www.apache.org/licenses/
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "AL2.0".
 *
 * You can find JNA original source files at:
 *
 * https://github.com/java-native-access/jna
 */

package noe.common.utils.processid;

import com.sun.jna.Native;

/* https://jna.dev.java.net/ */
public interface Kernel32 extends W32API {
    Kernel32 INSTANCE = (Kernel32) Native.loadLibrary("kernel32", Kernel32.class, DEFAULT_OPTIONS);

    /* http://msdn.microsoft.com/en-us/library/ms683179(VS.85).aspx */
    HANDLE GetCurrentProcess();

    /* http://msdn.microsoft.com/en-us/library/ms683215.aspx */
    int GetProcessId(HANDLE Process);
}
