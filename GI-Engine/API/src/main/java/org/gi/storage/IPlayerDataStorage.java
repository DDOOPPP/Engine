package org.gi.storage;

import org.gi.Result;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 플레이어 데이터 저장소 인터페이스
 *
 * 비동기 작업을 위해 CompletableFuture 사용
 */
public interface IPlayerDataStorage {
    Result initialize();

    void shutdown();

    /**
     * 플레이어 데이터 저장
     */
    CompletableFuture<Result> save(PlayerStatData data);

    /**
     * 플레이어 데이터 로드
     */
    CompletableFuture<Optional<PlayerStatData>> load(UUID playerUUID);

    /**
     * 플레이어 데이터 삭제
     */
    CompletableFuture<Result> delete(UUID playerUUID);

    /**
     * 플레이어 데이터 존재 여부
     */
    CompletableFuture<Boolean> exists(UUID playerUUID);

    /**
     * 저장소 타입
     */
    StorageType getType();

    /**
     * 연결 상태
     */
    boolean isConnected();
}
