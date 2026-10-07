package pro.nitron.zencall.data.local;

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
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import pro.nitron.zencall.data.local.dao.CategoryDao;
import pro.nitron.zencall.data.local.dao.CategoryDao_Impl;
import pro.nitron.zencall.data.local.dao.ModeDao;
import pro.nitron.zencall.data.local.dao.ModeDao_Impl;
import pro.nitron.zencall.data.local.dao.NumberDao;
import pro.nitron.zencall.data.local.dao.NumberDao_Impl;
import pro.nitron.zencall.data.local.dao.RuleDao;
import pro.nitron.zencall.data.local.dao.RuleDao_Impl;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ZenCallDatabase_Impl extends ZenCallDatabase {
  private volatile ModeDao _modeDao;

  private volatile CategoryDao _categoryDao;

  private volatile RuleDao _ruleDao;

  private volatile NumberDao _numberDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `call_modes` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `isEnabled` INTEGER NOT NULL, `defaultAction` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `caller_categories` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `isSystemDefault` INTEGER NOT NULL, `description` TEXT, `colorHex` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `call_rules` (`id` TEXT NOT NULL, `modeId` TEXT NOT NULL, `categoryId` TEXT NOT NULL, `action` TEXT NOT NULL, `priority` INTEGER NOT NULL, `isScheduleEnabled` INTEGER NOT NULL, `scheduleDaysCsv` TEXT NOT NULL, `startMinuteOfDay` INTEGER NOT NULL, `endMinuteOfDay` INTEGER NOT NULL, `outsideScheduleAction` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`modeId`) REFERENCES `call_modes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`categoryId`) REFERENCES `caller_categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_call_rules_modeId` ON `call_rules` (`modeId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_call_rules_categoryId` ON `call_rules` (`categoryId`)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_call_rules_modeId_categoryId` ON `call_rules` (`modeId`, `categoryId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `category_numbers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `categoryId` TEXT NOT NULL, `rawNumber` TEXT NOT NULL, `normalizedNumber` TEXT NOT NULL, `displayName` TEXT, `addedAt` INTEGER NOT NULL, FOREIGN KEY(`categoryId`) REFERENCES `caller_categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_category_numbers_categoryId` ON `category_numbers` (`categoryId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_category_numbers_normalizedNumber` ON `category_numbers` (`normalizedNumber`)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_category_numbers_categoryId_normalizedNumber` ON `category_numbers` (`categoryId`, `normalizedNumber`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '70c2d0f385efb90d0ef6037bd0d3cf11')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `call_modes`");
        db.execSQL("DROP TABLE IF EXISTS `caller_categories`");
        db.execSQL("DROP TABLE IF EXISTS `call_rules`");
        db.execSQL("DROP TABLE IF EXISTS `category_numbers`");
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
        db.execSQL("PRAGMA foreign_keys = ON");
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
        final HashMap<String, TableInfo.Column> _columnsCallModes = new HashMap<String, TableInfo.Column>(6);
        _columnsCallModes.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallModes.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallModes.put("isEnabled", new TableInfo.Column("isEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallModes.put("defaultAction", new TableInfo.Column("defaultAction", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallModes.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallModes.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCallModes = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCallModes = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCallModes = new TableInfo("call_modes", _columnsCallModes, _foreignKeysCallModes, _indicesCallModes);
        final TableInfo _existingCallModes = TableInfo.read(db, "call_modes");
        if (!_infoCallModes.equals(_existingCallModes)) {
          return new RoomOpenHelper.ValidationResult(false, "call_modes(pro.nitron.zencall.data.local.entity.CallModeEntity).\n"
                  + " Expected:\n" + _infoCallModes + "\n"
                  + " Found:\n" + _existingCallModes);
        }
        final HashMap<String, TableInfo.Column> _columnsCallerCategories = new HashMap<String, TableInfo.Column>(6);
        _columnsCallerCategories.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallerCategories.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallerCategories.put("isSystemDefault", new TableInfo.Column("isSystemDefault", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallerCategories.put("description", new TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallerCategories.put("colorHex", new TableInfo.Column("colorHex", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallerCategories.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCallerCategories = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCallerCategories = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCallerCategories = new TableInfo("caller_categories", _columnsCallerCategories, _foreignKeysCallerCategories, _indicesCallerCategories);
        final TableInfo _existingCallerCategories = TableInfo.read(db, "caller_categories");
        if (!_infoCallerCategories.equals(_existingCallerCategories)) {
          return new RoomOpenHelper.ValidationResult(false, "caller_categories(pro.nitron.zencall.data.local.entity.CallerCategoryEntity).\n"
                  + " Expected:\n" + _infoCallerCategories + "\n"
                  + " Found:\n" + _existingCallerCategories);
        }
        final HashMap<String, TableInfo.Column> _columnsCallRules = new HashMap<String, TableInfo.Column>(10);
        _columnsCallRules.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallRules.put("modeId", new TableInfo.Column("modeId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallRules.put("categoryId", new TableInfo.Column("categoryId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallRules.put("action", new TableInfo.Column("action", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallRules.put("priority", new TableInfo.Column("priority", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallRules.put("isScheduleEnabled", new TableInfo.Column("isScheduleEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallRules.put("scheduleDaysCsv", new TableInfo.Column("scheduleDaysCsv", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallRules.put("startMinuteOfDay", new TableInfo.Column("startMinuteOfDay", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallRules.put("endMinuteOfDay", new TableInfo.Column("endMinuteOfDay", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallRules.put("outsideScheduleAction", new TableInfo.Column("outsideScheduleAction", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCallRules = new HashSet<TableInfo.ForeignKey>(2);
        _foreignKeysCallRules.add(new TableInfo.ForeignKey("call_modes", "CASCADE", "NO ACTION", Arrays.asList("modeId"), Arrays.asList("id")));
        _foreignKeysCallRules.add(new TableInfo.ForeignKey("caller_categories", "CASCADE", "NO ACTION", Arrays.asList("categoryId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesCallRules = new HashSet<TableInfo.Index>(3);
        _indicesCallRules.add(new TableInfo.Index("index_call_rules_modeId", false, Arrays.asList("modeId"), Arrays.asList("ASC")));
        _indicesCallRules.add(new TableInfo.Index("index_call_rules_categoryId", false, Arrays.asList("categoryId"), Arrays.asList("ASC")));
        _indicesCallRules.add(new TableInfo.Index("index_call_rules_modeId_categoryId", true, Arrays.asList("modeId", "categoryId"), Arrays.asList("ASC", "ASC")));
        final TableInfo _infoCallRules = new TableInfo("call_rules", _columnsCallRules, _foreignKeysCallRules, _indicesCallRules);
        final TableInfo _existingCallRules = TableInfo.read(db, "call_rules");
        if (!_infoCallRules.equals(_existingCallRules)) {
          return new RoomOpenHelper.ValidationResult(false, "call_rules(pro.nitron.zencall.data.local.entity.CallRuleEntity).\n"
                  + " Expected:\n" + _infoCallRules + "\n"
                  + " Found:\n" + _existingCallRules);
        }
        final HashMap<String, TableInfo.Column> _columnsCategoryNumbers = new HashMap<String, TableInfo.Column>(6);
        _columnsCategoryNumbers.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategoryNumbers.put("categoryId", new TableInfo.Column("categoryId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategoryNumbers.put("rawNumber", new TableInfo.Column("rawNumber", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategoryNumbers.put("normalizedNumber", new TableInfo.Column("normalizedNumber", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategoryNumbers.put("displayName", new TableInfo.Column("displayName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategoryNumbers.put("addedAt", new TableInfo.Column("addedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCategoryNumbers = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysCategoryNumbers.add(new TableInfo.ForeignKey("caller_categories", "CASCADE", "NO ACTION", Arrays.asList("categoryId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesCategoryNumbers = new HashSet<TableInfo.Index>(3);
        _indicesCategoryNumbers.add(new TableInfo.Index("index_category_numbers_categoryId", false, Arrays.asList("categoryId"), Arrays.asList("ASC")));
        _indicesCategoryNumbers.add(new TableInfo.Index("index_category_numbers_normalizedNumber", false, Arrays.asList("normalizedNumber"), Arrays.asList("ASC")));
        _indicesCategoryNumbers.add(new TableInfo.Index("index_category_numbers_categoryId_normalizedNumber", true, Arrays.asList("categoryId", "normalizedNumber"), Arrays.asList("ASC", "ASC")));
        final TableInfo _infoCategoryNumbers = new TableInfo("category_numbers", _columnsCategoryNumbers, _foreignKeysCategoryNumbers, _indicesCategoryNumbers);
        final TableInfo _existingCategoryNumbers = TableInfo.read(db, "category_numbers");
        if (!_infoCategoryNumbers.equals(_existingCategoryNumbers)) {
          return new RoomOpenHelper.ValidationResult(false, "category_numbers(pro.nitron.zencall.data.local.entity.CategoryNumberEntity).\n"
                  + " Expected:\n" + _infoCategoryNumbers + "\n"
                  + " Found:\n" + _existingCategoryNumbers);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "70c2d0f385efb90d0ef6037bd0d3cf11", "e37baa94a69946efdacf0efc862ddf81");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "call_modes","caller_categories","call_rules","category_numbers");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `call_modes`");
      _db.execSQL("DELETE FROM `caller_categories`");
      _db.execSQL("DELETE FROM `call_rules`");
      _db.execSQL("DELETE FROM `category_numbers`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
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
    _typeConvertersMap.put(ModeDao.class, ModeDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CategoryDao.class, CategoryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(RuleDao.class, RuleDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(NumberDao.class, NumberDao_Impl.getRequiredConverters());
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
  public ModeDao modeDao() {
    if (_modeDao != null) {
      return _modeDao;
    } else {
      synchronized(this) {
        if(_modeDao == null) {
          _modeDao = new ModeDao_Impl(this);
        }
        return _modeDao;
      }
    }
  }

  @Override
  public CategoryDao categoryDao() {
    if (_categoryDao != null) {
      return _categoryDao;
    } else {
      synchronized(this) {
        if(_categoryDao == null) {
          _categoryDao = new CategoryDao_Impl(this);
        }
        return _categoryDao;
      }
    }
  }

  @Override
  public RuleDao ruleDao() {
    if (_ruleDao != null) {
      return _ruleDao;
    } else {
      synchronized(this) {
        if(_ruleDao == null) {
          _ruleDao = new RuleDao_Impl(this);
        }
        return _ruleDao;
      }
    }
  }

  @Override
  public NumberDao numberDao() {
    if (_numberDao != null) {
      return _numberDao;
    } else {
      synchronized(this) {
        if(_numberDao == null) {
          _numberDao = new NumberDao_Impl(this);
        }
        return _numberDao;
      }
    }
  }
}
