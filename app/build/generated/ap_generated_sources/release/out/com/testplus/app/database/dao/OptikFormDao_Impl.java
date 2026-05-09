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
import com.testplus.app.database.entities.OptikForm;
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
public final class OptikFormDao_Impl implements OptikFormDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<OptikForm> __insertionAdapterOfOptikForm;

  private final EntityDeletionOrUpdateAdapter<OptikForm> __deletionAdapterOfOptikForm;

  private final EntityDeletionOrUpdateAdapter<OptikForm> __updateAdapterOfOptikForm;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  public OptikFormDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfOptikForm = new EntityInsertionAdapter<OptikForm>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `optik_forms` (`id`,`ad`,`kagit`,`yon`,`olusturmaTarihi`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final OptikForm entity) {
        statement.bindLong(1, entity.id);
        if (entity.ad == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.ad);
        }
        if (entity.kagit == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.kagit);
        }
        if (entity.yon == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.yon);
        }
        statement.bindLong(5, entity.olusturmaTarihi);
      }
    };
    this.__deletionAdapterOfOptikForm = new EntityDeletionOrUpdateAdapter<OptikForm>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `optik_forms` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final OptikForm entity) {
        statement.bindLong(1, entity.id);
      }
    };
    this.__updateAdapterOfOptikForm = new EntityDeletionOrUpdateAdapter<OptikForm>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `optik_forms` SET `id` = ?,`ad` = ?,`kagit` = ?,`yon` = ?,`olusturmaTarihi` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final OptikForm entity) {
        statement.bindLong(1, entity.id);
        if (entity.ad == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.ad);
        }
        if (entity.kagit == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.kagit);
        }
        if (entity.yon == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.yon);
        }
        statement.bindLong(5, entity.olusturmaTarihi);
        statement.bindLong(6, entity.id);
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM optik_forms WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public long insert(final OptikForm form) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      final long _result = __insertionAdapterOfOptikForm.insertAndReturnId(form);
      __db.setTransactionSuccessful();
      return _result;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void delete(final OptikForm form) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfOptikForm.handle(form);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void update(final OptikForm form) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfOptikForm.handle(form);
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
  public LiveData<List<OptikForm>> getAll() {
    final String _sql = "SELECT * FROM optik_forms ORDER BY olusturmaTarihi DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[] {"optik_forms"}, false, new Callable<List<OptikForm>>() {
      @Override
      @Nullable
      public List<OptikForm> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAd = CursorUtil.getColumnIndexOrThrow(_cursor, "ad");
          final int _cursorIndexOfKagit = CursorUtil.getColumnIndexOrThrow(_cursor, "kagit");
          final int _cursorIndexOfYon = CursorUtil.getColumnIndexOrThrow(_cursor, "yon");
          final int _cursorIndexOfOlusturmaTarihi = CursorUtil.getColumnIndexOrThrow(_cursor, "olusturmaTarihi");
          final List<OptikForm> _result = new ArrayList<OptikForm>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final OptikForm _item;
            _item = new OptikForm();
            _item.id = _cursor.getLong(_cursorIndexOfId);
            if (_cursor.isNull(_cursorIndexOfAd)) {
              _item.ad = null;
            } else {
              _item.ad = _cursor.getString(_cursorIndexOfAd);
            }
            if (_cursor.isNull(_cursorIndexOfKagit)) {
              _item.kagit = null;
            } else {
              _item.kagit = _cursor.getString(_cursorIndexOfKagit);
            }
            if (_cursor.isNull(_cursorIndexOfYon)) {
              _item.yon = null;
            } else {
              _item.yon = _cursor.getString(_cursorIndexOfYon);
            }
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
  public OptikForm getById(final long id) {
    final String _sql = "SELECT * FROM optik_forms WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfAd = CursorUtil.getColumnIndexOrThrow(_cursor, "ad");
      final int _cursorIndexOfKagit = CursorUtil.getColumnIndexOrThrow(_cursor, "kagit");
      final int _cursorIndexOfYon = CursorUtil.getColumnIndexOrThrow(_cursor, "yon");
      final int _cursorIndexOfOlusturmaTarihi = CursorUtil.getColumnIndexOrThrow(_cursor, "olusturmaTarihi");
      final OptikForm _result;
      if (_cursor.moveToFirst()) {
        _result = new OptikForm();
        _result.id = _cursor.getLong(_cursorIndexOfId);
        if (_cursor.isNull(_cursorIndexOfAd)) {
          _result.ad = null;
        } else {
          _result.ad = _cursor.getString(_cursorIndexOfAd);
        }
        if (_cursor.isNull(_cursorIndexOfKagit)) {
          _result.kagit = null;
        } else {
          _result.kagit = _cursor.getString(_cursorIndexOfKagit);
        }
        if (_cursor.isNull(_cursorIndexOfYon)) {
          _result.yon = null;
        } else {
          _result.yon = _cursor.getString(_cursorIndexOfYon);
        }
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
