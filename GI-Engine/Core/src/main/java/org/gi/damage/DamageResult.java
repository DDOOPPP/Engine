package org.gi.damage;

import org.gi.stat.IStatHolder;

public class DamageResult implements IDamageResult{
    public final IDamageSource source;
    private final IStatHolder target;
    private final double finalDamage;
    private final double rawDamage;
    private final double mitigatedDamage;
    private final boolean critical;
    private final boolean evaded;
    private final boolean blocked;
    private final double blockedDamage;
    private final ResultType resultType;

    public DamageResult (Builder builder){
        this.source = builder.source;
        this.target = builder.target;
        this.finalDamage = builder.finalDamage;
        this.rawDamage = builder.rawDamage;
        this.mitigatedDamage = builder.mitigatedDamage;
        this.critical = builder.critical;
        this.evaded = builder.evaded;
        this.blocked = builder.blocked;
        this.blockedDamage = builder.blockedDamage;
        this.resultType = builder.resultType;
    }


    @Override
    public IDamageSource getSource() {
        return source;
    }

    @Override
    public IStatHolder getTarget() {
        return target;
    }

    @Override
    public double getFinalDamage() {
        return finalDamage;
    }

    @Override
    public double getRawDamage() {
        return rawDamage;
    }

    @Override
    public double getMitigationDamage() {
        return mitigatedDamage;
    }

    @Override
    public boolean isCritical() {
        return critical;
    }

    @Override
    public boolean isEvade() {
        return evaded;
    }

    @Override
    public boolean isBlocked() {
        return blocked;
    }

    @Override
    public double getBlockDamage() {
        return blockedDamage;
    }

    @Override
    public ResultType getResultType() {
        return resultType;
    }

    public static class Builder {
        private IDamageSource source;
        private IStatHolder target;
        private double finalDamage;
        private double rawDamage;
        private double mitigatedDamage;
        private boolean critical;
        private boolean evaded;
        private boolean blocked;
        private double blockedDamage;
        private ResultType resultType = ResultType.HIT;

        public Builder source(IDamageSource source) {
            this.source = source;
            return this;
        }

        public Builder target(IStatHolder target) {
            this.target = target;
            return this;
        }

        public Builder finalDamage(double finalDamage) {
            this.finalDamage = finalDamage;
            return this;
        }

        public Builder rawDamage(double rawDamage) {
            this.rawDamage = rawDamage;
            return this;
        }

        public Builder mitigatedDamage(double mitigatedDamage) {
            this.mitigatedDamage = mitigatedDamage;
            return this;
        }

        public Builder critical(boolean critical) {
            this.critical = critical;
            return this;
        }

        public Builder evaded(boolean evaded) {
            this.evaded = evaded;
            return this;
        }

        public Builder blocked(boolean blocked) {
            this.blocked = blocked;
            return this;
        }

        public Builder blockedDamage(double blockedDamage) {
            this.blockedDamage = blockedDamage;
            return this;
        }

        public Builder resultType(ResultType resultType) {
            this.resultType = resultType;
            return this;
        }

        public DamageResult build() {
            return new DamageResult(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
