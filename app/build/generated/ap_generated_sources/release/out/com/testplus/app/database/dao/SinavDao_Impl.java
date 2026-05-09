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
import com.testplus.app.database.entities.Sinav;
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
public final class SinavDao_Impl implements SinavDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Sinav> __insertionAdapterOfSinav;

  private final EntityDeletionOrUpdateAdapter<Sinav> __deletionAdapterOfSinav;

  private final EntityDeletionOrUpdateAdapter<Sinav> __updateAdapterOfSinav;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  public SinavDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSinav = new EntityInsertionAdapter<Sinav>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `sinavlar` (`id`,`ad`,`optikFormId`,`yanlisCezasi`,`sinavTarihi`,`olusturmaTarihi`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Sinav entity) {
        statement.bindLong(1, entity.id);
        if (entity.ad == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.ad);
        }
        statement.bindLong(3, entity.optikFormId);
        if (entity.yanlisCezasi == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.yanlisCezasi);
        }
        statement.bindLong(5, entity.sinavTarihi);
        statement.bindLong(6, entity.olusturmaTarihi);
      }
    };
    this.__deletionAdapterOfSinav = new EntityDeletionOrUpdateAdapter<Sinav>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `sinavlar` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Sinav entity) {
        statement.bindLong(1, entity.id);
      }
    };
    this.__updateAdapterOfSinav = new EntityDeletionOrUpdateAdapter<Sinav>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `sinavlar` SET `id` = ?,`ad` = ?,`optikFormId` = ?,`yanlisCezasi` = ?,`sinavTarihi` = ?,`olusturmaTarihi` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Sinav entity) {
        statement.bindLong(1, entity.id);
        if (entity.ad == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.ad);
        }
        statement.bindLong(3, entity.optikFormId);
        if (entity.yanlisCezasi == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.yanlisCezasi);
        }
        statement.bindLong(5, entity.sinavTarihi);
        statement.bindLong(6, entity.olusturmaTarihi);
        statement.bindLong(7, entity.id);
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM sinavlar WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public long insert(final Sinav sinav) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      final long _result = __insertionAdapterOfSinav.insertAndReturnId(sinav);
      __db.setTransactionSuccessful();
      return _result;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void delete(final Sinav sinav) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfSinav.handle(sinav);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void update(final Sinav sinav) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfSinav.handle(sinav);
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
  public LiveData<List<Sinav>> getAll() {
    final String _sql = "SELECT * FROM sinavlar ORDER BY olusturmaTarihi DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[] {"sinavlar"}, false, new Callable<List<Sinav>>() {
      @Override
      @Nullable
      public List<Sinav> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAd = CursorUtil.getColumnIndexOrThrow(_cursor, "ad");
          final int _cursorIndexOfOptikFormId = CursorUtil.getColumnIndexOrThrow(_cursor, "optikFormId");
          final int _cursorIndexOfYanlisCezasi = CursorUtil.getColumnIndexOrThrow(_cursor, "yanlisCezasi");
          final int _cursorIndexOfSinavTarihi = CursorUtil.getColumnIndexOrThrow(_cursor, "sinavTarihi");
          final int _cursorIndexOfOlusturmaTarihi = CursorUtil.getColumnIndexOrThrow(_cursor, "olusturmaTarihi");
          final List<Sinav> _result = new ArrayList<Sinav>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Sinav _item;
            _item = new Sinav();
            _item.id = _cursor.getLong(_cursorIndexOfId);
            if (_cursor.isNull(_cursorIndexOfAd)) {
              _item.ad = null;
            } else {
              _item.ad = _cursor.getString(_cursorIndexOfAd);
            }
            _item.optikFormId = _cursor.getLong(_cursorIndexOfOptikFormId);
            if (_cursor.isNull(_cursorIndexOfYanlisCezasi)) {
              _item.yanlisCezasi = null;
            } else {
              _item.yanlisCezasi = _cursor.getString(_cursorIndexOfYanlisCezasi);
            }
            _item.sinavTarihi = _cursor.getLong(_cursorIndexOfSinavTarihi);
            _item.olusturmaTarihi = _cursor.getLong(_cursorIndexOfOlusturmaTarihi);
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
  public Sinav getById(final long id) {
    final String _sql = "SELECT * FROM sinavlar WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfAd = CursorUtil.getColumnIndexOrThrow(_cursor, "ad");
      final int _cursorIndexOfOptikFormId = CursorUtil.getColumnIndexOrThrow(_cursor, "optikFormId");
      final int _cursorIndexOfYanlisCezasi = CursorUtil.getColumnIndexOrThrow(_cursor, "yanlisCezasi");
      final int _cursorIndexOfSinavTarihi = CursorUtil.getColumnIndexOrThrow(_cursor, "sinavTarihi");
      final int _cursorIndexOfOlusturmaTarihi = CursorUtil.getColumnIndexOrThrow(_cursor, "olusturmaTarihi");
      final Sinav _result;
      if (_cursor.moveToFirst()) {
        _result = new Sinav();
        _result.id = _cursor.getLong(_cursorIndexOfId);
        if (_cursor.isNull(_cursorIndexOfAd)) {
          _result.ad = null;
        } else {
          _result.ad = _cursor.getString(_cursorIndexOfAd);
        }
        _result.optikFormId = _cursor.getLong(_cursorIndexOfOptikFormId);
        if (_cursor.isNull(_cursorIndexOfYanlisCezasi)) {
          _result.yanlisCezasi = null;
        } else {
          _result.yanlisCezasi = _cursor.getString(_cursorIndexOfYanlisCezasi);
        }
        _result.sinavTarihi = _cursor.getLong(_cursorIndexOfSinavTarihi);
        _result.olusturmaTarihi = _cursor.getLong(_cursorIndexOfOlusturmaTarihi);
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
