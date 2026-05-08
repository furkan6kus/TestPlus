package com.testplus.app.database.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.testplus.app.database.entities.OgrenciKagidi;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

@SuppressWarnings({"unchecked", "deprecation"})
public final class OgrenciKagidiDao_Impl implements OgrenciKagidiDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<OgrenciKagidi> __insertionAdapterOfOgrenciKagidi;

  private final EntityDeletionOrUpdateAdapter<OgrenciKagidi> __deletionAdapterOfOgrenciKagidi;

  private final EntityDeletionOrUpdateAdapter<OgrenciKagidi> __updateAdapterOfOgrenciKagidi;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  private final SharedSQLiteStatement __preparedStmtOfDeleteBySinavId;

  public OgrenciKagidiDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfOgrenciKagidi = new EntityInsertionAdapter<OgrenciKagidi>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `ogrenci_kagitlari` (`id`,`sinavId`,`ad`,`numara`,`sinif`,`cevaplarJson`,`sonuclarJson`,`tarih`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final OgrenciKagidi entity) {
        statement.bindLong(1, entity.id);
        statement.bindLong(2, entity.sinavId);
        if (entity.ad == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.ad);
        }
        if (entity.numara == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.numara);
        }
        if (entity.sinif == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.sinif);
        }
        if (entity.cevaplarJson == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.cevaplarJson);
        }
        if (entity.sonuclarJson == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.sonuclarJson);
        }
        statement.bindLong(8, entity.tarih);
      }
    };
    this.__deletionAdapterOfOgrenciKagidi = new EntityDeletionOrUpdateAdapter<OgrenciKagidi>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `ogrenci_kagitlari` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final OgrenciKagidi entity) {
        statement.bindLong(1, entity.id);
      }
    };
    this.__updateAdapterOfOgrenciKagidi = new EntityDeletionOrUpdateAdapter<OgrenciKagidi>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `ogrenci_kagitlari` SET `id` = ?,`sinavId` = ?,`ad` = ?,`numara` = ?,`sinif` = ?,`cevaplarJson` = ?,`sonuclarJson` = ?,`tarih` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final OgrenciKagidi entity) {
        statement.bindLong(1, entity.id);
        statement.bindLong(2, entity.sinavId);
        if (entity.ad == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.ad);
        }
        if (entity.numara == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.numara);
        }
        if (entity.sinif == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.sinif);
        }
        if (entity.cevaplarJson == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.cevaplarJson);
        }
        if (entity.sonuclarJson == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.sonuclarJson);
        }
        statement.bindLong(8, entity.tarih);
        statement.bindLong(9, entity.id);
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM ogrenci_kagitlari WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteBySinavId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM ogrenci_kagitlari WHERE sinavId = ?";
        return _query;
      }
    };
  }

  @Override
  public long insert(final OgrenciKagidi kagit) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      final long _result = __insertionAdapterOfOgrenciKagidi.insertAndReturnId(kagit);
      __db.setTransactionSuccessful();
      return _result;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void delete(final OgrenciKagidi kagit) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfOgrenciKagidi.handle(kagit);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void update(final OgrenciKagidi kagit) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfOgrenciKagidi.handle(kagit);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteById(final long id) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, id);
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfDeleteById.release(_stmt);
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
  public LiveData<List<OgrenciKagidi>> getBySinavId(final long sinavId) {
    final String _sql = "SELECT * FROM ogrenci_kagitlari WHERE sinavId = ? ORDER BY tarih DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, sinavId);
    return __db.getInvalidationTracker().createLiveData(new String[] {"ogrenci_kagitlari"}, false, new Callable<List<OgrenciKagidi>>() {
      @Override
      @Nullable
      public List<OgrenciKagidi> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSinavId = CursorUtil.getColumnIndexOrThrow(_cursor, "sinavId");
          final int _cursorIndexOfAd = CursorUtil.getColumnIndexOrThrow(_cursor, "ad");
          final int _cursorIndexOfNumara = CursorUtil.getColumnIndexOrThrow(_cursor, "numara");
          final int _cursorIndexOfSinif = CursorUtil.getColumnIndexOrThrow(_cursor, "sinif");
          final int _cursorIndexOfCevaplarJson = CursorUtil.getColumnIndexOrThrow(_cursor, "cevaplarJson");
          final int _cursorIndexOfSonuclarJson = CursorUtil.getColumnIndexOrThrow(_cursor, "sonuclarJson");
          final int _cursorIndexOfTarih = CursorUtil.getColumnIndexOrThrow(_cursor, "tarih");
          final List<OgrenciKagidi> _result = new ArrayList<OgrenciKagidi>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final OgrenciKagidi _item;
            _item = new OgrenciKagidi();
            _item.id = _cursor.getLong(_cursorIndexOfId);
            _item.sinavId = _cursor.getLong(_cursorIndexOfSinavId);
            if (_cursor.isNull(_cursorIndexOfAd)) {
              _item.ad = null;
            } else {
              _item.ad = _cursor.getString(_cursorIndexOfAd);
            }
            if (_cursor.isNull(_cursorIndexOfNumara)) {
              _item.numara = null;
            } else {
              _item.numara = _cursor.getString(_cursorIndexOfNumara);
            }
            if (_cursor.isNull(_cursorIndexOfSinif)) {
              _item.sinif = null;
            } else {
              _item.sinif = _cursor.getString(_cursorIndexOfSinif);
            }
            if (_cursor.isNull(_cursorIndexOfCevaplarJson)) {
              _item.cevaplarJson = null;
            } else {
              _item.cevaplarJson = _cursor.getString(_cursorIndexOfCevaplarJson);
            }
            if (_cursor.isNull(_cursorIndexOfSonuclarJson)) {
              _item.sonuclarJson = null;
            } else {
              _item.sonuclarJson = _cursor.getString(_cursorIndexOfSonuclarJson);
            }
            _item.tarih = _cursor.getLong(_cursorIndexOfTarih);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public OgrenciKagidi getById(final long id) {
    final String _sql = "SELECT * FROM ogrenci_kagitlari WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfSinavId = CursorUtil.getColumnIndexOrThrow(_cursor, "sinavId");
      final int _cursorIndexOfAd = CursorUtil.getColumnIndexOrThrow(_cursor, "ad");
      final int _cursorIndexOfNumara = CursorUtil.getColumnIndexOrThrow(_cursor, "numara");
      final int _cursorIndexOfSinif = CursorUtil.getColumnIndexOrThrow(_cursor, "sinif");
      final int _cursorIndexOfCevaplarJson = CursorUtil.getColumnIndexOrThrow(_cursor, "cevaplarJson");
      final int _cursorIndexOfSonuclarJson = CursorUtil.getColumnIndexOrThrow(_cursor, "sonuclarJson");
      final int _cursorIndexOfTarih = CursorUtil.getColumnIndexOrThrow(_cursor, "tarih");
      final OgrenciKagidi _result;
      if (_cursor.moveToFirst()) {
        _result = new OgrenciKagidi();
        _result.id = _cursor.getLong(_cursorIndexOfId);
        _result.sinavId = _cursor.getLong(_cursorIndexOfSinavId);
        if (_cursor.isNull(_cursorIndexOfAd)) {
          _result.ad = null;
        } else {
          _result.ad = _cursor.getString(_cursorIndexOfAd);
        }
        if (_cursor.isNull(_cursorIndexOfNumara)) {
          _result.numara = null;
        } else {
          _result.numara = _cursor.getString(_cursorIndexOfNumara);
        }
        if (_cursor.isNull(_cursorIndexOfSinif)) {
          _result.sinif = null;
        } else {
          _result.sinif = _cursor.getString(_cursorIndexOfSinif);
        }
        if (_cursor.isNull(_cursorIndexOfCevaplarJson)) {
          _result.cevaplarJson = null;
        } else {
          _result.cevaplarJson = _cursor.getString(_cursorIndexOfCevaplarJson);
        }
        if (_cursor.isNull(_cursorIndexOfSonuclarJson)) {
          _result.sonuclarJson = null;
        } else {
          _result.sonuclarJson = _cursor.getString(_cursorIndexOfSonuclarJson);
        }
        _result.tarih = _cursor.getLong(_cursorIndexOfTarih);
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
