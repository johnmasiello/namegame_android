package com.willowtreeapps.namegame.mocking.standalone;

import android.support.test.filters.SmallTest;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@SmallTest
public class FooBarTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void mockFooIncrement() {
        Bar bar = new Bar(1);
        Foo foo = mock(Foo.class);
        Assert.assertNotEquals("Mocked foo should not return 2",
                2,
                foo.increment(bar));
    }

    @Test
    public void partialMockFooIncrement() {
        Bar bar = new Bar(1);
        Foo foo = mock(Foo.class);
        when(foo.increment(bar)).thenCallRealMethod();
        Assert.assertEquals("Mocked foo should return 2",
                2,
                foo.increment(bar));
    }

    @Test
    public void partialMockFooBarIncrement() {
        Bar bar = mock(Bar.class);
        Foo foo = mock(Foo.class);
        when(bar.getVal()).thenReturn(1);
        when(foo.increment(bar)).thenCallRealMethod();
        Assert.assertEquals("Mocked foo should return 2",
                2,
                foo.increment(bar));
    }

    @Test
    public void partialMockFooBarDecrement() {
        Bar bar = mock(Bar.class);
        Foo foo = mock(Foo.class);
        when(foo.increment(bar)).thenReturn(2);
        when(foo.decrement(bar)).thenCallRealMethod();
        Assert.assertEquals("Mocked foo should return 0",
                0,
                foo.decrement(bar));
    }

    // "Mockito.spy() is a recommended way of creating partial mocks.
    // The reason is it guarantees real methods are called against correctly
    // constructed object because you're responsible for
    // constructing the object passed to spy() method"
    @Test
    public void spyFooIncrement() {
        Bar bar = new Bar(1);
        Foo foo = new Foo();
        // Warning: Spy makes a copy of foo and will not test interactions on foo
        Foo spy = spy(foo);
        doReturn(3).when(spy).increment(any(Bar.class));
        Assert.assertEquals("Mocked foo should return 3",
                3,
                spy.increment(bar));
    }

    @Test
    public void spyFooIncrementThrowsException() throws Exception {
        Bar bar = new Bar(1);
        Foo foo = new Foo();
        // Warning: Spy makes a copy of foo and will not test interactions on foo
        Foo spy = spy(foo);

        final String SPY_CRASH_MESSAGE = "Real Method may crash when using \'when\' stubbing";

        thrown.expectMessage("\'when\'");

        try {
            when(spy.increment(any(Bar.class))).thenReturn(3);
            Assert.assertEquals("Mocked foo should return 3",
                    3,
                    spy.increment(bar));
        } catch (Exception ignore) {
            throw new Exception(SPY_CRASH_MESSAGE);
        }
    }
}
