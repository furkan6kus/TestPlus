package com.testplus.app.database.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.testplus.app.database.entities.CevapAnahtari;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings({"unchecked", "deprecation"})
public final class CevapAnahtariDao_Impl implements CevapAnahtariDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CevapAnahtari> __insertionAdapterOfCevapAnahtari;

  private final EntityDeletionOrUpdateAdapter<CevapAnahtari> __updateAdapterOfCevapAnahtari;

  private final SharedSQLiteStatement __preparedStmtOfDeleteBySinavAndAlan;

  private final SharedSQLiteStatement __preparedStmtOfDeleteBySinavId;

  public CevapAnahtariDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCevapAnahtari = new EntityInsertionAdapter<CevapAnahtari>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `cevap_anahtarlari` (`id`,`sinavId`,`optikFormAlanId`,`ders`,`cevaplarJson`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final CevapAnahtari entity) {
        statement.bindLong(1, entity.id);
        statement.bindLong(2, entity.sinavId);
        statement.bindLong(3, entity.optikFormAlanId);
        if (entity.ders == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.ders);
        }
        if (entity.cevaplarJson == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.cevaplarJson);
        }
      }
    };
    this.__updateAdapterOfCevapAnahtari = new EntityDeletionOrUpdateAdapter<CevapAnahtari>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `cevap_anahtarlari` SET `id` = ?,`sinavId` = ?,`optikFormAlanId` = ?,`ders` = ?,`cevaplarJson` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final CevapAnahtari entity) {
        statement.bindLong(1, entity.id);
        statement.bindLong(2, entity.sinavId);
        statement.bindLong(3, entity.optikFormAlanId);
        if (entity.ders == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.ders);
        }
        if (entity.cevaplarJson == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.cevaplarJson);
        }
        statement.bindLong(6, entity.id);
      }
    };
    this.__preparedStmtOfDeleteBySinavAndAlan = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM cevap_anahtarlari WHERE sinavId = ? AND optikFormAlanId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteBySinavId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM cevap_anahtarlari WHERE sinavId = ?";
        return _query;
      }
    };
  }

  @Override
  public long insert(final CevapAnahtari cevapAnahtari) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      final long _result = __insertionAdapterOfCevapAnahtari.insertAndReturnId(cevapAnahtari);
      __db.setTransactionSuccessful();
      return _result;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void update(final CevapAnahtari cevapAnahtari) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfCevapAnahtari.handle(cevapAnahtari);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteBySinavAndAlan(final long sinavId, final long alanId) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteBySinavAndAlan.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, sinavId);
    _argIndex = 2;
    _stmt.bindLong(_argIndex, alanId);
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfDeleteBySinavAndAlan.release(_stmt);
    }
  }

  @Override
  public void deleteBySinavId(final long sinavId) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteBySinavId.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, sinavId);
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfDeleteBySinavId.release(_stmt);
    }
  }

  @Override
  public List<CevapAnahtari> getBySinavId(final long sinavId) {
    final String _sql = "SELECT * FROM cevap_anahtarlari WHERE sinavId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, sinavId);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfSinavId = CursorUtil.getColumnIndexOrThrow(_cursor, "sinavId");
      final int _cursorIndexOfOptikFormAlanId = CursorUtil.getColumnIndexOrThrow(_cursor, "optikFormAlanId");
      final int _cursorIndexOfDers = CursorUtil.getColumnIndexOrThrow(_cursor, "ders");
      final int _cursorIndexOfCevaplarJson = CursorUtil.getColumnIndexOrThrow(_cursor, "cevaplarJson");
      final List<CevapAnahtari> _result = new ArrayList<CevapAnahtari>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final CevapAnahtari _item;
        _item = new CevapAnahtari();
        _item.id = _cursor.getLong(_cursorIndexOfId);
        _item.sinavId = _cursor.getLong(_cursorIndexOfSinavId);
        _item.optikFormAlanId = _cursor.getLong(_cursorIndexOfOptikFormAlanId);
        if (_cursor.isNull(_cursorIndexOfDers)) {
          _item.ders = null;
        } else {
          _item.ders = _cursor.getString(_cursorIndexOfDers);
        }
        if (_cursor.isNull(_cursorIndexOfCevaplarJson)) {
          _item.cevaplarJson = null;
        } else {
          _item.cevaplarJson = _cursor.getString(_cursorIndexOfCevaplarJson);
        }
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public CevapAnahtari getBySinavAndAlan(final long sinavId, final long alanId) {
    final String _sql = "SELECT * FROM cevap_anahtarlari WHERE sinavId = ? AND optikFormAlanId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, sinavId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, alanId);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfSinavId = CursorUtil.getColumnIndexOrThrow(_cursor, "sinavId");
      final int _cursorIndexOfOptikFormAlanId = CursorUtil.getColumnIndexOrThrow(_cursor, "optikFormAlanId");
      final int _cursorIndexOfDers = CursorUtil.getColumnIndexOrThrow(_cursor, "ders");
      final int _cursorIndexOfCevaplarJson = CursorUtil.getColumnIndexOrThrow(_cursor, "cevaplarJson");
      final CevapAnahtari _result;
      if (_cursor.moveToFirst()) {
        _result = new CevapAnahtari();
        _result.id = _cursor.getLong(_cursorIndexOfId);
        _result.sinavId = _cursor.getLong(_cursorIndexOfSinavId);
        _result.optikFormAlanId = _cursor.getLong(_cursorIndexOfOptikFormAlanId);
        if (_cursor.isNull(_cursorIndexOfDers)) {
          _result.ders = null;
        } else {
          _result.ders = _cursor.getString(_cursorIndexOfDers);
        }
        if (_cursor.isNull(_cursorIndexOfCevaplarJson)) {
          _result.cevaplarJson = null;
        } else {
          _result.cevaplarJson = _cursor.getString(_cursorIndexOfCevaplarJson);
        }
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
