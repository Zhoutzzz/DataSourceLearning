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
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.MethodDelegation;

import java.io.File;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.nameMatches;
import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * @author zhoutzzz
 */
public class ByteBuddyProxyConnection {
    public static void main(String[] args) {
        try {
            Map<TypeDescription, File> toString = new ByteBuddy()
                .subclass(MyProxyConnection.class)
                .name(MyProxyConnection.class.getPackageName() + ".MyProxyConnection_ByteBuddy")
                .method(named("toString"))
                .intercept(MethodDelegation.toConstructor(String.class))
                .make()
                .saveIn(new File("target/classes"));

            new ByteBuddy()
                .redefine(ConnectionFactory.class)
                .method(nameMatches("getConnection*"))
                .intercept(MethodDelegation.toConstructor(Class.forName(toString.keySet().iterator().next().getName())))
                .make()
                .saveIn(new File("target/classes"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
