# GI-Engine 코드 리뷰 및 수정 내역

**날짜:** 2026-01-18
**작업자:** Claude Code 분석 및 사용자 수정

---

## 개요

GI-Engine 프로젝트의 코드 분석을 통해 P0(심각), P1(중요), P2(보통) 이슈를 식별하고 수정했습니다.
스킬 시스템을 제외한 스탯, 데미지, 스토리지 시스템을 분석했습니다.

---

## P0 (심각) - 4건 수정 완료

### 1. MySQL 설정 null 체크
**파일:** `GIEngine.java:124-125`

**문제:** `config.getSection("storage.mysql")`이 null일 때 NPE 발생

**수정:**
```java
ConfigurationSection section = config.getSection("storage.mysql");

if (section == null) {
    getLogger().severe("MySQL configuration section not found! Falling back to SQLite.");
    yield new SQLiteStorage(getLogger(), getDataFolder(), "player_data.db");
}
```

---

### 2. StatInstance 캐시 경쟁 조건
**파일:** `StatInstance.java`

**문제:** `cacheValid`가 volatile이지만 `recalculate()`가 동기화되지 않아 동시 접근 시 불일치 발생

**수정:**
```java
// 필드 추가
private final Object cacheLock = new Object();

// recalculate() 메서드
private void recalculate(){
    synchronized (cacheLock) {
        if (cacheValid) return;  // Double-check

        // ... 계산 로직 ...

        cacheValid = true;
    }
}
```

---

### 3. getOrLoad() 메인스레드 블로킹 제거
**파일:** `PlayerStatManager.java`

**문제:** `Thread.sleep(100)` 최대 5초 블로킹 → 서버 프리징 위험

**수정:**
- `getOrLoad()` 메서드 완전 삭제
- 대신 `getHolderIfReady()` 사용 (비블로킹)

**영향받는 파일:**
- `StatCommand.java:57` - `getHolder()` + null 체크로 변경
- `DamageCommand.java:109, 110, 157, 239` - 동일하게 변경

---

## P1 (중요) - 4건 수정 완료

### 1. MySQLStorage HikariCP 예외처리
**파일:** `MySQLStorage.java:39`

**문제:** `new HikariDataSource(config)` 예외 발생 시 처리 없음

**수정:**
```java
try {
    dataSource = new HikariDataSource(config);
} catch (Exception e) {
    logger.severe("Failed to create MySQL connection pool: " + e.getMessage());
    return Result.Exception(e);
}
```

---

### 2. SQLiteStorage 커넥션 동기화
**파일:** `SQLiteStorage.java`

**문제:** 단일 커넥션에 동기화 없이 접근

**수정:**
```java
private final Object connectionLock = new Object();

@Override
public Connection getConnection() throws SQLException {
    synchronized (connectionLock) {
        if (connection == null || connection.isClosed()) {
            connection = createConnection();
            logger.info("SQLite connection recreated");
        }
        return connection;
    }
}
```

---

### 3. EngineAPI 싱글턴 경쟁 조건
**파일:** `EngineAPI.java:17-23`

**문제:** 동시 호출 시 인스턴스 중복 생성 가능

**수정:**
```java
private static final Object lock = new Object();

public static Result initialize(IStatRegistry statRegistry, IDamageCalculator damageCalculator){
    synchronized (lock) {
        if (instance != null){
            return Result.Error("EngineAPI is already initialized");
        }
        instance = new EngineAPI(statRegistry,damageCalculator);
        return Result.SUCCESS;
    }
}
```

---

### 4. StatHolder printStackTrace 수정
**파일:** `StatHolder.java:108-115`

**문제:** `e.printStackTrace()` 사용 → 로거로 변경

**수정:**
```java
private static final Logger logger = Logger.getLogger(StatHolder.class.getName());

protected void notifyStatChange(String statId, double oldValue, double newValue){
    for (IStatChangeListener listener : statChangeListeners){
        try{
            listener.onStatChange(this, statId, oldValue, newValue);
        }catch (Exception e){
            logger.warning("StatChangeListener error: " + e.getMessage());
        }
    }
}
```

---

## P2 (보통) - 1건 수정 완료

### 저장 실패 시 재시도 로직
**파일:** `PlayerStatManager.java`

**문제:** 저장 실패 시 로그만 찍고 데이터 손실

**수정:**
```java
public CompletableFuture<Void> save(PlayerStatHolder holder){
    return saveRetry(holder, 3);  // 최대 3회 재시도
}

private CompletableFuture<Void> saveRetry(PlayerStatHolder holder, int retryCount){
    // ... 데이터 준비 ...

    return storage.save(data)
            .thenAccept(result -> { ... })
            .exceptionally(e -> {
                if (retryCount > 0){
                    logger.warning("retrying... leftCount: "+(retryCount-1));
                    saveRetry(holder, retryCount-1);
                }else{
                    logger.warning("Failed to save PlayerData: "+data.getPlayerUUID());
                }
                return null;
            });
}
```

**추가:** `storageAvailable()` 헬퍼 메서드 추가

---

## 수정된 파일 목록

| 모듈 | 파일 | 수정 내용 |
|------|------|----------|
| src | `GIEngine.java` | MySQL 설정 null 체크 |
| src | `PlayerStatManager.java` | getOrLoad 삭제, saveRetry 추가, storageAvailable 추가 |
| src | `StatCommand.java` | getOrLoad → getHolder 변경 |
| src | `DamageCommand.java` | getOrLoad → getHolder 변경 |
| Core | `StatInstance.java` | cacheLock 동기화 추가 |
| Core | `StatHolder.java` | Logger 사용으로 변경 |
| Core | `MySQLStorage.java` | HikariCP try-catch 추가 |
| Core | `SQLiteStorage.java` | connectionLock 동기화 추가 |
| API | `EngineAPI.java` | 싱글턴 lock 동기화 추가 |

---

## 이전 수정 내역 (이번 세션 이전)

- AbstractStorage 커넥션 누수 수정
- MySQLStorage shutdown 수정
- SQLite 단일 스레드 executor
- DamageListener 몬스터 지원 추가
- DamageCalculator null 체크 추가
- getHolderIfReady() 메서드 추가

---

## 남은 P2 이슈 (선택사항)

수정하지 않은 P2 이슈 (실익 없어 스킵):

1. StatInstance 스트림 비효율 - 성능 차이 미미
2. GIConfig 기본값 없음 - 현재 동작에 문제 없음
3. 시간 계산 오버플로우 - 실제 발생 가능성 극히 낮음
4. MySQL URL 검증 없음 - 연결 시 에러로 감지됨
5. getSourceID() 미사용 - 향후 사용 가능성 있어 보류
