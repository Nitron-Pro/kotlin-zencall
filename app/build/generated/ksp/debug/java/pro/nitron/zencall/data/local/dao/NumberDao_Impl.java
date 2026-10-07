package pro.nitron.zencall.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
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
import pro.nitron.zencall.data.local.entity.CategoryNumberEntity;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class NumberDao_Impl implements NumberDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CategoryNumberEntity> __insertionAdapterOfCategoryNumberEntity;

  private final SharedSQLiteStatement __preparedStmtOfRemoveNumber;

  private final SharedSQLiteStatement __preparedStmtOfDeleteNumberById;

  public NumberDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCategoryNumberEntity = new EntityInsertionAdapter<CategoryNumberEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `category_numbers` (`id`,`categoryId`,`rawNumber`,`normalizedNumber`,`displayName`,`addedAt`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CategoryNumberEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getCategoryId());
        statement.bindString(3, entity.getRawNumber());
        statement.bindString(4, entity.getNormalizedNumber());
        if (entity.getDisplayName() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getDisplayName());
        }
        statement.bindLong(6, entity.getAddedAt());
      }
    };
    this.__preparedStmtOfRemoveNumber = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM category_numbers WHERE categoryId = ? AND normalizedNumber = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteNumberById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM category_numbers WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertNumber(final CategoryNumberEntity number,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCategoryNumberEntity.insert(number);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object removeNumber(final String categoryId, final String normalizedNumber,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfRemoveNumber.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, categoryId);
        _argIndex = 2;
        _stmt.bindString(_argIndex, normalizedNumber);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfRemoveNumber.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteNumberById(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteNumberById.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteNumberById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<CategoryNumberEntity>> getNumbersForCategoryFlow(final String categoryId) {
    final String _sql = "SELECT * FROM category_numbers WHERE categoryId = ? ORDER BY addedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, categoryId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"category_numbers"}, new Callable<List<CategoryNumberEntity>>() {
      @Override
      @NonNull
      public List<CategoryNumberEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfRawNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "rawNumber");
          final int _cursorIndexOfNormalizedNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "normalizedNumber");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final List<CategoryNumberEntity> _result = new ArrayList<CategoryNumberEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CategoryNumberEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpCategoryId;
            _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            final String _tmpRawNumber;
            _tmpRawNumber = _cursor.getString(_cursorIndexOfRawNumber);
            final String _tmpNormalizedNumber;
            _tmpNormalizedNumber = _cursor.getString(_cursorIndexOfNormalizedNumber);
            final String _tmpDisplayName;
            if (_cursor.isNull(_cursorIndexOfDisplayName)) {
              _tmpDisplayName = null;
            } else {
              _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            }
            final long _tmpAddedAt;
            _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            _item = new CategoryNumberEntity(_tmpId,_tmpCategoryId,_tmpRawNumber,_tmpNormalizedNumber,_tmpDisplayName,_tmpAddedAt);
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
  public Object getAllNumbersSync(
      final Continuation<? super List<CategoryNumberEntity>> $completion) {
    final String _sql = "SELECT * FROM category_numbers";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CategoryNumberEntity>>() {
      @Override
      @NonNull
      public List<CategoryNumberEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfRawNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "rawNumber");
          final int _cursorIndexOfNormalizedNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "normalizedNumber");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final List<CategoryNumberEntity> _result = new ArrayList<CategoryNumberEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CategoryNumberEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpCategoryId;
            _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            final String _tmpRawNumber;
            _tmpRawNumber = _cursor.getString(_cursorIndexOfRawNumber);
            final String _tmpNormalizedNumber;
            _tmpNormalizedNumber = _cursor.getString(_cursorIndexOfNormalizedNumber);
            final String _tmpDisplayName;
            if (_cursor.isNull(_cursorIndexOfDisplayName)) {
              _tmpDisplayName = null;
            } else {
              _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            }
            final long _tmpAddedAt;
            _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            _item = new CategoryNumberEntity(_tmpId,_tmpCategoryId,_tmpRawNumber,_tmpNormalizedNumber,_tmpDisplayName,_tmpAddedAt);
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
  public Object getCategoryIdsForNumber(final String normalizedNumber,
      final Continuation<? super List<String>> $completion) {
    final String _sql = "SELECT categoryId FROM category_numbers WHERE normalizedNumber = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, normalizedNumber);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<String>>() {
      @Override
      @NonNull
      public List<String> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<String> _result = new ArrayList<String>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final String _item;
            _item = _cursor.getString(0);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
