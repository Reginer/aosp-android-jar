/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.layout.remote.util;

import com.android.tools.layoutlib.annotations.NotNull;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.imageio.ImageIO;

/**
 * Naive implementation of {@link SerializableImage} using {@link ImageIO} and PNG format as
 * transport.
 */
public class SerializableImageImpl implements SerializableImage {
    @NotNull
    transient private BufferedImage mBufferedImage;

    public SerializableImageImpl(@NotNull BufferedImage delegate) {
        mBufferedImage = delegate;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        ImageIO.write(mBufferedImage, "png", out);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        mBufferedImage = ImageIO.read(in);
    }

    @Override
    @NotNull
    public BufferedImage getBufferedImage() {
        return mBufferedImage;
    }
}
