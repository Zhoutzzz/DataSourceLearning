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
import net.bytebuddy.dynamic.DynamicType;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author zhoutzzz
 */
public class ByteBuddyProxyConnection {
    public static void main(String[] args) {
        try {
//            Path pool = Files.createFile(Path.of("src/main/java/per/zhoutzzz/datasource/pool/MyProxyConnection$ByteBuddy.java"));
            DynamicType.Unloaded<MyProxyConnection> make = new ByteBuddy()
                .subclass(MyProxyConnection.class)
                .name(MyProxyConnection.class.getPackageName() + ".MyProxyConnection$ByteBuddy")
                .make();
            Object myProxyConnection$ByteBuddy = make
                .saveIn(new File("target/classes"));
//            make.load(MyProxyConnection.class.getClassLoader())
//                .getLoaded()
//                .getDeclaredConstructors()[0]
//                .newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
