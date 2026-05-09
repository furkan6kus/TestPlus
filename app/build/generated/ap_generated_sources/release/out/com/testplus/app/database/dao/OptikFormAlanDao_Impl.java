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
import com.testplus.app.database.entities.OptikFormAlan;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings({"unchecked", "deprecation"})
public final class OptikFormAlanDao_Impl implements OptikFormAlanDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<OptikFormAlan> __insertionAdapterOfOptikFormAlan;

  private final EntityDeletionOrUpdateAdapter<OptikFormAlan> __deletionAdapterOfOptikFormAlan;

  private final EntityDeletionOrUpdateAdapter<OptikFormAlan> __updateAdapterOfOptikFormAlan;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByFormId;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  public OptikFormAlanDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfOptikFormAlan = new EntityInsertionAdapter<OptikFormAlan>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `optik_form_alanlar` (`id`,`formId`,`tur`,`yon`,`etiket`,`desen`,`solBosluk`,`ustBosluk`,`blokSayisi`,`bloktakiVeriSayisi`,`ders`,`ilkSoruNumarasi`,`blokArasiBosluk`,`posX`,`posY`,`siraNo`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final OptikFormAlan entity) {
        statement.bindLong(1, entity.id);
        statement.bindLong(2, entity.formId);
        if (entity.tur == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.tur);
        }
        if (entity.yon == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.yon);
        }
        if (entity.etiket == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.etiket);
        }
        if (entity.desen == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.desen);
        }
        statement.bindDouble(7, entity.solBosluk);
        statement.bindDouble(8, entity.ustBosluk);
        statement.bindLong(9, entity.blokSayisi);
        statement.bindLong(10, entity.bloktakiVeriSayisi);
        if (entity.ders == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.ders);
        }
        statement.bindLong(12, entity.ilkSoruNumarasi);
        final int _tmp = entity.blokArasiBosluk ? 1 : 0;
        statement.bindLong(13, _tmp);
        statement.bindDouble(14, entity.posX);
        statement.bindDouble(15, entity.posY);
        statement.bindLong(16, entity.siraNo);
      }
    };
    this.__deletionAdapterOfOptikFormAlan = new EntityDeletionOrUpdateAdapter<OptikFormAlan>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `optik_form_alanlar` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final OptikFormAlan entity) {
        statement.bindLong(1, entity.id);
      }
    };
    this.__updateAdapterOfOptikFormAlan = new EntityDeletionOrUpdateAdapter<OptikFormAlan>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `optik_form_alanlar` SET `id` = ?,`formId` = ?,`tur` = ?,`yon` = ?,`etiket` = ?,`desen` = ?,`solBosluk` = ?,`ustBosluk` = ?,`blokSayisi` = ?,`bloktakiVeriSayisi` = ?,`ders` = ?,`ilkSoruNumarasi` = ?,`blokArasiBosluk` = ?,`posX` = ?,`posY` = ?,`siraNo` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final OptikFormAlan entity) {
        statement.bindLong(1, entity.id);
        statement.bindLong(2, entity.formId);
        if (entity.tur == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.tur);
        }
        if (entity.yon == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.yon);
        }
        if (entity.etiket == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.etiket);
        }
        if (entity.desen == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.desen);
        }
        statement.bindDouble(7, entity.solBosluk);
        statement.bindDouble(8, entity.ustBosluk);
        statement.bindLong(9, entity.blokSayisi);
        statement.bindLong(10, entity.bloktakiVeriSayisi);
        if (entity.ders == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.ders);
        }
        statement.bindLong(12, entity.ilkSoruNumarasi);
        final int _tmp = entity.blokArasiBosluk ? 1 : 0;
        statement.bindLong(13, _tmp);
        statement.bindDouble(14, entity.posX);
        statement.bindDouble(15, entity.posY);
        statement.bindLong(16, entity.siraNo);
        statement.bindLong(17, entity.id);
      }
    };
    this.__preparedStmtOfDeleteByFormId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM optik_form_alanlar WHERE formId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM optik_form_alanlar WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public long insert(final OptikFormAlan alan) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      final long _result = __insertionAdapterOfOptikFormAlan.insertAndReturnId(alan);
      __db.setTransactionSuccessful();
      return _result;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void delete(final OptikFormAlan alan) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfOptikFormAlan.handle(alan);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void update(final OptikFormAlan alan) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfOptikFormAlan.handle(alan);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteByFormId(final long formId) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByFormId.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, formId);
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfDeleteByFormId.release(_stmt);
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
  public List<OptikFormAlan> getByFormId(final long formId) {
    final String _sql = "SELECT * FROM optik_form_alanlar WHERE formId = ? ORDER BY siraNo ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, formId);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfFormId = CursorUtil.getColumnIndexOrThrow(_cursor, "formId");
      final int _cursorIndexOfTur = CursorUtil.getColumnIndexOrThrow(_cursor, "tur");
      final int _cursorIndexOfYon = CursorUtil.getColumnIndexOrThrow(_cursor, "yon");
      final int _cursorIndexOfEtiket = CursorUtil.getColumnIndexOrThrow(_cursor, "etiket");
      final int _cursorIndexOfDesen = CursorUtil.getColumnIndexOrThrow(_cursor, "desen");
      final int _cursorIndexOfSolBosluk = CursorUtil.getColumnIndexOrThrow(_cursor, "solBosluk");
      final int _cursorIndexOfUstBosluk = CursorUtil.getColumnIndexOrThrow(_cursor, "ustBosluk");
      final int _cursorIndexOfBlokSayisi = CursorUtil.getColumnIndexOrThrow(_cursor, "blokSayisi");
      final int _cursorIndexOfBloktakiVeriSayisi = CursorUtil.getColumnIndexOrThrow(_cursor, "bloktakiVeriSayisi");
      final int _cursorIndexOfDers = CursorUtil.getColumnIndexOrThrow(_cursor, "ders");
      final int _cursorIndexOfIlkSoruNumarasi = CursorUtil.getColumnIndexOrThrow(_cursor, "ilkSoruNumarasi");
      final int _cursorIndexOfBlokArasiBosluk = CursorUtil.getColumnIndexOrThrow(_cursor, "blokArasiBosluk");
      final int _cursorIndexOfPosX = CursorUtil.getColumnIndexOrThrow(_cursor, "posX");
      final int _cursorIndexOfPosY = CursorUtil.getColumnIndexOrThrow(_cursor, "posY");
      final int _cursorIndexOfSiraNo = CursorUtil.getColumnIndexOrThrow(_cursor, "siraNo");
      final List<OptikFormAlan> _result = new ArrayList<OptikFormAlan>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final OptikFormAlan _item;
        _item = new OptikFormAlan();
        _item.id = _cursor.getLong(_cursorIndexOfId);
        _item.formId = _cursor.getLong(_cursorIndexOfFormId);
        if (_cursor.isNull(_cursorIndexOfTur)) {
          _item.tur = null;
        } else {
          _item.tur = _cursor.getString(_cursorIndexOfTur);
        }
        if (_cursor.isNull(_cursorIndexOfYon)) {
          _item.yon = null;
        } else {
          _item.yon = _cursor.getString(_cursorIndexOfYon);
        }
        if (_cursor.isNull(_cursorIndexOfEtiket)) {
          _item.etiket = null;
        } else {
          _item.etiket = _cursor.getString(_cursorIndexOfEtiket);
        }
        if (_cursor.isNull(_cursorIndexOfDesen)) {
          _item.desen = null;
        } else {
          _item.desen = _cursor.getString(_cursorIndexOfDesen);
        }
        _item.solBosluk = _cursor.getFloat(_cursorIndexOfSolBosluk);
        _item.ustBosluk = _cursor.getFloat(_cursorIndexOfUstBosluk);
        _item.blokSayisi = _cursor.getInt(_cursorIndexOfBlokSayisi);
        _item.bloktakiVeriSayisi = _cursor.getInt(_cursorIndexOfBloktakiVeriSayisi);
        if (_cursor.isNull(_cursorIndexOfDers)) {
          _item.ders = null;
        } else {
          _item.ders = _cursor.getString(_cursorIndexOfDers);
        }
        _item.ilkSoruNumarasi = _cursor.getInt(_cursorIndexOfIlkSoruNumarasi);
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfBlokArasiBosluk);
        _item.blokArasiBosluk = _tmp != 0;
        _item.posX = _cursor.getFloat(_cursorIndexOfPosX);
        _item.posY = _cursor.getFloat(_cursorIndexOfPosY);
        _item.siraNo = _cursor.getInt(_cursorIndexOfSiraNo);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public OptikFormAlan getById(final long id) {
    final String _sql = "SELECT * FROM optik_form_alanlar WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfFormId = CursorUtil.getColumnIndexOrThrow(_cursor, "formId");
      final int _cursorIndexOfTur = CursorUtil.getColumnIndexOrThrow(_cursor, "tur");
      final int _cursorIndexOfYon = CursorUtil.getColumnIndexOrThrow(_cursor, "yon");
      final int _cursorIndexOfEtiket = CursorUtil.getColumnIndexOrThrow(_cursor, "etiket");
      final int _cursorIndexOfDesen = CursorUtil.getColumnIndexOrThrow(_cursor, "desen");
      final int _cursorIndexOfSolBosluk = CursorUtil.getColumnIndexOrThrow(_cursor, "solBosluk");
      final int _cursorIndexOfUstBosluk = CursorUtil.getColumnIndexOrThrow(_cursor, "ustBosluk");
      final int _cursorIndexOfBlokSayisi = CursorUtil.getColumnIndexOrThrow(_cursor, "blokSayisi");
      final int _cursorIndexOfBloktakiVeriSayisi = CursorUtil.getColumnIndexOrThrow(_cursor, "bloktakiVeriSayisi");
      final int _cursorIndexOfDers = CursorUtil.getColumnIndexOrThrow(_cursor, "ders");
      final int _cursorIndexOfIlkSoruNumarasi = CursorUtil.getColumnIndexOrThrow(_cursor, "ilkSoruNumarasi");
      final int _cursorIndexOfBlokArasiBosluk = CursorUtil.getColumnIndexOrThrow(_cursor, "blokArasiBosluk");
      final int _cursorIndexOfPosX = CursorUtil.getColumnIndexOrThrow(_cursor, "posX");
      final int _cursorIndexOfPosY = CursorUtil.getColumnIndexOrThrow(_cursor, "posY");
      final int _cursorIndexOfSiraNo = CursorUtil.getColumnIndexOrThrow(_cursor, "siraNo");
      final OptikFormAlan _result;
      if (_cursor.moveToFirst()) {
        _result = new OptikFormAlan();
        _result.id = _cursor.getLong(_cursorIndexOfId);
        _result.formId = _cursor.getLong(_cursorIndexOfFormId);
        if (_cursor.isNull(_cursorIndexOfTur)) {
          _result.tur = null;
        } else {
          _result.tur = _cursor.getString(_cursorIndexOfTur);
        }
        if (_cursor.isNull(_cursorIndexOfYon)) {
          _result.yon = null;
        } else {
          _result.yon = _cursor.getString(_cursorIndexOfYon);
        }
        if (_cursor.isNull(_cursorIndexOfEtiket)) {
          _result.etiket = null;
        } else {
          _result.etiket = _cursor.getString(_cursorIndexOfEtiket);
        }
        if (_cursor.isNull(_cursorIndexOfDesen)) {
          _result.desen = null;
        } else {
          _result.desen = _cursor.getString(_cursorIndexOfDesen);
        }
        _result.solBosluk = _cursor.getFloat(_cursorIndexOfSolBosluk);
        _result.ustBosluk = _cursor.getFloat(_cursorIndexOfUstBosluk);
        _result.blokSayisi = _cursor.getInt(_cursorIndexOfBlokSayisi);
        _result.bloktakiVeriSayisi = _cursor.getInt(_cursorIndexOfBloktakiVeriSayisi);
        if (_cursor.isNull(_cursorIndexOfDers)) {
          _result.ders = null;
        } else {
          _result.ders = _cursor.getString(_cursorIndexOfDers);
        }
        _result.ilkSoruNumarasi = _cursor.getInt(_cursorIndexOfIlkSoruNumarasi);
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfBlokArasiBosluk);
        _result.blokArasiBosluk = _tmp != 0;
        _result.posX = _cursor.getFloat(_cursorIndexOfPosX);
        _result.posY = _cursor.getFloat(_cursorIndexOfPosY);
        _result.siraNo = _cursor.getInt(_cursorIndexOfSiraNo);
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
