package pro.nitron.zencall.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;
import pro.nitron.zencall.data.local.entity.CallerCategoryEntity;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class CategoryDao_Impl implements CategoryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CallerCategoryEntity> __insertionAdapterOfCallerCategoryEntity;

  private final EntityInsertionAdapter<CallerCategoryEntity> __insertionAdapterOfCallerCategoryEntity_1;

  private final EntityDeletionOrUpdateAdapter<CallerCategoryEntity> __deletionAdapterOfCallerCategoryEntity;

  public CategoryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCallerCategoryEntity = new EntityInsertionAdapter<CallerCategoryEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `caller_categories` (`id`,`name`,`isSystemDefault`,`description`,`colorHex`,`createdAt`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CallerCategoryEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        final int _tmp = entity.isSystemDefault() ? 1 : 0;
        statement.bindLong(3, _tmp);
        if (entity.getDescription() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getDescription());
        }
        if (entity.getColorHex() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getColorHex());
        }
        statement.bindLong(6, entity.getCreatedAt());
      }
    };
    this.__insertionAdapterOfCallerCategoryEntity_1 = new EntityInsertionAdapter<CallerCategoryEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `caller_categories` (`id`,`name`,`isSystemDefault`,`description`,`colorHex`,`createdAt`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CallerCategoryEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        final int _tmp = entity.isSystemDefault() ? 1 : 0;
        statement.bindLong(3, _tmp);
        if (entity.getDescription() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getDescription());
        }
        if (entity.getColorHex() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getColorHex());
        }
        statement.bindLong(6, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfCallerCategoryEntity = new EntityDeletionOrUpdateAdapter<CallerCategoryEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `caller_categories` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CallerCategoryEntity entity) {
        statement.bindString(1, entity.getId());
      }
    };
  }

  @Override
  public Object insertOrUpdateCategory(final CallerCategoryEntity category,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCallerCategoryEntity.insert(category);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertCategories(final List<CallerCategoryEntity> categories,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCallerCategoryEntity_1.insert(categories);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteCategory(final CallerCategoryEntity category,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfCallerCategoryEntity.handle(category);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<CallerCategoryEntity>> getAllCategoriesFlow() {
    final String _sql = "SELECT * FROM caller_categories ORDER BY isSystemDefault DESC, name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"caller_categories"}, new Callable<List<CallerCategoryEntity>>() {
      @Override
      @NonNull
      public List<CallerCategoryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfIsSystemDefault = CursorUtil.getColumnIndexOrThrow(_cursor, "isSystemDefault");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfColorHex = CursorUtil.getColumnIndexOrThrow(_cursor, "colorHex");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<CallerCategoryEntity> _result = new ArrayList<CallerCategoryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CallerCategoryEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final boolean _tmpIsSystemDefault;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsSystemDefault);
            _tmpIsSystemDefault = _tmp != 0;
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final String _tmpColorHex;
            if (_cursor.isNull(_cursorIndexOfColorHex)) {
              _tmpColorHex = null;
            } else {
              _tmpColorHex = _cursor.getString(_cursorIndexOfColorHex);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new CallerCategoryEntity(_tmpId,_tmpName,_tmpIsSystemDefault,_tmpDescription,_tmpColorHex,_tmpCreatedAt);
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
  public Object getAllCategoriesSync(
      final Continuation<? super List<CallerCategoryEntity>> $completion) {
    final String _sql = "SELECT * FROM caller_categories";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CallerCategoryEntity>>() {
      @Override
      @NonNull
      public List<CallerCategoryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfIsSystemDefault = CursorUtil.getColumnIndexOrThrow(_cursor, "isSystemDefault");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfColorHex = CursorUtil.getColumnIndexOrThrow(_cursor, "colorHex");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<CallerCategoryEntity> _result = new ArrayList<CallerCategoryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CallerCategoryEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final boolean _tmpIsSystemDefault;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsSystemDefault);
            _tmpIsSystemDefault = _tmp != 0;
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final String _tmpColorHex;
            if (_cursor.isNull(_cursorIndexOfColorHex)) {
              _tmpColorHex = null;
            } else {
              _tmpColorHex = _cursor.getString(_cursorIndexOfColorHex);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new CallerCategoryEntity(_tmpId,_tmpName,_tmpIsSystemDefault,_tmpDescription,_tmpColorHex,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getCategoryById(final String id,
      final Continuation<? super CallerCategoryEntity> $completion) {
    final String _sql = "SELECT * FROM caller_categories WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<CallerCategoryEntity>() {
      @Override
      @Nullable
      public CallerCategoryEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfIsSystemDefault = CursorUtil.getColumnIndexOrThrow(_cursor, "isSystemDefault");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfColorHex = CursorUtil.getColumnIndexOrThrow(_cursor, "colorHex");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final CallerCategoryEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final boolean _tmpIsSystemDefault;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsSystemDefault);
            _tmpIsSystemDefault = _tmp != 0;
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final String _tmpColorHex;
            if (_cursor.isNull(_cursorIndexOfColorHex)) {
              _tmpColorHex = null;
            } else {
              _tmpColorHex = _cursor.getString(_cursorIndexOfColorHex);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new CallerCategoryEntity(_tmpId,_tmpName,_tmpIsSystemDefault,_tmpDescription,_tmpColorHex,_tmpCreatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
