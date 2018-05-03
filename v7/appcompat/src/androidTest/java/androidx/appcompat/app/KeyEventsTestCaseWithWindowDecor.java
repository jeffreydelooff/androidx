/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.appcompat.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.test.R;
import androidx.core.view.ViewCompat;

import org.junit.Test;

public class KeyEventsTestCaseWithWindowDecor extends BaseKeyEventsTestCase<WindowDecorAppCompatActivity> {
    public KeyEventsTestCaseWithWindowDecor() {
        super(WindowDecorAppCompatActivity.class);
    }

    @Test
    @SmallTest
    public void testUnhandledKeys() throws Throwable {
        final ViewGroup container = mActivityTestRule.getActivity().findViewById(R.id.test_content);
        final MockUnhandledKeyListener listener = new MockUnhandledKeyListener();
        final View mockView1 = new HandlerView(mActivityTestRule.getActivity());
        final HandlerView mockView2 = new HandlerView(mActivityTestRule.getActivity());
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();

        // Sanity check: should work before any unhandled stuff is used. This just needs to run
        // without causing a crash
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                container.addView(mockView2);
                mockView2.setFocusableInTouchMode(true);
                mockView2.requestFocus();
            }
        });
        instrumentation.waitForIdleSync();
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_B);
        // check that we're fine if a view consumes a down but not an up.
        mockView2.respondToDown = true;
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_B);
        assertTrue(mockView2.gotDown);
        mockView2.respondToDown = false;

        ViewCompat.addOnUnhandledKeyEventListener(mockView1, listener);

        // Before the view is attached, it shouldn't respond to anything
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_B);
        assertFalse(listener.fired());

        // Once attached, it should start receiving fallback events
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                container.addView(mockView1);
            }
        });
        instrumentation.waitForIdleSync();
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_B);
        assertTrue(listener.fired());
        listener.reset();

        // If removed, it should not receive fallbacks anymore
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViewCompat.removeOnUnhandledKeyEventListener(mockView1, listener);
            }
        });
        instrumentation.waitForIdleSync();
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_B);
        assertFalse(listener.fired());
    }

    private static class MockUnhandledKeyListener implements
            ViewCompat.OnUnhandledKeyEventListenerCompat {
        public View mLastView = null;
        public boolean mGotUp = false;
        public boolean mReturnVal = false;

        @Override
        public boolean onUnhandledKeyEvent(View v, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                mLastView = v;
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                mGotUp = true;
            }
            return mReturnVal;
        }
        public void reset() {
            mLastView = null;
            mGotUp = false;
        }
        public boolean fired() {
            return mLastView != null && mGotUp;
        }
    }

    /**
     * A View which can be set to consume or not consume key events.
     */
    private static class HandlerView extends View {
        HandlerView(Context ctx) {
            super(ctx);
        }

        public boolean respondToDown = false;
        public boolean gotDown = false;

        @Override
        public boolean dispatchKeyEvent(KeyEvent evt) {
            if (evt.getAction() == KeyEvent.ACTION_DOWN && respondToDown) {
                gotDown = true;
                return true;
            }
            return super.dispatchKeyEvent(evt);
        }
    }
}
