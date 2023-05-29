package org.square16.ictdroid.logcat;

import org.square16.ictdroid.logcat.handler.StackTraceHandler;
import org.square16.ictdroid.logcat.interfaces.ILogcatParser;
import org.square16.ictdroid.logcat.utils.LogInfo;
import org.square16.ictdroid.logcat.utils.TraceBlock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StackTraceHandlerTest {

    @Test
    void testHandle1() {
        String log = """
                2022-04-12 22:37:27.392 11115 11115 I TestService: TestService started at port 53110
                2022-04-12 22:37:27.392 11115 11131 D HostConnection: ExtendedRCEncoderContext GL_VERSION return OpenGL ES 3.1
                2022-04-12 22:37:27.394 11115 11115 W System.err: java.lang.Exception: Toast callstack! strTip=Test thread start
                2022-04-12 22:37:27.395 11115 11115 W System.err: \tat android.widget.Toast.show(Toast.java:130)
                2022-04-12 22:37:27.395 11115 11115 W System.err: \tat com.test.apptestclient.services.TestService.onStartCommand(TestService.java:222)
                2022-04-12 22:37:27.395 11115 11115 W System.err: \tat android.app.ActivityThread.handleServiceArgs(ActivityThread.java:3359)
                2022-04-12 22:37:27.395 11115 11115 W System.err: \tat android.app.ActivityThread.-wrap21(ActivityThread.java)
                2022-04-12 22:37:27.395 11115 11115 W System.err: \tat android.app.ActivityThread$H.handleMessage(ActivityThread.java:1587)
                2022-04-12 22:37:27.395 11115 11115 W System.err: \tat android.os.Handler.dispatchMessage(Handler.java:102)
                2022-04-12 22:37:27.395 11115 11115 W System.err: \tat android.os.Looper.loop(Looper.java:154)
                2022-04-12 22:37:27.395 11115 11115 W System.err: \tat android.app.ActivityThread.main(ActivityThread.java:6190)
                2022-04-12 22:37:27.395 11115 11115 W System.err: \tat java.lang.reflect.Method.invoke(Native Method)
                2022-04-12 22:37:27.395 11115 11115 W System.err: \tat com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:892)
                2022-04-12 22:37:27.395 11115 11115 W System.err: \tat com.android.internal.os.ZygoteInit.main(ZygoteInit.java:782)
                2022-04-12 22:37:27.405 11115 11131 E EGL_adreno: tid 11131: eglSurfaceAttrib(1338): error 0x3009 (EGL_BAD_MATCH)
                2022-04-12 22:37:27.405 11115 11131 W OpenGLRenderer: Failed to set EGL_SWAP_BEHAVIOR on surface 0x7fffe8b5c380, error=EGL_BAD_MATCH
                2022-04-12 22:37:27.431 11115 11131 D EGL_adreno: eglMakeCurrent: 0x7ffff2e7f6a0: ver 3 1 (tinfo 0x7fffe8b7d900)
                2022-04-12 22:38:28.105 11185 11185 D AndroidRuntime: Shutting down VM
                2022-04-12 22:38:28.123 11185 11217 W CrashlyticsCore: Cannot send reports. Settings are unavailable.
                2022-04-12 22:38:28.123 11185 11185 W System.err: java.lang.RuntimeException: Unable to start activity ComponentInfo{com.integreight.onesheeld/com.facebook.FacebookActivity}: java.lang.NullPointerException: Attempt to invoke virtual method 'boolean android.os.Bundle.getBoolean(java.lang.String, boolean)' on a null object reference
                2022-04-12 22:38:28.123 11185 11185 W System.err: \tat android.app.ActivityThread.performLaunchActivity(ActivityThread.java:2698)
                2022-04-12 22:38:28.123 11185 11185 W System.err: \tat android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:2759)
                2022-04-12 22:38:28.123 11185 11185 W System.err: \tat android.app.ActivityThread.-wrap12(ActivityThread.java)
                2022-04-12 22:38:28.123 11185 11185 W System.err: \tat android.app.ActivityThread$H.handleMessage(ActivityThread.java:1482)
                2022-04-12 22:38:28.123 11185 11185 W System.err: \tat android.os.Handler.dispatchMessage(Handler.java:102)
                2022-04-12 22:38:28.123 11185 11185 W System.err: \tat android.os.Looper.loop(Looper.java:154)
                2022-04-12 22:38:28.123 11185 11185 W System.err: \tat android.app.ActivityThread.main(ActivityThread.java:6190)
                2022-04-12 22:38:28.123 11185 11185 W System.err: \tat java.lang.reflect.Method.invoke(Native Method)
                2022-04-12 22:38:28.123 11185 11185 W System.err: \tat com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:892)
                2022-04-12 22:38:28.123 11185 11185 W System.err: \tat com.android.internal.os.ZygoteInit.main(ZygoteInit.java:782)
                2022-04-12 22:38:28.123 11185 11185 W System.err: Caused by: java.lang.NullPointerException: Attempt to invoke virtual method 'boolean android.os.Bundle.getBoolean(java.lang.String, boolean)' on a null object reference
                2022-04-12 22:38:28.123 11185 11185 W System.err: \tat com.facebook.internal.FacebookDialogFragment.onCreate(FacebookDialogFragment.java:62)
                2022-04-12 22:38:28.123 11185 11185 W System.err: \tat android.support.v4.app.Fragment.performCreate(Fragment.java:2180)
                2022-04-12 22:38:28.123 11185 11185 W System.err: \tat android.support.v4.app.FragmentManagerImpl.moveToState(FragmentManager.java:1244)
                2022-04-12 22:38:28.123 11185 11185 W System.err: \tat android.support.v4.app.FragmentManagerImpl.moveFragmentToExpectedState(FragmentManager.java:1528)
                2022-04-12 22:38:28.123 11185 11185 W System.err: \tat android.support.v4.app.FragmentManagerImpl.moveToState(FragmentManager.java:1595)
                2022-04-12 22:38:28.123 11185 11185 W System.err: \tat android.support.v4.app.BackStackRecord.executeOps(BackStackRecord.java:758)
                2022-04-12 22:38:28.123 11185 11185 W System.err: \tat android.support.v4.app.FragmentManagerImpl.executeOps(FragmentManager.java:2363)
                2022-04-12 22:38:28.123 11185 11185 W System.err: \tat android.support.v4.app.FragmentManagerImpl.executeOpsTogether(FragmentManager.java:2149)
                2022-04-12 22:38:28.123 11185 11185 W System.err: \tat android.support.v4.app.FragmentManagerImpl.optimizeAndExecuteOps(FragmentManager.java:2103)
                2022-04-12 22:38:28.123 11185 11185 W System.err: \tat android.support.v4.app.FragmentManagerImpl.execPendingActions(FragmentManager.java:2013)
                2022-04-12 22:38:28.123 11185 11185 W System.err: \tat android.support.v4.app.FragmentController.execPendingActions(FragmentController.java:388)
                2022-04-12 22:38:28.123 11185 11185 W System.err: \tat android.support.v4.app.FragmentActivity.onStart(FragmentActivity.java:607)
                2022-04-12 22:38:28.123 11185 11185 W System.err: \tat android.app.Instrumentation.callActivityOnStart(Instrumentation.java:1248)
                2022-04-12 22:38:28.123 11185 11185 W System.err: \tat android.app.Activity.performStart(Activity.java:6715)
                2022-04-12 22:38:28.123 11185 11185 W System.err: \tat android.app.ActivityThread.performLaunchActivity(ActivityThread.java:2661)
                2022-04-12 22:38:28.123 11185 11185 W System.err: \t... 9 more
                2022-04-12 22:38:28.127 11185 11196 I art     : Starting a blocking GC HeapTrim""";
        String expectedTraceBlockBody1 = """
                java.lang.Exception: Toast callstack! strTip=Test thread start
                \tat android.widget.Toast.show(Toast.java:130)
                \tat com.test.apptestclient.services.TestService.onStartCommand(TestService.java:222)
                \tat android.app.ActivityThread.handleServiceArgs(ActivityThread.java:3359)
                \tat android.app.ActivityThread.-wrap21(ActivityThread.java)
                \tat android.app.ActivityThread$H.handleMessage(ActivityThread.java:1587)
                \tat android.os.Handler.dispatchMessage(Handler.java:102)
                \tat android.os.Looper.loop(Looper.java:154)
                \tat android.app.ActivityThread.main(ActivityThread.java:6190)
                \tat java.lang.reflect.Method.invoke(Native Method)
                \tat com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:892)
                \tat com.android.internal.os.ZygoteInit.main(ZygoteInit.java:782)""";
        String expectedTraceBlockBody2 = """
                java.lang.RuntimeException: Unable to start activity ComponentInfo{com.integreight.onesheeld/com.facebook.FacebookActivity}: java.lang.NullPointerException: Attempt to invoke virtual method 'boolean android.os.Bundle.getBoolean(java.lang.String, boolean)' on a null object reference
                \tat android.app.ActivityThread.performLaunchActivity(ActivityThread.java:2698)
                \tat android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:2759)
                \tat android.app.ActivityThread.-wrap12(ActivityThread.java)
                \tat android.app.ActivityThread$H.handleMessage(ActivityThread.java:1482)
                \tat android.os.Handler.dispatchMessage(Handler.java:102)
                \tat android.os.Looper.loop(Looper.java:154)
                \tat android.app.ActivityThread.main(ActivityThread.java:6190)
                \tat java.lang.reflect.Method.invoke(Native Method)
                \tat com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:892)
                \tat com.android.internal.os.ZygoteInit.main(ZygoteInit.java:782)
                Caused by: java.lang.NullPointerException: Attempt to invoke virtual method 'boolean android.os.Bundle.getBoolean(java.lang.String, boolean)' on a null object reference
                \tat com.facebook.internal.FacebookDialogFragment.onCreate(FacebookDialogFragment.java:62)
                \tat android.support.v4.app.Fragment.performCreate(Fragment.java:2180)
                \tat android.support.v4.app.FragmentManagerImpl.moveToState(FragmentManager.java:1244)
                \tat android.support.v4.app.FragmentManagerImpl.moveFragmentToExpectedState(FragmentManager.java:1528)
                \tat android.support.v4.app.FragmentManagerImpl.moveToState(FragmentManager.java:1595)
                \tat android.support.v4.app.BackStackRecord.executeOps(BackStackRecord.java:758)
                \tat android.support.v4.app.FragmentManagerImpl.executeOps(FragmentManager.java:2363)
                \tat android.support.v4.app.FragmentManagerImpl.executeOpsTogether(FragmentManager.java:2149)
                \tat android.support.v4.app.FragmentManagerImpl.optimizeAndExecuteOps(FragmentManager.java:2103)
                \tat android.support.v4.app.FragmentManagerImpl.execPendingActions(FragmentManager.java:2013)
                \tat android.support.v4.app.FragmentController.execPendingActions(FragmentController.java:388)
                \tat android.support.v4.app.FragmentActivity.onStart(FragmentActivity.java:607)
                \tat android.app.Instrumentation.callActivityOnStart(Instrumentation.java:1248)
                \tat android.app.Activity.performStart(Activity.java:6715)
                \tat android.app.ActivityThread.performLaunchActivity(ActivityThread.java:2661)""";
        ILogcatParser parser = LogcatParserFactory.getParser(ILogcatParser.LOGCAT_V_YEAR);
        assertNotNull(parser);
        StackTraceHandler handler = new StackTraceHandler();
        assertNull(handler.getTraceBlock());

        for (String logLine : log.split("\n")) {
            LogInfo logInfo = parser.parse(logLine);
            handler.handle(null, logInfo);
            handler.setLastLineInfo(logInfo);
        }

        TraceBlock traceBlock1 = handler.getTraceBlock();
        assertEquals(traceBlock1.headInfo.time, "2022-04-12 22:37:27.394");
        assertEquals(traceBlock1.headInfo.pid, "11115");
        assertEquals(traceBlock1.headInfo.tid, "11115");
        assertEquals(traceBlock1.headInfo.level, "W");
        assertEquals(traceBlock1.headInfo.tag, "System.err");
        assertEquals(traceBlock1.headInfo.msg, "java.lang.Exception: Toast callstack! strTip=Test thread start");
        assertEquals(traceBlock1.body, expectedTraceBlockBody1);

        TraceBlock traceBlock2 = handler.getTraceBlock();
        assertEquals(traceBlock2.headInfo.time, "2022-04-12 22:38:28.123");
        assertEquals(traceBlock2.headInfo.pid, "11185");
        assertEquals(traceBlock2.headInfo.tid, "11185");
        assertEquals(traceBlock2.headInfo.level, "W");
        assertEquals(traceBlock2.headInfo.tag, "System.err");
        assertEquals(traceBlock2.headInfo.msg, "java.lang.RuntimeException: Unable to start activity ComponentInfo{" +
                "com.integreight.onesheeld/com.facebook.FacebookActivity}: java.lang.NullPointerException: " +
                "Attempt to invoke virtual method 'boolean android.os.Bundle.getBoolean(java.lang.String, boolean)' " +
                "on a null object reference");
        assertEquals(traceBlock2.body, expectedTraceBlockBody2);

        assertNull(handler.getTraceBlock());
    }
}