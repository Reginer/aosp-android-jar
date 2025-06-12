/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.media;

import static android.annotation.SystemApi.Client.MODULE_LIBRARIES;

import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MediaMetadataRetriever class provides a unified interface for retrieving
 * frame and meta data from an input media file.
 */
public class MediaMetadataRetriever implements AutoCloseable {
    private static final String TAG = "MediaMetadataRetriever";

    // borrowed from ExoPlayer
    private static final String[] STANDARD_GENRES = new String[] {
            // These are the official ID3v1 genres.
            "Blues",
            "Classic Rock",
            "Country",
            "Dance",
            "Disco",
            "Funk",
            "Grunge",
            "Hip-Hop",
            "Jazz",
            "Metal",
            "New Age",
            "Oldies",
            "Other",
            "Pop",
            "R&B",
            "Rap",
            "Reggae",
            "Rock",
            "Techno",
            "Industrial",
            "Alternative",
            "Ska",
            "Death Metal",
            "Pranks",
            "Soundtrack",
            "Euro-Techno",
            "Ambient",
            "Trip-Hop",
            "Vocal",
            "Jazz+Funk",
            "Fusion",
            "Trance",
            "Classical",
            "Instrumental",
            "Acid",
            "House",
            "Game",
            "Sound Clip",
            "Gospel",
            "Noise",
            "AlternRock",
            "Bass",
            "Soul",
            "Punk",
            "Space",
            "Meditative",
            "Instrumental Pop",
            "Instrumental Rock",
            "Ethnic",
            "Gothic",
            "Darkwave",
            "Techno-Industrial",
            "Electronic",
            "Pop-Folk",
            "Eurodance",
            "Dream",
            "Southern Rock",
            "Comedy",
            "Cult",
            "Gangsta",
            "Top 40",
            "Christian Rap",
            "Pop/Funk",
            "Jungle",
            "Native American",
            "Cabaret",
            "New Wave",
            "Psychadelic",
            "Rave",
            "Showtunes",
            "Trailer",
            "Lo-Fi",
            "Tribal",
            "Acid Punk",
            "Acid Jazz",
            "Polka",
            "Retro",
            "Musical",
            "Rock & Roll",
            "Hard Rock",
            // These were made up by the authors of Winamp and later added to the ID3 spec.
            "Folk",
            "Folk-Rock",
            "National Folk",
            "Swing",
            "Fast Fusion",
            "Bebob",
            "Latin",
            "Revival",
            "Celtic",
            "Bluegrass",
            "Avantgarde",
            "Gothic Rock",
            "Progressive Rock",
            "Psychedelic Rock",
            "Symphonic Rock",
            "Slow Rock",
            "Big Band",
            "Chorus",
            "Easy Listening",
            "Acoustic",
            "Humour",
            "Speech",
            "Chanson",
            "Opera",
            "Chamber Music",
            "Sonata",
            "Symphony",
            "Booty Bass",
            "Primus",
            "Porn Groove",
            "Satire",
            "Slow Jam",
            "Club",
            "Tango",
            "Samba",
            "Folklore",
            "Ballad",
            "Power Ballad",
            "Rhythmic Soul",
            "Freestyle",
            "Duet",
            "Punk Rock",
            "Drum Solo",
            "A capella",
            "Euro-House",
            "Dance Hall",
            // These were made up by the authors of Winamp but have not been added to the ID3 spec.
            "Goa",
            "Drum & Bass",
            "Club-House",
            "Hardcore",
            "Terror",
            "Indie",
            "BritPop",
            "Afro-Punk",
            "Polsk Punk",
            "Beat",
            "Christian Gangsta Rap",
            "Heavy Metal",
            "Black Metal",
            "Crossover",
            "Contemporary Christian",
            "Christian Rock",
            "Merengue",
            "Salsa",
            "Thrash Metal",
            "Anime",
            "Jpop",
            "Synthpop"
    };

    static {
        System.loadLibrary("media_jni");
        native_init();
    }

    // The field below is accessed by native methods
    @SuppressWarnings("unused")
    private long mNativeContext;

    private static final int EMBEDDED_PICTURE_TYPE_ANY = 0xFFFF;

    public MediaMetadataRetriever() {
        native_setup();
    }

    /**
     * Sets the data source (file pathname) to use. Call this
     * method before the rest of the methods in this class. This method may be
     * time-consuming.
     *
     * @param path The path, or the URI (doesn't support streaming source currently)
     * of the input media file.
     * @throws IllegalArgumentException If the path is invalid.
     */
    public void setDataSource(String path) throws IllegalArgumentException {
        if (path == null) {
            throw new IllegalArgumentException("null path");
        }

        final Uri uri = Uri.parse(path);
        final String scheme = uri.getScheme();
        if ("file".equals(scheme)) {
            path = uri.getPath();
        } else if (scheme != null) {
            setDataSource(path, new HashMap<String, String>());
            return;
        }

        try (FileInputStream is = new FileInputStream(path)) {
            FileDescriptor fd = is.getFD();
            setDataSource(fd, 0, 0x7ffffffffffffffL);
        } catch (FileNotFoundException fileEx) {
            throw new IllegalArgumentException(path + " does not exist");
        } catch (IOException ioEx) {
            throw new IllegalArgumentException("couldn't open " + path);
        }
    }

    /**
     * Sets the data source (URI) to use. Call this
     * method before the rest of the methods in this class. This method may be
     * time-consuming.
     *
     * @param uri The URI of the input media.
     * @param headers the headers to be sent together with the request for the data
     * @throws IllegalArgumentException If the URI is invalid.
     */
    public void setDataSource(String uri,  Map<String, String> headers)
            throws IllegalArgumentException {
        int i = 0;
        String[] keys = new String[headers.size()];
        String[] values = new String[headers.size()];
        for (Map.Entry<String, String> entry: headers.entrySet()) {
            keys[i] = entry.getKey();
            values[i] = entry.getValue();
            ++i;
        }

        _setDataSource(
                MediaHTTPService.createHttpServiceBinderIfNecessary(uri),
                uri,
                keys,
                values);
    }

    private native void _setDataSource(
        IBinder httpServiceBinder, String uri, String[] keys, String[] values)
        throws IllegalArgumentException;

    /**
     * Sets the data source (FileDescriptor) to use.  It is the caller's
     * responsibility to close the file descriptor. It is safe to do so as soon
     * as this call returns. Call this method before the rest of the methods in
     * this class. This method may be time-consuming.
     *
     * @param fd the FileDescriptor for the file you want to play
     * @param offset the offset into the file where the data to be played starts,
     * in bytes. It must be non-negative
     * @param length the length in bytes of the data to be played. It must be
     * non-negative.
     * @throws IllegalArgumentException if the arguments are invalid
     */
    public void setDataSource(FileDescriptor fd, long offset, long length)
            throws IllegalArgumentException  {

        try (ParcelFileDescriptor modernFd = FileUtils.convertToModernFd(fd)) {
            if (modernFd == null) {
                _setDataSource(fd, offset, length);
            } else {
                _setDataSource(modernFd.getFileDescriptor(), offset, length);
            }
        } catch (IOException e) {
            Log.w(TAG, "Ignoring IO error while setting data source", e);
        }
    }

    private native void _setDataSource(FileDescriptor fd, long offset, long length)
            throws IllegalArgumentException;

    /**
     * Sets the data source (FileDescriptor) to use. It is the caller's
     * responsibility to close the file descriptor. It is safe to do so as soon
     * as this call returns. Call this method before the rest of the methods in
     * this class. This method may be time-consuming.
     *
     * @param fd the FileDescriptor for the file you want to play
     * @throws IllegalArgumentException if the FileDescriptor is invalid
     */
    public void setDataSource(FileDescriptor fd)
            throws IllegalArgumentException {
        // intentionally less than LONG_MAX
        setDataSource(fd, 0, 0x7ffffffffffffffL);
    }

    /**
     * Sets the data source as a content Uri. Call this method before
     * the rest of the methods in this class. This method may be time-consuming.
     *
     * @param context the Context to use when resolving the Uri
     * @param uri the Content URI of the data you want to play
     * @throws IllegalArgumentException if the Uri is invalid
     * @throws SecurityException if the Uri cannot be used due to lack of
     * permission.
     */
    public void setDataSource(Context context, Uri uri)
        throws IllegalArgumentException, SecurityException {
        if (uri == null) {
            throw new IllegalArgumentException("null uri");
        }

        String scheme = uri.getScheme();
        if(scheme == null || scheme.equals("file")) {
            setDataSource(uri.getPath());
            return;
        }

        AssetFileDescriptor fd = null;
        try {
            ContentResolver resolver = context.getContentResolver();
            try {
                boolean optimize =
                        SystemProperties.getBoolean("fuse.sys.transcode_retriever_optimize", false);
                Bundle opts = new Bundle();
                opts.putBoolean("android.provider.extra.ACCEPT_ORIGINAL_MEDIA_FORMAT", true);
                fd = optimize ? resolver.openTypedAssetFileDescriptor(uri, "*/*", opts)
                        : resolver.openAssetFileDescriptor(uri, "r");
            } catch(FileNotFoundException e) {
                throw new IllegalArgumentException("could not access " + uri);
            }
            if (fd == null) {
                throw new IllegalArgumentException("got null FileDescriptor for " + uri);
            }
            FileDescriptor descriptor = fd.getFileDescriptor();
            if (!descriptor.valid()) {
                throw new IllegalArgumentException("got invalid FileDescriptor for " + uri);
            }
            // Note: using getDeclaredLength so that our behavior is the same
            // as previous versions when the content provider is returning
            // a full file.
            if (fd.getDeclaredLength() < 0) {
                setDataSource(descriptor);
            } else {
                setDataSource(descriptor, fd.getStartOffset(), fd.getDeclaredLength());
            }
            return;
        } catch (SecurityException ex) {
        } finally {
            try {
                if (fd != null) {
                    fd.close();
                }
            } catch(IOException ioEx) {
            }
        }
        setDataSource(uri.toString());
    }

    /**
     * Sets the data source (MediaDataSource) to use.
     *
     * @param dataSource the MediaDataSource for the media you want to play
     */
    public void setDataSource(MediaDataSource dataSource)
            throws IllegalArgumentException {
        _setDataSource(dataSource);
    }

    private native void _setDataSource(MediaDataSource dataSource)
          throws IllegalArgumentException;

    private native @Nullable String nativeExtractMetadata(int keyCode);

    /**
     * Call this method after setDataSource(). This method retrieves the
     * meta data value associated with the keyCode.
     *
     * The keyCode currently supported is listed below as METADATA_XXX
     * constants. With any other value, it returns a null pointer.
     *
     * @param keyCode One of the constants listed below at the end of the class.
     * @return The meta data value associate with the given keyCode on success;
     * null on failure.
     */
    public @Nullable String extractMetadata(int keyCode) {
        String meta = nativeExtractMetadata(keyCode);
        if (keyCode == METADATA_KEY_GENRE) {
            // translate numeric genre code(s) to human readable
            meta = convertGenreTag(meta);
        }
        return meta;
    }

    /*
     * The id3v2 spec doesn't specify the syntax of the genre tag very precisely, so
     * some assumptions are made. Using one possible interpretation of the id3v2
     * spec, this method converts an id3 genre tag string to a human readable string,
     * as follows:
     * - if the first character of the tag is a digit, the entire tag is assumed to
     *   be an id3v1 numeric genre code. If the tag does not parse to a number, or
     *   the number is outside the range of defined standard genres, it is ignored.
     * - if the tag does not start with a digit, it is assumed to be an id3v2 style
     *   tag consisting of one or more genres, with each genre being either a parenthesized
     *   integer referring to an id3v1 numeric genre code, the special indicators "(CR)" or
     *   "(RX)" (for "Cover" or "Remix", respectively), or a custom genre string. When
     *   a custom genre string is encountered, it is assumed to continue until the end
     *   of the tag, unless it starts with "((" in which case it is assumed to continue
     *   until the next close-parenthesis or the end of the tag. Any parse error in the tag
     *   causes it to be ignored.
     * The human-readable genre string is not localized, and uses the English genre names
     * from the spec.
     */
    private String convertGenreTag(String meta) {
        if (TextUtils.isEmpty(meta)) {
            return null;
        }

        if (Character.isDigit(meta.charAt(0))) {
            // assume a single id3v1-style bare number without any extra characters
            try {
                int genreIndex = Integer.parseInt(meta);
                if (genreIndex >= 0 && genreIndex < STANDARD_GENRES.length) {
                    return STANDARD_GENRES[genreIndex];
                }
            } catch (NumberFormatException e) {
                // ignore and fall through
            }
            return null;
        } else {
            // assume id3v2-style genre tag, with parenthesized numeric genres
            // and/or literal genre strings, possibly more than one per tag.
            StringBuilder genres = null;
            String nextGenre = null;
            while (true) {
                if (!TextUtils.isEmpty(nextGenre)) {
                    if (genres == null) {
                        genres = new StringBuilder();
                    }
                    if (genres.length() != 0) {
                        genres.append(", ");
                    }
                    genres.append(nextGenre);
                    nextGenre = null;
                }
                if (TextUtils.isEmpty(meta)) {
                    // entire tag has been processed.
                    break;
                }
                if (meta.startsWith("(RX)")) {
                    nextGenre = "Remix";
                    meta = meta.substring(4);
                } else if (meta.startsWith("(CR)")) {
                    nextGenre = "Cover";
                    meta = meta.substring(4);
                } else if (meta.startsWith("((")) {
                    // the id3v2 spec says that custom genres that start with a parenthesis
                    // should be "escaped" with another parenthesis, however the spec doesn't
                    // specify escaping parentheses inside the custom string. We'll parse any
                    // such strings until a closing parenthesis is found, or the end of
                    // the tag is reached.
                    int closeParenOffset = meta.indexOf(')');
                    if (closeParenOffset == -1) {
                        // string continues to end of tag
                        nextGenre = meta.substring(1);
                        meta = "";
                    } else {
                        nextGenre = meta.substring(1, closeParenOffset + 1);
                        meta = meta.substring(closeParenOffset + 1);
                    }
                } else if (meta.startsWith("(")) {
                    // should be a parenthesized numeric genre
                    int closeParenOffset = meta.indexOf(')');
                    if (closeParenOffset == -1) {
                        return null;
                    }
                    String genreNumString = meta.substring(1, closeParenOffset);
                    try {
                        int genreIndex = Integer.parseInt(genreNumString.toString());
                        if (genreIndex >= 0 && genreIndex < STANDARD_GENRES.length) {
                            nextGenre = STANDARD_GENRES[genreIndex];
                        } else {
                            return null;
                        }
                    } catch (NumberFormatException e) {
                        return null;
                    }
                    meta = meta.substring(closeParenOffset + 1);
                } else {
                    // custom genre
                    nextGenre = meta;
                    meta = "";
                }
            }
            return genres == null || genres.length() == 0 ? null : genres.toString();
        }
    }

    /**
     * This method is similar to {@link #getFrameAtTime(long, int, BitmapParams)}
     * except that the device will choose the actual {@link Bitmap.Config} to use.
     *
     * @param timeUs The time position where the frame will be retrieved.
     * When retrieving the frame at the given time position, there is no
     * guarantee that the data source has a frame located at the position.
     * When this happens, a frame nearby will be returned. If timeUs is
     * negative, time position and option will ignored, and any frame
     * that the implementation considers as representative may be returned.
     *
     * @param option a hint on how the frame is found. Use
     * {@link #OPTION_PREVIOUS_SYNC} if one wants to retrieve a sync frame
     * that has a timestamp earlier than or the same as timeUs. Use
     * {@link #OPTION_NEXT_SYNC} if one wants to retrieve a sync frame
     * that has a timestamp later than or the same as timeUs. Use
     * {@link #OPTION_CLOSEST_SYNC} if one wants to retrieve a sync frame
     * that has a timestamp closest to or the same as timeUs. Use
     * {@link #OPTION_CLOSEST} if one wants to retrieve a frame that may
     * or may not be a sync frame but is closest to or the same as timeUs.
     * {@link #OPTION_CLOSEST} often has larger performance overhead compared
     * to the other options if there is no sync frame located at timeUs.
     *
     * @return A Bitmap containing a representative video frame, which can be null,
     *         if such a frame cannot be retrieved. {@link Bitmap#getConfig()} can
     *         be used to query the actual {@link Bitmap.Config}.
     *
     * @see #getFrameAtTime(long, int, BitmapParams)
     */
    public @Nullable Bitmap getFrameAtTime(long timeUs, @Option int option) {
        if (option < OPTION_PREVIOUS_SYNC ||
            option > OPTION_CLOSEST) {
            throw new IllegalArgumentException("Unsupported option: " + option);
        }

        return _getFrameAtTime(timeUs, option, -1 /*dst_width*/, -1 /*dst_height*/, null);
    }

    /**
     * Call this method after setDataSource(). This method finds a
     * representative frame close to the given time position by considering
     * the given option if possible, and returns it as a bitmap.
     *
     * <p>If you don't need a full-resolution
     * frame (for example, because you need a thumbnail image), use
     * {@link #getScaledFrameAtTime getScaledFrameAtTime()} instead of this
     * method.</p>
     *
     * @param timeUs The time position where the frame will be retrieved.
     * When retrieving the frame at the given time position, there is no
     * guarantee that the data source has a frame located at the position.
     * When this happens, a frame nearby will be returned. If timeUs is
     * negative, time position and option will ignored, and any frame
     * that the implementation considers as representative may be returned.
     *
     * @param option a hint on how the frame is found. Use
     * {@link #OPTION_PREVIOUS_SYNC} if one wants to retrieve a sync frame
     * that has a timestamp earlier than or the same as timeUs. Use
     * {@link #OPTION_NEXT_SYNC} if one wants to retrieve a sync frame
     * that has a timestamp later than or the same as timeUs. Use
     * {@link #OPTION_CLOSEST_SYNC} if one wants to retrieve a sync frame
     * that has a timestamp closest to or the same as timeUs. Use
     * {@link #OPTION_CLOSEST} if one wants to retrieve a frame that may
     * or may not be a sync frame but is closest to or the same as timeUs.
     * {@link #OPTION_CLOSEST} often has larger performance overhead compared
     * to the other options if there is no sync frame located at timeUs.
     *
     * @param params BitmapParams that controls the returned bitmap config
     *        (such as pixel formats).
     *
     * @return A Bitmap containing a representative video frame, which
     *         can be null, if such a frame cannot be retrieved.
     *
     * @see #getFrameAtTime(long, int)
     */
    public @Nullable Bitmap getFrameAtTime(
            long timeUs, @Option int option, @NonNull BitmapParams params) {
        if (option < OPTION_PREVIOUS_SYNC ||
            option > OPTION_CLOSEST) {
            throw new IllegalArgumentException("Unsupported option: " + option);
        }

        return _getFrameAtTime(timeUs, option, -1 /*dst_width*/, -1 /*dst_height*/, params);
    }

    /**
     * This method is similar to {@link #getScaledFrameAtTime(long, int, int, int, BitmapParams)}
     * except that the device will choose the actual {@link Bitmap.Config} to use.
     *
     * @param timeUs The time position in microseconds where the frame will be retrieved.
     * When retrieving the frame at the given time position, there is no
     * guarantee that the data source has a frame located at the position.
     * When this happens, a frame nearby will be returned. If timeUs is
     * negative, time position and option will ignored, and any frame
     * that the implementation considers as representative may be returned.
     *
     * @param option a hint on how the frame is found. Use
     * {@link #OPTION_PREVIOUS_SYNC} if one wants to retrieve a sync frame
     * that has a timestamp earlier than or the same as timeUs. Use
     * {@link #OPTION_NEXT_SYNC} if one wants to retrieve a sync frame
     * that has a timestamp later than or the same as timeUs. Use
     * {@link #OPTION_CLOSEST_SYNC} if one wants to retrieve a sync frame
     * that has a timestamp closest to or the same as timeUs. Use
     * {@link #OPTION_CLOSEST} if one wants to retrieve a frame that may
     * or may not be a sync frame but is closest to or the same as timeUs.
     * {@link #OPTION_CLOSEST} often has larger performance overhead compared
     * to the other options if there is no sync frame located at timeUs.
     *
     * @param dstWidth expected output bitmap width
     * @param dstHeight expected output bitmap height
     * @return A Bitmap containing a representative video frame, which can be null,
     *         if such a frame cannot be retrieved. {@link Bitmap#getConfig()} can
     *         be used to query the actual {@link Bitmap.Config}.
     * @throws IllegalArgumentException if passed in invalid option or width by height
     *         is less than or equal to 0.
     * @see #getScaledFrameAtTime(long, int, int, int, BitmapParams)
     */
    public @Nullable Bitmap getScaledFrameAtTime(long timeUs, @Option int option,
            @IntRange(from=1) int dstWidth, @IntRange(from=1) int dstHeight) {
        validate(option, dstWidth, dstHeight);
        return _getFrameAtTime(timeUs, option, dstWidth, dstHeight, null);
    }

    /**
     * Retrieve a video frame near a given timestamp scaled to a desired size.
     * Call this method after setDataSource(). This method finds a representative
     * frame close to the given time position by considering the given option
     * if possible, and returns it as a bitmap with same aspect ratio as the source
     * while scaling it so that it fits into the desired size of dst_width by dst_height.
     * This is useful for generating a thumbnail for an input data source or just to
     * obtain a scaled frame at the given time position.
     *
     * @param timeUs The time position in microseconds where the frame will be retrieved.
     * When retrieving the frame at the given time position, there is no
     * guarantee that the data source has a frame located at the position.
     * When this happens, a frame nearby will be returned. If timeUs is
     * negative, time position and option will ignored, and any frame
     * that the implementation considers as representative may be returned.
     *
     * @param option a hint on how the frame is found. Use
     * {@link #OPTION_PREVIOUS_SYNC} if one wants to retrieve a sync frame
     * that has a timestamp earlier than or the same as timeUs. Use
     * {@link #OPTION_NEXT_SYNC} if one wants to retrieve a sync frame
     * that has a timestamp later than or the same as timeUs. Use
     * {@link #OPTION_CLOSEST_SYNC} if one wants to retrieve a sync frame
     * that has a timestamp closest to or the same as timeUs. Use
     * {@link #OPTION_CLOSEST} if one wants to retrieve a frame that may
     * or may not be a sync frame but is closest to or the same as timeUs.
     * {@link #OPTION_CLOSEST} often has larger performance overhead compared
     * to the other options if there is no sync frame located at timeUs.
     *
     * @param dstWidth expected output bitmap width
     * @param dstHeight expected output bitmap height
     * @param params BitmapParams that controls the returned bitmap config
     *        (such as pixel formats).
     *
     * @return A Bitmap of size not larger than dstWidth by dstHeight containing a
     *         scaled video frame, which can be null, if such a frame cannot be retrieved.
     * @throws IllegalArgumentException if passed in invalid option or width by height
     *         is less than or equal to 0.
     * @see #getScaledFrameAtTime(long, int, int, int)
     */
    public @Nullable Bitmap getScaledFrameAtTime(long timeUs, @Option int option,
            @IntRange(from=1) int dstWidth, @IntRange(from=1) int dstHeight,
            @NonNull BitmapParams params) {
        validate(option, dstWidth, dstHeight);
        return _getFrameAtTime(timeUs, option, dstWidth, dstHeight, params);
    }

    private void validate(@Option int option, int dstWidth, int dstHeight) {
        if (option < OPTION_PREVIOUS_SYNC || option > OPTION_CLOSEST) {
            throw new IllegalArgumentException("Unsupported option: " + option);
        }
        if (dstWidth <= 0) {
            throw new IllegalArgumentException("Invalid width: " + dstWidth);
        }
        if (dstHeight <= 0) {
            throw new IllegalArgumentException("Invalid height: " + dstHeight);
        }
    }

    /**
     * Call this method after setDataSource(). This method finds a
     * representative frame close to the given time position if possible,
     * and returns it as a bitmap. Call this method if one does not care
     * how the frame is found as long as it is close to the given time;
     * otherwise, please call {@link #getFrameAtTime(long, int)}.
     *
     * <p>If you don't need a full-resolution
     * frame (for example, because you need a thumbnail image), use
     * {@link #getScaledFrameAtTime getScaledFrameAtTime()} instead of this
     * method.</p>
     *
     * @param timeUs The time position where the frame will be retrieved.
     * When retrieving the frame at the given time position, there is no
     * guarentee that the data source has a frame located at the position.
     * When this happens, a frame nearby will be returned. If timeUs is
     * negative, time position and option will ignored, and any frame
     * that the implementation considers as representative may be returned.
     *
     * @return A Bitmap of size dst_widthxdst_height containing a representative
     *         video frame, which can be null, if such a frame cannot be retrieved.
     *
     * @see #getFrameAtTime(long, int)
     */
    public @Nullable Bitmap getFrameAtTime(long timeUs) {
        return getFrameAtTime(timeUs, OPTION_CLOSEST_SYNC);
    }

    /**
     * Call this method after setDataSource(). This method finds a
     * representative frame at any time position if possible,
     * and returns it as a bitmap. Call this method if one does not
     * care about where the frame is located; otherwise, please call
     * {@link #getFrameAtTime(long)} or {@link #getFrameAtTime(long, int)}
     *
     * <p>If you don't need a full-resolution
     * frame (for example, because you need a thumbnail image), use
     * {@link #getScaledFrameAtTime getScaledFrameAtTime()} instead of this
     * method.</p>
     *
     * @return A Bitmap containing a representative video frame, which
     *         can be null, if such a frame cannot be retrieved.
     *
     * @see #getFrameAtTime(long)
     * @see #getFrameAtTime(long, int)
     */
    public @Nullable Bitmap getFrameAtTime() {
        return _getFrameAtTime(
                -1, OPTION_CLOSEST_SYNC, -1 /*dst_width*/, -1 /*dst_height*/, null);
    }

    private native Bitmap _getFrameAtTime(
            long timeUs, int option, int width, int height, @Nullable BitmapParams params);

    public static final class BitmapParams {
        private Bitmap.Config inPreferredConfig = Bitmap.Config.ARGB_8888;
        private Bitmap.Config outActualConfig = Bitmap.Config.ARGB_8888;

        /**
         * Create a default BitmapParams object. By default, it uses {@link Bitmap.Config#ARGB_8888}
         * as the preferred bitmap config.
         */
        public BitmapParams() {}

        /**
         * Set the preferred bitmap config for the decoder to decode into.
         *
         * If not set, or the request cannot be met, the decoder will output
         * in {@link Bitmap.Config#ARGB_8888} config by default.
         *
         * After decode, the actual config used can be retrieved by {@link #getActualConfig()}.
         *
         * @param config the preferred bitmap config to use.
         */
        public void setPreferredConfig(@NonNull Bitmap.Config config) {
            if (config == null) {
                throw new IllegalArgumentException("preferred config can't be null");
            }
            inPreferredConfig = config;
        }

        /**
         * Retrieve the preferred bitmap config in the params.
         *
         * @return the preferred bitmap config.
         */
        public @NonNull Bitmap.Config getPreferredConfig() {
            return inPreferredConfig;
        }

        /**
         * Get the actual bitmap config used to decode the bitmap after the decoding.
         *
         * @return the actual bitmap config used.
         */
        public @NonNull Bitmap.Config getActualConfig() {
            return outActualConfig;
        }
    }

    /**
     * This method retrieves a video frame by its index. It should only be called
     * after {@link #setDataSource}.
     *
     * After the bitmap is returned, you can query the actual parameters that were
     * used to create the bitmap from the {@code BitmapParams} argument, for instance
     * to query the bitmap config used for the bitmap with {@link BitmapParams#getActualConfig}.
     *
     * @param frameIndex 0-based index of the video frame. The frame index must be that of
     *        a valid frame. The total number of frames available for retrieval can be queried
     *        via the {@link #METADATA_KEY_VIDEO_FRAME_COUNT} key.
     * @param params BitmapParams that controls the returned bitmap config (such as pixel formats).
     *
     * @throws IllegalStateException if the container doesn't contain video or image sequences.
     * @throws IllegalArgumentException if the requested frame index does not exist.
     *
     * @return A Bitmap containing the requested video frame, or null if the retrieval fails.
     *
     * @see #getFrameAtIndex(int)
     * @see #getFramesAtIndex(int, int, BitmapParams)
     * @see #getFramesAtIndex(int, int)
     */
    public @Nullable Bitmap getFrameAtIndex(int frameIndex, @NonNull BitmapParams params) {
        List<Bitmap> bitmaps = getFramesAtIndex(frameIndex, 1, params);
        return bitmaps.get(0);
    }

    /**
     * This method is similar to {@link #getFrameAtIndex(int, BitmapParams)} except that
     * the default for {@link BitmapParams} will be used.
     *
     * @param frameIndex 0-based index of the video frame. The frame index must be that of
     *        a valid frame. The total number of frames available for retrieval can be queried
     *        via the {@link #METADATA_KEY_VIDEO_FRAME_COUNT} key.
     *
     * @throws IllegalStateException if the container doesn't contain video or image sequences.
     * @throws IllegalArgumentException if the requested frame index does not exist.
     *
     * @return A Bitmap containing the requested video frame, or null if the retrieval fails.
     *
     * @see #getFrameAtIndex(int, BitmapParams)
     * @see #getFramesAtIndex(int, int, BitmapParams)
     * @see #getFramesAtIndex(int, int)
     */
    public @Nullable Bitmap getFrameAtIndex(int frameIndex) {
        List<Bitmap> bitmaps = getFramesAtIndex(frameIndex, 1);
        return bitmaps.get(0);
    }

    /**
     * This method retrieves a consecutive set of video frames starting at the
     * specified index. It should only be called after {@link #setDataSource}.
     *
     * If the caller intends to retrieve more than one consecutive video frames,
     * this method is preferred over {@link #getFrameAtIndex(int, BitmapParams)} for efficiency.
     *
     * After the bitmaps are returned, you can query the actual parameters that were
     * used to create the bitmaps from the {@code BitmapParams} argument, for instance
     * to query the bitmap config used for the bitmaps with {@link BitmapParams#getActualConfig}.
     *
     * @param frameIndex 0-based index of the first video frame to retrieve. The frame index
     *        must be that of a valid frame. The total number of frames available for retrieval
     *        can be queried via the {@link #METADATA_KEY_VIDEO_FRAME_COUNT} key.
     * @param numFrames number of consecutive video frames to retrieve. Must be a positive
     *        value. The stream must contain at least numFrames frames starting at frameIndex.
     * @param params BitmapParams that controls the returned bitmap config (such as pixel formats).
     *
     * @throws IllegalStateException if the container doesn't contain video or image sequences.
     * @throws IllegalArgumentException if the frameIndex or numFrames is invalid, or the
     *         stream doesn't contain at least numFrames starting at frameIndex.

     * @return An list of Bitmaps containing the requested video frames. The returned
     *         array could contain less frames than requested if the retrieval fails.
     *
     * @see #getFrameAtIndex(int, BitmapParams)
     * @see #getFrameAtIndex(int)
     * @see #getFramesAtIndex(int, int)
     */
    public @NonNull List<Bitmap> getFramesAtIndex(
            int frameIndex, int numFrames, @NonNull BitmapParams params) {
        return getFramesAtIndexInternal(frameIndex, numFrames, params);
    }

    /**
     * This method is similar to {@link #getFramesAtIndex(int, int, BitmapParams)} except that
     * the default for {@link BitmapParams} will be used.
     *
     * @param frameIndex 0-based index of the first video frame to retrieve. The frame index
     *        must be that of a valid frame. The total number of frames available for retrieval
     *        can be queried via the {@link #METADATA_KEY_VIDEO_FRAME_COUNT} key.
     * @param numFrames number of consecutive video frames to retrieve. Must be a positive
     *        value. The stream must contain at least numFrames frames starting at frameIndex.
     *
     * @throws IllegalStateException if the container doesn't contain video or image sequences.
     * @throws IllegalArgumentException if the frameIndex or numFrames is invalid, or the
     *         stream doesn't contain at least numFrames starting at frameIndex.

     * @return An list of Bitmaps containing the requested video frames. The returned
     *         array could contain less frames than requested if the retrieval fails.
     *
     * @see #getFrameAtIndex(int, BitmapParams)
     * @see #getFrameAtIndex(int)
     * @see #getFramesAtIndex(int, int, BitmapParams)
     */
    public @NonNull List<Bitmap> getFramesAtIndex(int frameIndex, int numFrames) {
        return getFramesAtIndexInternal(frameIndex, numFrames, null);
    }

    private @NonNull List<Bitmap> getFramesAtIndexInternal(
            int frameIndex, int numFrames, @Nullable BitmapParams params) {
        if (!"yes".equals(extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO))) {
            throw new IllegalStateException("Does not contain video or image sequences");
        }
        int frameCount = Integer.parseInt(
                extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT));
        if (frameIndex < 0 || numFrames < 1
                || frameIndex >= frameCount
                || frameIndex > frameCount - numFrames) {
            throw new IllegalArgumentException("Invalid frameIndex or numFrames: "
                + frameIndex + ", " + numFrames);
        }
        return _getFrameAtIndex(frameIndex, numFrames, params);
    }

    private native @NonNull List<Bitmap> _getFrameAtIndex(
            int frameIndex, int numFrames, @Nullable BitmapParams params);

    /**
     * This method retrieves a still image by its index. It should only be called
     * after {@link #setDataSource}.
     *
     * After the bitmap is returned, you can query the actual parameters that were
     * used to create the bitmap from the {@code BitmapParams} argument, for instance
     * to query the bitmap config used for the bitmap with {@link BitmapParams#getActualConfig}.
     *
     * @param imageIndex 0-based index of the image.
     * @param params BitmapParams that controls the returned bitmap config (such as pixel formats).
     *
     * @throws IllegalStateException if the container doesn't contain still images.
     * @throws IllegalArgumentException if the requested image does not exist.
     *
     * @return the requested still image, or null if the image cannot be retrieved.
     *
     * @see #getImageAtIndex(int)
     * @see #getPrimaryImage(BitmapParams)
     * @see #getPrimaryImage()
     */
    public @Nullable Bitmap getImageAtIndex(int imageIndex, @NonNull BitmapParams params) {
        return getImageAtIndexInternal(imageIndex, params);
    }

    /**
     * @hide
     *
     * This method retrieves the thumbnail image for a still image if it's available.
     * It should only be called after {@link #setDataSource}.
     *
     * @param imageIndex 0-based index of the image, negative value indicates primary image.
     * @param params BitmapParams that controls the returned bitmap config (such as pixel formats).
     * @param targetSize intended size of one edge (wdith or height) of the thumbnail,
     *                   this is a heuristic for the framework to decide whether the embedded
     *                   thumbnail should be used.
     * @param maxPixels maximum pixels of thumbnail, this is a heuristic for the frameowrk to
     *                  decide whehther the embedded thumnbail (or a downscaled version of it)
     *                  should be used.
     * @return the retrieved thumbnail, or null if no suitable thumbnail is available.
     */
    public native @Nullable Bitmap getThumbnailImageAtIndex(
            int imageIndex, @NonNull BitmapParams params, int targetSize, int maxPixels);

    /**
     * This method is similar to {@link #getImageAtIndex(int, BitmapParams)} except that
     * the default for {@link BitmapParams} will be used.
     *
     * @param imageIndex 0-based index of the image.
     *
     * @throws IllegalStateException if the container doesn't contain still images.
     * @throws IllegalArgumentException if the requested image does not exist.
     *
     * @return the requested still image, or null if the image cannot be retrieved.
     *
     * @see #getImageAtIndex(int, BitmapParams)
     * @see #getPrimaryImage(BitmapParams)
     * @see #getPrimaryImage()
     */
    public @Nullable Bitmap getImageAtIndex(int imageIndex) {
        return getImageAtIndexInternal(imageIndex, null);
    }

    /**
     * This method retrieves the primary image of the media content. It should only
     * be called after {@link #setDataSource}.
     *
     * After the bitmap is returned, you can query the actual parameters that were
     * used to create the bitmap from the {@code BitmapParams} argument, for instance
     * to query the bitmap config used for the bitmap with {@link BitmapParams#getActualConfig}.
     *
     * @param params BitmapParams that controls the returned bitmap config (such as pixel formats).
     *
     * @return the primary image, or null if it cannot be retrieved.
     *
     * @throws IllegalStateException if the container doesn't contain still images.
     *
     * @see #getImageAtIndex(int, BitmapParams)
     * @see #getImageAtIndex(int)
     * @see #getPrimaryImage()
     */
    public @Nullable Bitmap getPrimaryImage(@NonNull BitmapParams params) {
        return getImageAtIndexInternal(-1, params);
    }

    /**
     * This method is similar to {@link #getPrimaryImage(BitmapParams)} except that
     * the default for {@link BitmapParams} will be used.
     *
     * @return the primary image, or null if it cannot be retrieved.
     *
     * @throws IllegalStateException if the container doesn't contain still images.
     *
     * @see #getImageAtIndex(int, BitmapParams)
     * @see #getImageAtIndex(int)
     * @see #getPrimaryImage(BitmapParams)
     */
    public @Nullable Bitmap getPrimaryImage() {
        return getImageAtIndexInternal(-1, null);
    }

    private Bitmap getImageAtIndexInternal(int imageIndex, @Nullable BitmapParams params) {
        if (!"yes".equals(extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_IMAGE))) {
            throw new IllegalStateException("Does not contain still images");
        }

        String imageCount = extractMetadata(MediaMetadataRetriever.METADATA_KEY_IMAGE_COUNT);
        if (imageIndex >= Integer.parseInt(imageCount)) {
            throw new IllegalArgumentException("Invalid image index: " + imageCount);
        }

        return _getImageAtIndex(imageIndex, params);
    }

    private native Bitmap _getImageAtIndex(int imageIndex, @Nullable BitmapParams params);

    /**
     * Call this method after setDataSource(). This method finds the optional
     * graphic or album/cover art associated associated with the data source. If
     * there are more than one pictures, (any) one of them is returned.
     *
     * @return null if no such graphic is found.
     */
    public @Nullable byte[] getEmbeddedPicture() {
        return getEmbeddedPicture(EMBEDDED_PICTURE_TYPE_ANY);
    }

    @UnsupportedAppUsage
    private native byte[] getEmbeddedPicture(int pictureType);

    /**
     * Releases any acquired resources. Call it when done with the object.
     *
     * @throws IOException When an {@link IOException} is thrown while closing a {@link
     * MediaDataSource} passed to {@link #setDataSource(MediaDataSource)}. This throws clause exists
     * since API {@link android.os.Build.VERSION_CODES#TIRAMISU}, but this method can throw in
     * earlier API versions where the exception is not declared.
     */
    @Override
    public void close() throws IOException {
        release();
    }

    /**
     * Releases any acquired resources. Call it when done with the object.
     *
     * @throws IOException When an {@link IOException} is thrown while closing a {@link
     * MediaDataSource} passed to {@link #setDataSource(MediaDataSource)}. This throws clause exists
     * since API {@link android.os.Build.VERSION_CODES#TIRAMISU}, but this method can throw in
     * earlier API versions where the exception is not declared.
     */
    public native void release() throws IOException;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P, trackingBug = 115609023)
    private native void native_setup();
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P, trackingBug = 115609023)
    private static native void native_init();

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P, trackingBug = 115609023)
    private native final void native_finalize();

    @Override
    protected void finalize() throws Throwable {
        try {
            native_finalize();
        } finally {
            super.finalize();
        }
    }

    /**
     * Option used in method {@link #getFrameAtTime(long, int)} to get a
     * frame at a specified location.
     *
     * @see #getFrameAtTime(long, int)
     */
    /* Do not change these option values without updating their counterparts
     * in include/media/MediaSource.h!
     */
    /**
     * This option is used with {@link #getFrameAtTime(long, int)} to retrieve
     * a sync (or key) frame associated with a data source that is located
     * right before or at the given time.
     *
     * @see #getFrameAtTime(long, int)
     */
    public static final int OPTION_PREVIOUS_SYNC    = 0x00;
    /**
     * This option is used with {@link #getFrameAtTime(long, int)} to retrieve
     * a sync (or key) frame associated with a data source that is located
     * right after or at the given time.
     *
     * @see #getFrameAtTime(long, int)
     */
    public static final int OPTION_NEXT_SYNC        = 0x01;
    /**
     * This option is used with {@link #getFrameAtTime(long, int)} to retrieve
     * a sync (or key) frame associated with a data source that is located
     * closest to (in time) or at the given time.
     *
     * @see #getFrameAtTime(long, int)
     */
    public static final int OPTION_CLOSEST_SYNC     = 0x02;
    /**
     * This option is used with {@link #getFrameAtTime(long, int)} to retrieve
     * a frame (not necessarily a key frame) associated with a data source that
     * is located closest to or at the given time.
     *
     * @see #getFrameAtTime(long, int)
     */
    public static final int OPTION_CLOSEST          = 0x03;

    /** @hide */
    @IntDef(flag = false, prefix = { "OPTION_" }, value = {
            OPTION_PREVIOUS_SYNC,
            OPTION_NEXT_SYNC,
            OPTION_CLOSEST_SYNC,
            OPTION_CLOSEST,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Option {}

    /*
     * Do not change these metadata key values without updating their
     * counterparts in include/media/mediametadataretriever.h!
     */
    /**
     * The metadata key to retrieve the numeric string describing the
     * order of the audio data source on its original recording.
     */
    public static final int METADATA_KEY_CD_TRACK_NUMBER = 0;
    /**
     * The metadata key to retrieve the information about the album title
     * of the data source.
     */
    public static final int METADATA_KEY_ALBUM           = 1;
    /**
     * The metadata key to retrieve the information about the artist of
     * the data source.
     */
    public static final int METADATA_KEY_ARTIST          = 2;
    /**
     * The metadata key to retrieve the information about the author of
     * the data source.
     */
    public static final int METADATA_KEY_AUTHOR          = 3;
    /**
     * The metadata key to retrieve the information about the composer of
     * the data source.
     */
    public static final int METADATA_KEY_COMPOSER        = 4;
    /**
     * The metadata key to retrieve the date when the data source was created
     * or modified.
     */
    public static final int METADATA_KEY_DATE            = 5;
    /**
     * The metadata key to retrieve the content type or genre of the data
     * source.
     */
    public static final int METADATA_KEY_GENRE           = 6;
    /**
     * The metadata key to retrieve the data source title.
     */
    public static final int METADATA_KEY_TITLE           = 7;
    /**
     * The metadata key to retrieve the year when the data source was created
     * or modified.
     */
    public static final int METADATA_KEY_YEAR            = 8;
    /**
     * The metadata key to retrieve the playback duration (in ms) of the data source.
     */
    public static final int METADATA_KEY_DURATION        = 9;
    /**
     * The metadata key to retrieve the number of tracks, such as audio, video,
     * text, in the data source, such as a mp4 or 3gpp file.
     */
    public static final int METADATA_KEY_NUM_TRACKS      = 10;
    /**
     * The metadata key to retrieve the information of the writer (such as
     * lyricist) of the data source.
     */
    public static final int METADATA_KEY_WRITER          = 11;
    /**
     * The metadata key to retrieve the mime type of the data source. Some
     * example mime types include: "video/mp4", "audio/mp4", "audio/amr-wb",
     * etc.
     */
    public static final int METADATA_KEY_MIMETYPE        = 12;
    /**
     * The metadata key to retrieve the information about the performers or
     * artist associated with the data source.
     */
    public static final int METADATA_KEY_ALBUMARTIST     = 13;
    /**
     * The metadata key to retrieve the numberic string that describes which
     * part of a set the audio data source comes from.
     */
    public static final int METADATA_KEY_DISC_NUMBER     = 14;
    /**
     * The metadata key to retrieve the music album compilation status.
     */
    public static final int METADATA_KEY_COMPILATION     = 15;
    /**
     * If this key exists the media contains audio content.
     */
    public static final int METADATA_KEY_HAS_AUDIO       = 16;
    /**
     * If this key exists the media contains video content.
     */
    public static final int METADATA_KEY_HAS_VIDEO       = 17;
    /**
     * If the media contains video, this key retrieves its width.
     */
    public static final int METADATA_KEY_VIDEO_WIDTH     = 18;
    /**
     * If the media contains video, this key retrieves its height.
     */
    public static final int METADATA_KEY_VIDEO_HEIGHT    = 19;
    /**
     * This key retrieves the average bitrate (in bits/sec), if available.
     */
    public static final int METADATA_KEY_BITRATE         = 20;
    /**
     * This key retrieves the language code of text tracks, if available.
     * If multiple text tracks present, the return value will look like:
     * "eng:chi"
     * @hide
     */
    public static final int METADATA_KEY_TIMED_TEXT_LANGUAGES      = 21;
    /**
     * If this key exists the media is drm-protected.
     * @hide
     */
    public static final int METADATA_KEY_IS_DRM          = 22;
    /**
     * This key retrieves the location information, if available.
     * The location should be specified according to ISO-6709 standard, under
     * a mp4/3gp box "@xyz". Location with longitude of -90 degrees and latitude
     * of 180 degrees will be retrieved as "+180.0000-90.0000/", for instance.
     */
    public static final int METADATA_KEY_LOCATION        = 23;
    /**
     * This key retrieves the video rotation angle in degrees, if available.
     * The video rotation angle may be 0, 90, 180, or 270 degrees.
     */
    public static final int METADATA_KEY_VIDEO_ROTATION = 24;
    /**
     * This key retrieves the original capture framerate, if it's
     * available. The capture framerate will be a floating point
     * number.
     */
    public static final int METADATA_KEY_CAPTURE_FRAMERATE = 25;
    /**
     * If this key exists the media contains still image content.
     */
    public static final int METADATA_KEY_HAS_IMAGE       = 26;
    /**
     * If the media contains still images, this key retrieves the number
     * of still images.
     */
    public static final int METADATA_KEY_IMAGE_COUNT     = 27;
    /**
     * If the media contains still images, this key retrieves the image
     * index of the primary image.
     */
    public static final int METADATA_KEY_IMAGE_PRIMARY   = 28;
    /**
     * If the media contains still images, this key retrieves the width
     * of the primary image.
     */
    public static final int METADATA_KEY_IMAGE_WIDTH     = 29;
    /**
     * If the media contains still images, this key retrieves the height
     * of the primary image.
     */
    public static final int METADATA_KEY_IMAGE_HEIGHT    = 30;
    /**
     * If the media contains still images, this key retrieves the rotation
     * angle (in degrees clockwise) of the primary image. The image rotation
     * angle must be one of 0, 90, 180, or 270 degrees.
     */
    public static final int METADATA_KEY_IMAGE_ROTATION  = 31;
    /**
     * If the media contains video and this key exists, it retrieves the
     * total number of frames in the video sequence.
     */
    public static final int METADATA_KEY_VIDEO_FRAME_COUNT = 32;

    /**
     * If the media contains EXIF data, this key retrieves the offset (in bytes)
     * of the data.
     */
    public static final int METADATA_KEY_EXIF_OFFSET = 33;

    /**
     * If the media contains EXIF data, this key retrieves the length (in bytes)
     * of the data.
     */
    public static final int METADATA_KEY_EXIF_LENGTH = 34;

    /**
     * This key retrieves the color standard, if available.
     *
     * @see MediaFormat#COLOR_STANDARD_BT709
     * @see MediaFormat#COLOR_STANDARD_BT601_PAL
     * @see MediaFormat#COLOR_STANDARD_BT601_NTSC
     * @see MediaFormat#COLOR_STANDARD_BT2020
     */
    public static final int METADATA_KEY_COLOR_STANDARD = 35;

    /**
     * This key retrieves the color transfer, if available.
     *
     * @see MediaFormat#COLOR_TRANSFER_LINEAR
     * @see MediaFormat#COLOR_TRANSFER_SDR_VIDEO
     * @see MediaFormat#COLOR_TRANSFER_ST2084
     * @see MediaFormat#COLOR_TRANSFER_HLG
     */
    public static final int METADATA_KEY_COLOR_TRANSFER = 36;

    /**
     * This key retrieves the color range, if available.
     *
     * @see MediaFormat#COLOR_RANGE_LIMITED
     * @see MediaFormat#COLOR_RANGE_FULL
     */
    public static final int METADATA_KEY_COLOR_RANGE    = 37;

    /**
     * This key retrieves the sample rate in Hz, if available.
     * This is a signed 32-bit integer formatted as a string in base 10.
     */
    public static final int METADATA_KEY_SAMPLERATE      = 38;

    /**
     * This key retrieves the bits per sample in numbers of bits, if available.
     * This is a signed 32-bit integer formatted as a string in base 10.
     */
    public static final int METADATA_KEY_BITS_PER_SAMPLE = 39;

    /**
     * This key retrieves the video codec mimetype if available.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static final int METADATA_KEY_VIDEO_CODEC_MIME_TYPE = 40;

    /**
     * If the media contains XMP data, this key retrieves the offset (in bytes)
     * of the data.
     */
    public static final int METADATA_KEY_XMP_OFFSET = 41;

    /**
     * If the media contains XMP data, this key retrieves the length (in bytes)
     * of the data.
     */
    public static final int METADATA_KEY_XMP_LENGTH = 42;

    // Add more here...
}
