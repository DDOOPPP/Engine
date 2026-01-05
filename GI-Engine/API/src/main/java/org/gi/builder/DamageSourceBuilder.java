package org.gi.builder;

import org.gi.damage.DamageType;
import org.gi.damage.IDamageSource;
import org.gi.stat.IStatHolder;

public class DamageSourceBuilder {
    private IStatHolder attacker;
    private DamageType damageType = DamageType.PHYSICAL;
    private double baseDamage = 0;
    private double skillScaling = 1;
    private String sourceId = "unknown";
    private String displayName = "";
    private boolean canCritical = false;
    private boolean canEvade = false;
    private boolean canBlock = false;

    public static DamageSourceBuilder create(){
        return new DamageSourceBuilder();
    }

    /**
     * @param attacker 만약 null인 경우 환경요소로인한 데미지
     * */
    public DamageSourceBuilder attacker(IStatHolder attacker){
        this.attacker = attacker;
        return this;
    }

    public DamageSourceBuilder damageType(DamageType damageType){
        this.damageType = damageType;
        return this;
    }

    public DamageSourceBuilder physical(){
        this.damageType = DamageType.PHYSICAL;
        return this;
    }

    public DamageSourceBuilder magical(){
        this.damageType = DamageType.MAGICAL;
        return this;
    }

    public DamageSourceBuilder trueDamage(){
        this.damageType = DamageType.TRUE;
        return this;
    }

    public DamageSourceBuilder baseDamage(double baseDamage){
        this.baseDamage = baseDamage;
        return this;
    }

    /**
     * 스킬 계수 (targetStat * scaling)
     * */
    public DamageSourceBuilder skillScaling(double skillScaling){
        this.skillScaling = skillScaling;
        return this;
    }

    public DamageSourceBuilder sourceId(String sourceId){
        this.sourceId = sourceId;
        return this;
    }

    public DamageSourceBuilder displayName(String displayName){
        this.displayName = displayName;
        return this;
    }

    public DamageSourceBuilder canCritical(boolean canCritical){
        this.canCritical = canCritical;
        return this;
    }

    public DamageSourceBuilder canEvade(boolean canEvade){
        this.canEvade = canEvade;
        return this;
    }

    public DamageSourceBuilder canBlock(boolean canBlock){
        this.canBlock = canBlock;
        return this;
    }

    public DamageSourceBuilder unavoidable(){
        this.canEvade = false;
        this.canBlock = false;
        return this;
    }

    public IDamageSource build() {
        return new DamageSourceImpl(
                attacker,
                damageType,
                baseDamage,
                skillScaling,
                sourceId,
                displayName,
                canCritical,
                canEvade,
                canBlock
        );
    }

    private record DamageSourceImpl(
            IStatHolder attacker,
            DamageType damageType,
            double baseDamage,
            double skillScaling,
            String sourceId,
            String displayName,
            boolean canCritical,
            boolean canEvade,
            boolean canBlock
    ) implements IDamageSource {

        @Override
        public IStatHolder getAttacker() { return attacker; }

        @Override
        public DamageType getDamageType() { return damageType; }

        @Override
        public double getBaseDamage() { return baseDamage; }

        @Override
        public double getSkillScaling() { return skillScaling; }

        @Override
        public String getSourceId() { return sourceId; }

        @Override
        public String getDisplayName() { return displayName; }

        @Override
        public boolean canCritical() { return canCritical; }

        @Override
        public boolean canEvade() { return canEvade; }

        @Override
        public boolean canBlock() { return canBlock; }
    }
}
