package win.regin.sample.impl.a;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import win.regin.sample.impl.m.ContextManager;

/**
 * @author :Reginer in  2020/10/14 14:05.
 * 联系方式:QQ:282921012
 * 功能描述:
 */
public class FuncProvider extends ContentProvider {

    @Override
    public boolean onCreate() {
        ContextManager.getInstance().setContext(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

}
