package org.gi;

import org.gi.stat.IStatRegistry;

public final class EngineAPI {
    private static EngineAPI instance;

    private final IStatRegistry statRegistry;

    private EngineAPI(IStatRegistry statRegistry){
        this.statRegistry = statRegistry;
    }

    public static Result initialize(IStatRegistry statRegistry){
        if (instance != null){
            return Result.Error("EngineAPI is already initialized");
        }
        instance = new EngineAPI(statRegistry);
        return Result.SUCCESS;
    }

    public static EngineAPI getAPI(){
        if (instance == null){
            throw new IllegalStateException("EngineAPI is not initialized");
        }
        return instance;
    }

    //크게 의미가 있을까?
    public static boolean isInitialized(){
        return instance != null;
    }

    public IStatRegistry getStatRegistry(){
        return statRegistry;
    }

    public static void shutdown(){
        instance = null;
    }
}
