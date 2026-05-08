package com.testplus.app.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.testplus.app.database.dao.CevapAnahtariDao;
import com.testplus.app.database.dao.CevapAnahtariDao_Impl;
import com.testplus.app.database.dao.OgrenciKagidiDao;
import com.testplus.app.database.dao.OgrenciKagidiDao_Impl;
import com.testplus.app.database.dao.OptikFormAlanDao;
import com.testplus.app.database.dao.OptikFormAlanDao_Impl;
import com.testplus.app.database.dao.OptikFormDao;
import com.testplus.app.database.dao.OptikFormDao_Impl;
import com.testplus.app.database.dao.SinavDao;
import com.testplus.app.database.dao.SinavDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile OptikFormDao _optikFormDao;

  private volatile OptikFormAlanDao _optikFormAlanDao;

  private volatile SinavDao _sinavDao;

  private volatile CevapAnahtariDao _cevapAnahtariDao;

  private volatile OgrenciKagidiDao _ogrenciKagidiDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `optik_forms` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ad` TEXT, `kagit` TEXT, `yon` TEXT, `olusturmaTarihi` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `optik_form_alanlar` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `formId` INTEGER NOT NULL, `tur` TEXT, `yon` TEXT, `etiket` TEXT, `desen` TEXT, `solBosluk` REAL NOT NULL, `ustBosluk` REAL NOT NULL, `blokSayisi` INTEGER NOT NULL, `bloktakiVeriSayisi` INTEGER NOT NULL, `ders` TEXT, `ilkSoruNumarasi` INTEGER NOT NULL, `blokArasiBosluk` INTEGER NOT NULL, `posX` REAL NOT NULL, `posY` REAL NOT NULL, `siraNo` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `sinavlar` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ad` TEXT, `optikFormId` INTEGER NOT NULL, `yanlisCezasi` TEXT, `sinavTarihi` INTEGER NOT NULL, `olusturmaTarihi` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `cevap_anahtarlari` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sinavId` INTEGER NOT NULL, `optikFormAlanId` INTEGER NOT NULL, `ders` TEXT, `cevaplarJson` TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `ogrenci_kagitlari` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sinavId` INTEGER NOT NULL, `ad` TEXT, `numara` TEXT, `sinif` TEXT, `cevaplarJson` TEXT, `sonuclarJson` TEXT, `tarih` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '4040fecd757999cbd133f4237be24027')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `optik_forms`");
        db.execSQL("DROP TABLE IF EXISTS `optik_form_alanlar`");
        db.execSQL("DROP TABLE IF EXISTS `sinavlar`");
        db.execSQL("DROP TABLE IF EXISTS `cevap_anahtarlari`");
        db.execSQL("DROP TABLE IF EXISTS `ogrenci_kagitlari`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsOptikForms = new HashMap<String, TableInfo.Column>(5);
        _columnsOptikForms.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOptikForms.put("ad", new TableInfo.Column("ad", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOptikForms.put("kagit", new TableInfo.Column("kagit", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOptikForms.put("yon", new TableInfo.Column("yon", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOptikForms.put("olusturmaTarihi", new TableInfo.Column("olusturmaTarihi", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysOptikForms = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesOptikForms = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoOptikForms = new TableInfo("optik_forms", _columnsOptikForms, _foreignKeysOptikForms, _indicesOptikForms);
        final TableInfo _existingOptikForms = TableInfo.read(db, "optik_forms");
        if (!_infoOptikForms.equals(_existingOptikForms)) {
          return new RoomOpenHelper.ValidationResult(false, "optik_forms(com.testplus.app.database.entities.OptikForm).\n"
                  + " Expected:\n" + _infoOptikForms + "\n"
                  + " Found:\n" + _existingOptikForms);
        }
        final HashMap<String, TableInfo.Column> _columnsOptikFormAlanlar = new HashMap<String, TableInfo.Column>(16);
        _columnsOptikFormAlanlar.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOptikFormAlanlar.put("formId", new TableInfo.Column("formId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOptikFormAlanlar.put("tur", new TableInfo.Column("tur", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOptikFormAlanlar.put("yon", new TableInfo.Column("yon", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOptikFormAlanlar.put("etiket", new TableInfo.Column("etiket", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOptikFormAlanlar.put("desen", new TableInfo.Column("desen", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOptikFormAlanlar.put("solBosluk", new TableInfo.Column("solBosluk", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOptikFormAlanlar.put("ustBosluk", new TableInfo.Column("ustBosluk", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOptikFormAlanlar.put("blokSayisi", new TableInfo.Column("blokSayisi", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOptikFormAlanlar.put("bloktakiVeriSayisi", new TableInfo.Column("bloktakiVeriSayisi", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOptikFormAlanlar.put("ders", new TableInfo.Column("ders", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOptikFormAlanlar.put("ilkSoruNumarasi", new TableInfo.Column("ilkSoruNumarasi", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOptikFormAlanlar.put("blokArasiBosluk", new TableInfo.Column("blokArasiBosluk", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOptikFormAlanlar.put("posX", new TableInfo.Column("posX", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOptikFormAlanlar.put("posY", new TableInfo.Column("posY", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOptikFormAlanlar.put("siraNo", new TableInfo.Column("siraNo", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysOptikFormAlanlar = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesOptikFormAlanlar = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoOptikFormAlanlar = new TableInfo("optik_form_alanlar", _columnsOptikFormAlanlar, _foreignKeysOptikFormAlanlar, _indicesOptikFormAlanlar);
        final TableInfo _existingOptikFormAlanlar = TableInfo.read(db, "optik_form_alanlar");
        if (!_infoOptikFormAlanlar.equals(_existingOptikFormAlanlar)) {
          return new RoomOpenHelper.ValidationResult(false, "optik_form_alanlar(com.testplus.app.database.entities.OptikFormAlan).\n"
                  + " Expected:\n" + _infoOptikFormAlanlar + "\n"
                  + " Found:\n" + _existingOptikFormAlanlar);
        }
        final HashMap<String, TableInfo.Column> _columnsSinavlar = new HashMap<String, TableInfo.Column>(6);
        _columnsSinavlar.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSinavlar.put("ad", new TableInfo.Column("ad", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSinavlar.put("optikFormId", new TableInfo.Column("optikFormId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSinavlar.put("yanlisCezasi", new TableInfo.Column("yanlisCezasi", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSinavlar.put("sinavTarihi", new TableInfo.Column("sinavTarihi", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSinavlar.put("olusturmaTarihi", new TableInfo.Column("olusturmaTarihi", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSinavlar = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSinavlar = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSinavlar = new TableInfo("sinavlar", _columnsSinavlar, _foreignKeysSinavlar, _indicesSinavlar);
        final TableInfo _existingSinavlar = TableInfo.read(db, "sinavlar");
        if (!_infoSinavlar.equals(_existingSinavlar)) {
          return new RoomOpenHelper.ValidationResult(false, "sinavlar(com.testplus.app.database.entities.Sinav).\n"
                  + " Expected:\n" + _infoSinavlar + "\n"
                  + " Found:\n" + _existingSinavlar);
        }
        final HashMap<String, TableInfo.Column> _columnsCevapAnahtarlari = new HashMap<String, TableInfo.Column>(5);
        _columnsCevapAnahtarlari.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCevapAnahtarlari.put("sinavId", new TableInfo.Column("sinavId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCevapAnahtarlari.put("optikFormAlanId", new TableInfo.Column("optikFormAlanId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCevapAnahtarlari.put("ders", new TableInfo.Column("ders", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCevapAnahtarlari.put("cevaplarJson", new TableInfo.Column("cevaplarJson", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCevapAnahtarlari = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCevapAnahtarlari = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCevapAnahtarlari = new TableInfo("cevap_anahtarlari", _columnsCevapAnahtarlari, _foreignKeysCevapAnahtarlari, _indicesCevapAnahtarlari);
        final TableInfo _existingCevapAnahtarlari = TableInfo.read(db, "cevap_anahtarlari");
        if (!_infoCevapAnahtarlari.equals(_existingCevapAnahtarlari)) {
          return new RoomOpenHelper.ValidationResult(false, "cevap_anahtarlari(com.testplus.app.database.entities.CevapAnahtari).\n"
                  + " Expected:\n" + _infoCevapAnahtarlari + "\n"
                  + " Found:\n" + _existingCevapAnahtarlari);
        }
        final HashMap<String, TableInfo.Column> _columnsOgrenciKagitlari = new HashMap<String, TableInfo.Column>(8);
        _columnsOgrenciKagitlari.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOgrenciKagitlari.put("sinavId", new TableInfo.Column("sinavId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOgrenciKagitlari.put("ad", new TableInfo.Column("ad", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOgrenciKagitlari.put("numara", new TableInfo.Column("numara", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOgrenciKagitlari.put("sinif", new TableInfo.Column("sinif", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOgrenciKagitlari.put("cevaplarJson", new TableInfo.Column("cevaplarJson", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOgrenciKagitlari.put("sonuclarJson", new TableInfo.Column("sonuclarJson", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOgrenciKagitlari.put("tarih", new TableInfo.Column("tarih", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysOgrenciKagitlari = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesOgrenciKagitlari = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoOgrenciKagitlari = new TableInfo("ogrenci_kagitlari", _columnsOgrenciKagitlari, _foreignKeysOgrenciKagitlari, _indicesOgrenciKagitlari);
        final TableInfo _existingOgrenciKagitlari = TableInfo.read(db, "ogrenci_kagitlari");
        if (!_infoOgrenciKagitlari.equals(_existingOgrenciKagitlari)) {
          return new RoomOpenHelper.ValidationResult(false, "ogrenci_kagitlari(com.testplus.app.database.entities.OgrenciKagidi).\n"
                  + " Expected:\n" + _infoOgrenciKagitlari + "\n"
                  + " Found:\n" + _existingOgrenciKagitlari);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "4040fecd757999cbd133f4237be24027", "4599c6b47298fa96dbbb07c369ca9788");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "optik_forms","optik_form_alanlar","sinavlar","cevap_anahtarlari","ogrenci_kagitlari");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `optik_forms`");
      _db.execSQL("DELETE FROM `optik_form_alanlar`");
      _db.execSQL("DELETE FROM `sinavlar`");
      _db.execSQL("DELETE FROM `cevap_anahtarlari`");
      _db.execSQL("DELETE FROM `ogrenci_kagitlari`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(OptikFormDao.class, OptikFormDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(OptikFormAlanDao.class, OptikFormAlanDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SinavDao.class, SinavDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CevapAnahtariDao.class, CevapAnahtariDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(OgrenciKagidiDao.class, OgrenciKagidiDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public OptikFormDao optikFormDao() {
    if (_optikFormDao != null) {
      return _optikFormDao;
    } else {
      synchronized(this) {
        if(_optikFormDao == null) {
          _optikFormDao = new OptikFormDao_Impl(this);
        }
        return _optikFormDao;
      }
    }
  }

  @Override
  public OptikFormAlanDao optikFormAlanDao() {
    if (_optikFormAlanDao != null) {
      return _optikFormAlanDao;
    } else {
      synchronized(this) {
        if(_optikFormAlanDao == null) {
          _optikFormAlanDao = new OptikFormAlanDao_Impl(this);
        }
        return _optikFormAlanDao;
      }
    }
  }

  @Override
  public SinavDao sinavDao() {
    if (_sinavDao != null) {
      return _sinavDao;
    } else {
      synchronized(this) {
        if(_sinavDao == null) {
          _sinavDao = new SinavDao_Impl(this);
        }
        return _sinavDao;
      }
    }
  }

  @Override
  public CevapAnahtariDao cevapAnahtariDao() {
    if (_cevapAnahtariDao != null) {
      return _cevapAnahtariDao;
    } else {
      synchronized(this) {
        if(_cevapAnahtariDao == null) {
          _cevapAnahtariDao = new CevapAnahtariDao_Impl(this);
        }
        return _cevapAnahtariDao;
      }
    }
  }

  @Override
  public OgrenciKagidiDao ogrenciKagidiDao() {
    if (_ogrenciKagidiDao != null) {
      return _ogrenciKagidiDao;
    } else {
      synchronized(this) {
        if(_ogrenciKagidiDao == null) {
          _ogrenciKagidiDao = new OgrenciKagidiDao_Impl(this);
        }
        return _ogrenciKagidiDao;
      }
    }
  }
}
