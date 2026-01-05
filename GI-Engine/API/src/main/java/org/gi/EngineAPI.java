package org.gi;

import org.gi.damage.IDamageCalculator;
import org.gi.stat.IStatRegistry;

public final class EngineAPI {
    private static EngineAPI instance;

    private final IStatRegistry statRegistry;
    private final IDamageCalculator damageCalculator;

    private EngineAPI(IStatRegistry statRegistry, IDamageCalculator damageCalculator){
        this.statRegistry = statRegistry;
        this.damageCalculator = damageCalculator;
    }

    public static Result initialize(IStatRegistry statRegistry, IDamageCalculator damageCalculator){
        if (instance != null){
            return Result.Error("EngineAPI is already initialized");
        }
        instance = new EngineAPI(statRegistry,damageCalculator);
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

    public IDamageCalculator getDamageCalculator(){
        return damageCalculator;
    }

    public static void shutdown(){
        instance = null;
    }
}
