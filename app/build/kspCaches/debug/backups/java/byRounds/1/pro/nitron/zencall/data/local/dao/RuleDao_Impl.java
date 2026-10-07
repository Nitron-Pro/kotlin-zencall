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
import pro.nitron.zencall.data.local.entity.CallRuleEntity;
import pro.nitron.zencall.domain.model.CallAction;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class RuleDao_Impl implements RuleDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CallRuleEntity> __insertionAdapterOfCallRuleEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteRule;

  public RuleDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCallRuleEntity = new EntityInsertionAdapter<CallRuleEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `call_rules` (`id`,`modeId`,`categoryId`,`action`,`priority`,`isScheduleEnabled`,`scheduleDaysCsv`,`startMinuteOfDay`,`endMinuteOfDay`,`outsideScheduleAction`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CallRuleEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getModeId());
        statement.bindString(3, entity.getCategoryId());
        statement.bindString(4, __CallAction_enumToString(entity.getAction()));
        statement.bindLong(5, entity.getPriority());
        final int _tmp = entity.isScheduleEnabled() ? 1 : 0;
        statement.bindLong(6, _tmp);
        statement.bindString(7, entity.getScheduleDaysCsv());
        statement.bindLong(8, entity.getStartMinuteOfDay());
        statement.bindLong(9, entity.getEndMinuteOfDay());
        if (entity.getOutsideScheduleAction() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, __CallAction_enumToString(entity.getOutsideScheduleAction()));
        }
      }
    };
    this.__preparedStmtOfDeleteRule = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM call_rules WHERE modeId = ? AND categoryId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertOrUpdateRule(final CallRuleEntity rule,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCallRuleEntity.insert(rule);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertRules(final List<CallRuleEntity> rules,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCallRuleEntity.insert(rules);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteRule(final String modeId, final String categoryId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteRule.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, modeId);
        _argIndex = 2;
        _stmt.bindString(_argIndex, categoryId);
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
          __preparedStmtOfDeleteRule.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<CallRuleEntity>> getRulesForModeFlow(final String modeId) {
    final String _sql = "SELECT * FROM call_rules WHERE modeId = ? ORDER BY priority DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, modeId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"call_rules"}, new Callable<List<CallRuleEntity>>() {
      @Override
      @NonNull
      public List<CallRuleEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfModeId = CursorUtil.getColumnIndexOrThrow(_cursor, "modeId");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfAction = CursorUtil.getColumnIndexOrThrow(_cursor, "action");
          final int _cursorIndexOfPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "priority");
          final int _cursorIndexOfIsScheduleEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "isScheduleEnabled");
          final int _cursorIndexOfScheduleDaysCsv = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduleDaysCsv");
          final int _cursorIndexOfStartMinuteOfDay = CursorUtil.getColumnIndexOrThrow(_cursor, "startMinuteOfDay");
          final int _cursorIndexOfEndMinuteOfDay = CursorUtil.getColumnIndexOrThrow(_cursor, "endMinuteOfDay");
          final int _cursorIndexOfOutsideScheduleAction = CursorUtil.getColumnIndexOrThrow(_cursor, "outsideScheduleAction");
          final List<CallRuleEntity> _result = new ArrayList<CallRuleEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CallRuleEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpModeId;
            _tmpModeId = _cursor.getString(_cursorIndexOfModeId);
            final String _tmpCategoryId;
            _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            final CallAction _tmpAction;
            _tmpAction = __CallAction_stringToEnum(_cursor.getString(_cursorIndexOfAction));
            final int _tmpPriority;
            _tmpPriority = _cursor.getInt(_cursorIndexOfPriority);
            final boolean _tmpIsScheduleEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsScheduleEnabled);
            _tmpIsScheduleEnabled = _tmp != 0;
            final String _tmpScheduleDaysCsv;
            _tmpScheduleDaysCsv = _cursor.getString(_cursorIndexOfScheduleDaysCsv);
            final int _tmpStartMinuteOfDay;
            _tmpStartMinuteOfDay = _cursor.getInt(_cursorIndexOfStartMinuteOfDay);
            final int _tmpEndMinuteOfDay;
            _tmpEndMinuteOfDay = _cursor.getInt(_cursorIndexOfEndMinuteOfDay);
            final CallAction _tmpOutsideScheduleAction;
            if (_cursor.isNull(_cursorIndexOfOutsideScheduleAction)) {
              _tmpOutsideScheduleAction = null;
            } else {
              _tmpOutsideScheduleAction = __CallAction_stringToEnum(_cursor.getString(_cursorIndexOfOutsideScheduleAction));
            }
            _item = new CallRuleEntity(_tmpId,_tmpModeId,_tmpCategoryId,_tmpAction,_tmpPriority,_tmpIsScheduleEnabled,_tmpScheduleDaysCsv,_tmpStartMinuteOfDay,_tmpEndMinuteOfDay,_tmpOutsideScheduleAction);
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
  public Object getRulesForModeSync(final String modeId,
      final Continuation<? super List<CallRuleEntity>> $completion) {
    final String _sql = "SELECT * FROM call_rules WHERE modeId = ? ORDER BY priority DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, modeId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CallRuleEntity>>() {
      @Override
      @NonNull
      public List<CallRuleEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfModeId = CursorUtil.getColumnIndexOrThrow(_cursor, "modeId");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfAction = CursorUtil.getColumnIndexOrThrow(_cursor, "action");
          final int _cursorIndexOfPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "priority");
          final int _cursorIndexOfIsScheduleEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "isScheduleEnabled");
          final int _cursorIndexOfScheduleDaysCsv = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduleDaysCsv");
          final int _cursorIndexOfStartMinuteOfDay = CursorUtil.getColumnIndexOrThrow(_cursor, "startMinuteOfDay");
          final int _cursorIndexOfEndMinuteOfDay = CursorUtil.getColumnIndexOrThrow(_cursor, "endMinuteOfDay");
          final int _cursorIndexOfOutsideScheduleAction = CursorUtil.getColumnIndexOrThrow(_cursor, "outsideScheduleAction");
          final List<CallRuleEntity> _result = new ArrayList<CallRuleEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CallRuleEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpModeId;
            _tmpModeId = _cursor.getString(_cursorIndexOfModeId);
            final String _tmpCategoryId;
            _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            final CallAction _tmpAction;
            _tmpAction = __CallAction_stringToEnum(_cursor.getString(_cursorIndexOfAction));
            final int _tmpPriority;
            _tmpPriority = _cursor.getInt(_cursorIndexOfPriority);
            final boolean _tmpIsScheduleEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsScheduleEnabled);
            _tmpIsScheduleEnabled = _tmp != 0;
            final String _tmpScheduleDaysCsv;
            _tmpScheduleDaysCsv = _cursor.getString(_cursorIndexOfScheduleDaysCsv);
            final int _tmpStartMinuteOfDay;
            _tmpStartMinuteOfDay = _cursor.getInt(_cursorIndexOfStartMinuteOfDay);
            final int _tmpEndMinuteOfDay;
            _tmpEndMinuteOfDay = _cursor.getInt(_cursorIndexOfEndMinuteOfDay);
            final CallAction _tmpOutsideScheduleAction;
            if (_cursor.isNull(_cursorIndexOfOutsideScheduleAction)) {
              _tmpOutsideScheduleAction = null;
            } else {
              _tmpOutsideScheduleAction = __CallAction_stringToEnum(_cursor.getString(_cursorIndexOfOutsideScheduleAction));
            }
            _item = new CallRuleEntity(_tmpId,_tmpModeId,_tmpCategoryId,_tmpAction,_tmpPriority,_tmpIsScheduleEnabled,_tmpScheduleDaysCsv,_tmpStartMinuteOfDay,_tmpEndMinuteOfDay,_tmpOutsideScheduleAction);
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
