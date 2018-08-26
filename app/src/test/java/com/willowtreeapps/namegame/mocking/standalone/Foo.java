package com.willowtreeapps.namegame.mocking.standalone;

/**
 * A class to test whether Foo can be mocked an call its real method
 */
public class Foo {
    /**
     *
     * @param x A dependent object we may mock
     * @return x.getVal() + 1
     */
    int increment(Bar x) {
         return x.getVal() + 1;
    }

    /**
     * Define decrement in terms of increment to test stubbing
     * @param x A dependent object we may mock
     * @return -2 + increment(x)
     */
    int decrement(Bar x) {
        return -2 + increment(x);
    }
}
