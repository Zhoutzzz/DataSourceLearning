/*
 * MIT License
 *
 * Copyright (c) 2021 Z
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 */

package per.zhoutzzz.datasource.pool;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.FixedValue;

import java.io.File;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * @author zhoutzzz
 */
public class ByteBuddyProxyConnection {
    public static void main(String[] args) {
        try {
            new ByteBuddy()
                .subclass(MyProxyConnection.class)
                .name(MyProxyConnection.class.getPackageName() + ".MyProxyConnection$ByteBuddy")
                .method(named("toString"))
                .intercept(FixedValue.nullValue())
                .make()
                .saveIn(new File("target/classes"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
