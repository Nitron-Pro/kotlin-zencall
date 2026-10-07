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
import java.lang.IllegalArgumentException;
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
import pro.nitron.zencall.data.local.entity.CallModeEntity;
import pro.nitron.zencall.domain.model.CallAction;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ModeDao_Impl implements ModeDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CallModeEntity> __insertionAdapterOfCallModeEntity;

  private final EntityInsertionAdapter<CallModeEntity> __insertionAdapterOfCallModeEntity_1;

  private final EntityDeletionOrUpdateAdapter<CallModeEntity> __deletionAdapterOfCallModeEntity;

  public ModeDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCallModeEntity = new EntityInsertionAdapter<CallModeEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `call_modes` (`id`,`name`,`isEnabled`,`defaultAction`,`createdAt`,`updatedAt`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CallModeEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        final int _tmp = entity.isEnabled() ? 1 : 0;
        statement.bindLong(3, _tmp);
        statement.bindString(4, __CallAction_enumToString(entity.getDefaultAction()));
        statement.bindLong(5, entity.getCreatedAt());
        statement.bindLong(6, entity.getUpdatedAt());
      }
    };
    this.__insertionAdapterOfCallModeEntity_1 = new EntityInsertionAdapter<CallModeEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `call_modes` (`id`,`name`,`isEnabled`,`defaultAction`,`createdAt`,`updatedAt`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CallModeEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        final int _tmp = entity.isEnabled() ? 1 : 0;
        statement.bindLong(3, _tmp);
        statement.bindString(4, __CallAction_enumToString(entity.getDefaultAction()));
        statement.bindLong(5, entity.getCreatedAt());
        statement.bindLong(6, entity.getUpdatedAt());
      }
    };
    this.__deletionAdapterOfCallModeEntity = new EntityDeletionOrUpdateAdapter<CallModeEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `call_modes` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CallModeEntity entity) {
        statement.bindString(1, entity.getId());
      }
    };
  }

  @Override
  public Object insertOrUpdateMode(final CallModeEntity mode,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCallModeEntity.insert(mode);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertModes(final List<CallModeEntity> modes,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCallModeEntity_1.insert(modes);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteMode(final CallModeEntity mode,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfCallModeEntity.handle(mode);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<CallModeEntity>> getAllModesFlow() {
    final String _sql = "SELECT * FROM call_modes ORDER BY createdAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"call_modes"}, new Callable<List<CallModeEntity>>() {
      @Override
      @NonNull
      public List<CallModeEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfIsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "isEnabled");
          final int _cursorIndexOfDefaultAction = CursorUtil.getColumnIndexOrThrow(_cursor, "defaultAction");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<CallModeEntity> _result = new ArrayList<CallModeEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CallModeEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final boolean _tmpIsEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsEnabled);
            _tmpIsEnabled = _tmp != 0;
            final CallAction _tmpDefaultAction;
            _tmpDefaultAction = __CallAction_stringToEnum(_cursor.getString(_cursorIndexOfDefaultAction));
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new CallModeEntity(_tmpId,_tmpName,_tmpIsEnabled,_tmpDefaultAction,_tmpCreatedAt,_tmpUpdatedAt);
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
  public Object getModeById(final String id,
      final Continuation<? super CallModeEntity> $completion) {
    final String _sql = "SELECT * FROM call_modes WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<CallModeEntity>() {
      @Override
      @Nullable
      public CallModeEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfIsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "isEnabled");
          final int _cursorIndexOfDefaultAction = CursorUtil.getColumnIndexOrThrow(_cursor, "defaultAction");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final CallModeEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final boolean _tmpIsEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsEnabled);
            _tmpIsEnabled = _tmp != 0;
            final CallAction _tmpDefaultAction;
            _tmpDefaultAction = __CallAction_stringToEnum(_cursor.getString(_cursorIndexOfDefaultAction));
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new CallModeEntity(_tmpId,_tmpName,_tmpIsEnabled,_tmpDefaultAction,_tmpCreatedAt,_tmpUpdatedAt);
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

  private String __CallAction_enumToString(@NonNull final CallAction _value) {
    switch (_value) {
      case ALLOW: return "ALLOW";
      case SILENCE: return "SILENCE";
      case REJECT: return "REJECT";
      default: throw new IllegalArgumentException("Can't convert enum to string, unknown enum value: " + _value);
    }
  }

  private CallAction __CallAction_stringToEnum(@NonNull final String _value) {
    switch (_value) {
      case "ALLOW": return CallAction.ALLOW;
      case "SILENCE": return CallAction.SILENCE;
      case "REJECT": return CallAction.REJECT;
      default: throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + _value);
    }
  }
}
